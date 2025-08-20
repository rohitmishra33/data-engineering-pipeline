-- Clear existing data
TRUNCATE TABLE monthly_sales_summary;
TRUNCATE TABLE top_products;
TRUNCATE TABLE region_wise_performance;
TRUNCATE TABLE category_discount_map;
TRUNCATE TABLE anomaly_records;

-- Populate monthly_sales_summary table
INSERT INTO monthly_sales_summary (year, month, total_revenue, total_quantity, avg_discount, order_count)
SELECT YEAR(STR_TO_DATE(sale_date, '%Y-%m-%d'))  AS year,
       MONTH(STR_TO_DATE(sale_date, '%Y-%m-%d')) AS month,
       ROUND(SUM(revenue), 2)                    AS total_revenue,
       SUM(quantity)                             AS total_quantity,
       ROUND(AVG(discount_percent), 4)           AS avg_discount,
       COUNT(DISTINCT order_id)                  AS order_count
FROM sales_data
WHERE sale_date IS NOT NULL
  AND STR_TO_DATE(sale_date, '%Y-%m-%d') IS NOT NULL
GROUP BY YEAR(STR_TO_DATE(sale_date, '%Y-%m-%d')),
         MONTH(STR_TO_DATE(sale_date, '%Y-%m-%d'))
ORDER BY year, month;

-- Check the populated data
SELECT *
FROM monthly_sales_summary
ORDER BY year, month;

-- Get summary statistics
SELECT COUNT(*)           AS total_months,
       MIN(year)          AS earliest_year,
       MAX(year)          AS latest_year,
       SUM(total_revenue) AS grand_total_revenue,
       SUM(order_count)   AS total_orders
FROM monthly_sales_summary;

-- Populate top_products table
-- Option 1: Top 10 by Revenue Only
INSERT INTO top_products (product_name, category, total_revenue, total_units_sold, avg_unit_price, avg_discount,
                          revenue_rank, units_rank)
SELECT product_name,
       category,
       ROUND(SUM(revenue), 2)                         AS total_revenue,
       SUM(quantity)                                  AS total_units_sold,
       ROUND(AVG(unit_price), 2)                      AS avg_unit_price,
       ROUND(AVG(discount_percent), 4)                AS avg_discount,
       ROW_NUMBER() OVER (ORDER BY SUM(revenue) DESC) AS revenue_rank,
       NULL                                           AS units_rank
FROM sales_data
WHERE quantity > 0
GROUP BY product_name, category
ORDER BY total_revenue DESC
LIMIT 10;

-- Option 2: Add Top 10 by Units (excluding duplicates)
INSERT INTO top_products (product_name, category, total_revenue, total_units_sold, avg_unit_price, avg_discount,
                          revenue_rank, units_rank)
SELECT s.product_name,
       s.category,
       ROUND(SUM(s.revenue), 2)                          AS total_revenue,
       SUM(s.quantity)                                   AS total_units_sold,
       ROUND(AVG(s.unit_price), 2)                       AS avg_unit_price,
       ROUND(AVG(s.discount_percent), 4)                 AS avg_discount,
       NULL                                              AS revenue_rank,
       ROW_NUMBER() OVER (ORDER BY SUM(s.quantity) DESC) AS units_rank
FROM sales_data s
WHERE s.quantity > 0
  AND NOT EXISTS (SELECT 1
                  FROM top_products tp
                  WHERE tp.product_name = s.product_name
                    AND tp.category = s.category)
GROUP BY s.product_name, s.category
ORDER BY SUM(s.quantity) DESC
LIMIT 10;

-- Check the populated data
SELECT product_name,
       category,
       total_revenue,
       total_units_sold,
       revenue_rank,
       units_rank
FROM top_products
ORDER BY COALESCE(revenue_rank, 999), COALESCE(units_rank, 999);

-- Summary statistics
SELECT COUNT(*)                                       AS total_products,
       COUNT(CASE WHEN revenue_rank <= 10 THEN 1 END) AS top_revenue_products,
       COUNT(CASE WHEN units_rank <= 10 THEN 1 END)   AS top_units_products,
       MAX(total_revenue)                             AS highest_revenue,
       MAX(total_units_sold)                          AS highest_units
FROM top_products;

-- Populate region_wise_performance table
INSERT INTO region_wise_performance (region, total_revenue, total_orders, total_quantity, avg_order_value, avg_discount,
                                     top_product, top_category, market_share_pct)
WITH region_stats AS (SELECT region,
                             ROUND(SUM(revenue), 2)                            AS total_revenue,
                             COUNT(DISTINCT order_id)                          AS total_orders,
                             SUM(quantity)                                     AS total_quantity,
                             ROUND(SUM(revenue) / COUNT(DISTINCT order_id), 2) AS avg_order_value,
                             ROUND(AVG(discount_percent), 4)                   AS avg_discount
                      FROM sales_data
                      WHERE region IS NOT NULL
                        AND region != ''
                      GROUP BY region),
     region_top_products AS (SELECT region,
                                    product_name,
                                    category,
                                    SUM(revenue)                                                       AS product_revenue,
                                    ROW_NUMBER() OVER (PARTITION BY region ORDER BY SUM(revenue) DESC) AS rn
                             FROM sales_data
                             WHERE region IS NOT NULL
                               AND region != ''
                             GROUP BY region, product_name, category),
     total_market AS (SELECT SUM(revenue) AS total_market_revenue FROM sales_data WHERE revenue > 0)
SELECT rs.region,
       rs.total_revenue,
       rs.total_orders,
       rs.total_quantity,
       rs.avg_order_value,
       rs.avg_discount,
       rtp.product_name                                             AS top_product,
       rtp.category                                                 AS top_category,
       ROUND((rs.total_revenue / tm.total_market_revenue) * 100, 2) AS market_share_pct
FROM region_stats rs
         LEFT JOIN region_top_products rtp ON rs.region = rtp.region AND rtp.rn = 1
         CROSS JOIN total_market tm
ORDER BY rs.total_revenue DESC;

-- Check the populated data
SELECT region,
       total_revenue,
       total_orders,
       total_quantity,
       avg_order_value,
       avg_discount,
       top_product,
       top_category,
       market_share_pct
FROM region_wise_performance
ORDER BY total_revenue DESC;

-- Summary statistics
SELECT COUNT(*)                        AS total_regions,
       SUM(total_revenue)              AS grand_total_revenue,
       SUM(total_orders)               AS grand_total_orders,
       ROUND(AVG(market_share_pct), 2) AS avg_market_share,
       MAX(market_share_pct)           AS highest_market_share
FROM region_wise_performance;

-- Check if market share adds up to 100%
SELECT ROUND(SUM(market_share_pct), 2) AS total_market_share_pct
FROM region_wise_performance;


-- Populate category_discount_map table
INSERT INTO category_discount_map (category, avg_discount, min_discount, max_discount, median_discount, total_orders,
                                   total_revenue, discount_impact)
WITH category_stats AS (SELECT category,
                               ROUND(AVG(discount_percent), 4)                  AS avg_discount,
                               ROUND(MIN(discount_percent), 4)                  AS min_discount,
                               ROUND(MAX(discount_percent), 4)                  AS max_discount,
                               COUNT(*)                                         AS total_orders,
                               ROUND(SUM(revenue), 2)                           AS total_revenue,
                               ROUND(SUM((unit_price * quantity) - revenue), 2) AS discount_impact
                        FROM sales_data
                        WHERE category IS NOT NULL
                          AND category != ''
                          AND discount_percent >= 0
                          AND unit_price > 0
                          AND quantity > 0
                        GROUP BY category),
     category_medians AS (SELECT category,
                                 AVG(discount_percent) AS median_discount
                          FROM (SELECT category,
                                       discount_percent,
                                       ROW_NUMBER() OVER (PARTITION BY category ORDER BY discount_percent) AS row_num,
                                       COUNT(*) OVER (PARTITION BY category)                               AS total_count
                                FROM sales_data
                                WHERE category IS NOT NULL
                                  AND category != ''
                                  AND discount_percent >= 0) ranked
                          WHERE row_num IN (FLOOR((total_count + 1) / 2), CEIL((total_count + 1) / 2))
                          GROUP BY category)
SELECT cs.category,
       cs.avg_discount,
       cs.min_discount,
       cs.max_discount,
       ROUND(COALESCE(cm.median_discount, cs.avg_discount), 4) AS median_discount,
       cs.total_orders,
       cs.total_revenue,
       cs.discount_impact
FROM category_stats cs
         LEFT JOIN category_medians cm ON cs.category = cm.category
ORDER BY cs.avg_discount DESC;

-- Check the populated data
SELECT category,
       avg_discount,
       min_discount,
       max_discount,
       median_discount,
       total_orders,
       total_revenue,
       discount_impact
FROM category_discount_map
ORDER BY avg_discount DESC;

-- Summary statistics
SELECT COUNT(*)                       AS total_categories,
       ROUND(AVG(avg_discount), 4)    AS overall_avg_discount,
       ROUND(SUM(total_revenue), 2)   AS grand_total_revenue,
       ROUND(SUM(discount_impact), 2) AS total_discount_impact
FROM category_discount_map;


-- Populate anomaly_records table
INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue,
                             sale_date, customer_email, anomaly_type, anomaly_score, percentile)
-- Top 5 high revenue anomalies
    (SELECT order_id,
            product_name,
            category,
            region,
            quantity,
            unit_price,
            discount_percent,
            revenue,
            sale_date,
            customer_email,
            'HIGH_REVENUE' as anomaly_type,
            revenue        as anomaly_score,
            99.9           as percentile
     FROM sales_data
     WHERE revenue > 0
     ORDER BY revenue DESC
     LIMIT 5)

UNION ALL

-- Top 5 high quantity anomalies
(SELECT order_id,
        product_name,
        category,
        region,
        quantity,
        unit_price,
        discount_percent,
        revenue,
        sale_date,
        customer_email,
        'HIGH_QUANTITY' as anomaly_type,
        quantity        as anomaly_score,
        99.9            as percentile
 FROM sales_data
 WHERE quantity > 0
 ORDER BY quantity DESC
 LIMIT 5)

UNION ALL

-- Top 5 high discount anomalies
(SELECT order_id,
        product_name,
        category,
        region,
        quantity,
        unit_price,
        discount_percent,
        revenue,
        sale_date,
        customer_email,
        'HIGH_DISCOUNT'  as anomaly_type,
        discount_percent as anomaly_score,
        99.0             as percentile
 FROM sales_data
 WHERE discount_percent >= 100.0 -- 100% or more discount
 ORDER BY discount_percent DESC
 LIMIT 5)

UNION ALL

-- Negative values (data quality issues)
(SELECT order_id,
        product_name,
        category,
        region,
        quantity,
        unit_price,
        discount_percent,
        revenue,
        sale_date,
        customer_email,
        'NEGATIVE_VALUES'                         as anomaly_type,
        ABS(LEAST(quantity, unit_price, revenue)) as anomaly_score,
        0                                         as percentile
 FROM sales_data
 WHERE quantity < 0
    OR unit_price < 0
    OR revenue < 0
 ORDER BY ABS(LEAST(quantity, unit_price, revenue)) DESC
 LIMIT 5)

UNION ALL

-- Price anomalies (extremely high unit prices)
(SELECT order_id,
        product_name,
        category,
        region,
        quantity,
        unit_price,
        discount_percent,
        revenue,
        sale_date,
        customer_email,
        'PRICE_ANOMALY' as anomaly_type,
        unit_price      as anomaly_score,
        99.5            as percentile
 FROM sales_data
 WHERE unit_price > (SELECT AVG(unit_price) + (3 * STDDEV(unit_price))
                     FROM sales_data
                     WHERE unit_price > 0)
 ORDER BY unit_price DESC
 LIMIT 5);

-- Check the populated data
SELECT
    anomaly_type,
    COUNT(*) as count,
    ROUND(MAX(anomaly_score), 2) as max_score,
    ROUND(MIN(anomaly_score), 2) as min_score,
    ROUND(AVG(anomaly_score), 2) as avg_score
FROM anomaly_records
GROUP BY anomaly_type
ORDER BY anomaly_type;

-- View top anomalies by type
SELECT
    order_id,
    product_name,
    anomaly_type,
    ROUND(anomaly_score, 2) as anomaly_score,
    percentile
FROM anomaly_records
ORDER BY anomaly_type, anomaly_score DESC;

