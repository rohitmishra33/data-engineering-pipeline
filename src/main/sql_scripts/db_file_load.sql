LOAD DATA INFILE '/var/lib/mysql-files/final_output_10000000_rows.csv'
    IGNORE INTO TABLE sales_data
    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    IGNORE 1 ROWS
    (@order_id, @product_name, @category, @quantity, @unit_price, @discount_percent, @region, @sale_date,
     @customer_email, @revenue)
    SET
        order_id = NULLIF(@order_id, ''),
        product_name = NULLIF(@product_name, ''),
        category = NULLIF(@category, ''),
        quantity = NULLIF(@quantity, ''),
        unit_price = NULLIF(@unit_price, ''),
        discount_percent = NULLIF(@discount_percent, ''),
        region = NULLIF(@region, ''),
        sale_date = CASE WHEN @sale_date = '' THEN NULL ELSE STR_TO_DATE(@sale_date, '%Y-%m-%d') END,
        customer_email = NULLIF(@customer_email, ''),
        revenue = NULLIF(@revenue, '');