package com.blurr.pipeline.core.analytics;

public class QueryStore {
    public static String MONTHLY_SALES_ANALYTICS_QUERY = """
            INSERT INTO monthly_sales_summary (year, month, total_revenue, total_quantity, avg_discount, order_count)
            SELECT
                YEAR(STR_TO_DATE(sale_date, '%Y-%m-%d')) AS year,
                MONTH(STR_TO_DATE(sale_date, '%Y-%m-%d')) AS month,
                ROUND(SUM(revenue), 2) AS total_revenue,
                SUM(quantity) AS total_quantity,
                ROUND(AVG(discount_percent), 4) AS avg_discount,
                COUNT(DISTINCT order_id) AS order_count
            FROM sales_data
            WHERE sale_date IS NOT NULL
              AND STR_TO_DATE(sale_date, '%Y-%m-%d') IS NOT NULL
            GROUP BY YEAR(STR_TO_DATE(sale_date, '%Y-%m-%d')),
                     MONTH(STR_TO_DATE(sale_date, '%Y-%m-%d'))
            ORDER BY year, month
            """;

    public static String TOP_PRODUCTS_ANALYTICS_QUERY = """
            INSERT INTO top_products (product_name, category, total_revenue, total_units_sold, avg_unit_price, avg_discount, revenue_rank, units_rank)
            WITH product_stats AS (
                SELECT
                    product_name,
                    category,
                    ROUND(SUM(revenue), 2) AS total_revenue,
                    SUM(quantity) AS total_units_sold,
                    ROUND(AVG(unit_price), 2) AS avg_unit_price,
                    ROUND(AVG(discount_percent), 4) AS avg_discount
                FROM sales_data
                WHERE quantity > 0
                GROUP BY product_name, category
            ),
            revenue_ranked AS (
                SELECT *,
                       ROW_NUMBER() OVER (ORDER BY total_revenue DESC) AS revenue_rank
                FROM product_stats
            ),
            units_ranked AS (
                SELECT *,
                       ROW_NUMBER() OVER (ORDER BY total_units_sold DESC) AS units_rank
                FROM product_stats
            )
            SELECT
                r.product_name,
                r.category,
                r.total_revenue,
                r.total_units_sold,
                r.avg_unit_price,
                r.avg_discount,
                r.revenue_rank,
                COALESCE(u.units_rank, 999) AS units_rank
            FROM revenue_ranked r
            LEFT JOIN units_ranked u ON r.product_name = u.product_name AND r.category = u.category
            WHERE r.revenue_rank <= 10
            UNION
            SELECT
                u.product_name,
                u.category,
                u.total_revenue,
                u.total_units_sold,
                u.avg_unit_price,
                u.avg_discount,
                COALESCE(r.revenue_rank, 999) AS revenue_rank,
                u.units_rank
            FROM units_ranked u
            LEFT JOIN revenue_ranked r ON u.product_name = r.product_name AND u.category = r.category
            WHERE u.units_rank <= 10 AND r.product_name IS NULL
            ORDER BY revenue_rank, units_rank
            """;

    public static String REGION_WISE_PERFORMANCE_ANALYTICS_QUERY = """
            INSERT INTO region_wise_performance (region, total_revenue, total_orders, total_quantity, avg_order_value, avg_discount, top_product, top_category, market_share_pct)
            WITH region_stats AS (
                SELECT
                    region,
                    ROUND(SUM(revenue), 2) AS total_revenue,
                    COUNT(DISTINCT order_id) AS total_orders,
                    SUM(quantity) AS total_quantity,
                    ROUND(SUM(revenue) / COUNT(DISTINCT order_id), 2) AS avg_order_value,
                    ROUND(AVG(discount_percent), 4) AS avg_discount
                FROM sales_data
                WHERE region IS NOT NULL AND region != ''
                GROUP BY region
            ),
            region_top_products AS (
                SELECT
                    region,
                    product_name,
                    category,
                    SUM(revenue) AS product_revenue,
                    ROW_NUMBER() OVER (PARTITION BY region ORDER BY SUM(revenue) DESC) AS rn
                FROM sales_data
                WHERE region IS NOT NULL AND region != ''
                GROUP BY region, product_name, category
            ),
            total_market AS (
                SELECT SUM(revenue) AS total_market_revenue FROM sales_data WHERE revenue > 0
            )
            SELECT
                rs.region,
                rs.total_revenue,
                rs.total_orders,
                rs.total_quantity,
                rs.avg_order_value,
                rs.avg_discount,
                rtp.product_name AS top_product,
                rtp.category AS top_category,
                ROUND((rs.total_revenue / tm.total_market_revenue) * 100, 2) AS market_share_pct
            FROM region_stats rs
            LEFT JOIN region_top_products rtp ON rs.region = rtp.region AND rtp.rn = 1
            CROSS JOIN total_market tm
            ORDER BY rs.total_revenue DESC
            """;

    public static String CATEGORY_DISCOUNT_MAP_ANALYTICS_QUERY = """
            INSERT INTO category_discount_map (category, avg_discount, min_discount, max_discount, median_discount, total_orders, total_revenue, discount_impact)
            SELECT
                category,
                ROUND(AVG(discount_percent), 4) AS avg_discount,
                ROUND(MIN(discount_percent), 4) AS min_discount,
                ROUND(MAX(discount_percent), 4) AS max_discount,
                ROUND(AVG(discount_percent), 4) AS median_discount,
                COUNT(*) AS total_orders,
                ROUND(SUM(revenue), 2) AS total_revenue,
                ROUND(SUM((unit_price * quantity) - revenue), 2) AS discount_impact
            FROM sales_data
            WHERE category IS NOT NULL
              AND category != ''
              AND discount_percent >= 0
              AND unit_price > 0
              AND quantity > 0
            GROUP BY category
            ORDER BY avg_discount DESC
            """;

    public static String HIGH_REVENUE_ANOMALIES_QUERY = """
            INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue, sale_date, customer_email, anomaly_type, anomaly_score, percentile)
            SELECT
                order_id, product_name, category, region, quantity, unit_price,
                discount_percent, revenue, sale_date, customer_email,
                'HIGH_REVENUE' as anomaly_type,
                revenue as anomaly_score,
                99.9 as percentile
            FROM sales_data
            WHERE revenue > 0
            ORDER BY revenue DESC
            LIMIT 5
            """;

    public static String HIGH_QUANTITY_ANOMALIES_QUERY = """
            INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue, sale_date, customer_email, anomaly_type, anomaly_score, percentile)
            SELECT
                order_id, product_name, category, region, quantity, unit_price,
                discount_percent, revenue, sale_date, customer_email,
                'HIGH_QUANTITY' as anomaly_type,
                quantity as anomaly_score,
                99.9 as percentile
            FROM sales_data
            WHERE quantity > 0
            ORDER BY quantity DESC
            LIMIT 5
            """;

    public static String HIGH_DISCOUNT_ANOMALIES_QUERY = """
            INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue, sale_date, customer_email, anomaly_type, anomaly_score, percentile)
            SELECT
                order_id, product_name, category, region, quantity, unit_price,
                discount_percent, revenue, sale_date, customer_email,
                'HIGH_DISCOUNT' as anomaly_type,
                discount_percent as anomaly_score,
                99.0 as percentile
            FROM sales_data
            WHERE discount_percent >= 100.0
            ORDER BY discount_percent DESC
            LIMIT 5
            """;

    public static String NEGATIVE_VALUE_ANOMALIES_QUERY = """
            INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue, sale_date, customer_email, anomaly_type, anomaly_score, percentile)
            SELECT
                order_id, product_name, category, region, quantity, unit_price,
                discount_percent, revenue, sale_date, customer_email,
                'NEGATIVE_VALUES' as anomaly_type,
                ABS(LEAST(quantity, unit_price, revenue)) as anomaly_score,
                0 as percentile
            FROM sales_data
            WHERE quantity < 0 OR unit_price < 0 OR revenue < 0
            ORDER BY ABS(LEAST(quantity, unit_price, revenue)) DESC
            LIMIT 5
            """;

    public static String PRICING_ANOMALIES_QUERY = """
            INSERT INTO anomaly_records (order_id, product_name, category, region, quantity, unit_price, discount_percent, revenue, sale_date, customer_email, anomaly_type, anomaly_score, percentile)
            SELECT
                s.order_id, s.product_name, s.category, s.region, s.quantity, s.unit_price,
                s.discount_percent, s.revenue, s.sale_date, s.customer_email,
                'PRICE_ANOMALY' as anomaly_type,
                s.unit_price as anomaly_score,
                99.5 as percentile
            FROM sales_data s
            CROSS JOIN (
                SELECT AVG(unit_price) + (3 * STDDEV(unit_price)) as threshold
                FROM sales_data
                WHERE unit_price > 0
            ) t
            WHERE s.unit_price > t.threshold
            ORDER BY s.unit_price DESC
            LIMIT 5
            """;
}
