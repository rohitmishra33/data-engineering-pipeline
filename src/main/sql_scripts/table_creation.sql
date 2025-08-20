CREATE TABLE sales_data
(
    order_id         VARCHAR(20) PRIMARY KEY,
    product_name     VARCHAR(100),
    category         VARCHAR(50),
    quantity         INT,
    unit_price       DECIMAL(10, 2),
    discount_percent DECIMAL(5, 2),
    region           VARCHAR(20),
    sale_date        DATE,
    customer_email   VARCHAR(100),
    revenue          DECIMAL(12, 2)
);

CREATE TABLE monthly_sales_summary
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    year           INT            NOT NULL,
    month          INT            NOT NULL,
    total_revenue  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_quantity BIGINT         NOT NULL DEFAULT 0,
    avg_discount   DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    order_count    INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_year_month (year, month)
);

CREATE TABLE top_products
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    product_name     VARCHAR(255)   NOT NULL,
    category         VARCHAR(100),
    total_revenue    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_units_sold BIGINT         NOT NULL DEFAULT 0,
    avg_unit_price   DECIMAL(10, 2) NOT NULL DEFAULT 0,
    avg_discount     DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    revenue_rank     INT,
    units_rank       INT,
    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_name (product_name),
    INDEX idx_revenue_rank (revenue_rank),
    INDEX idx_units_rank (units_rank)
);

CREATE TABLE region_wise_performance
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    region           VARCHAR(100)   NOT NULL,
    total_revenue    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_orders     INT            NOT NULL DEFAULT 0,
    total_quantity   BIGINT         NOT NULL DEFAULT 0,
    avg_order_value  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    avg_discount     DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    top_product      VARCHAR(255),
    top_category     VARCHAR(100),
    market_share_pct DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_region (region),
    INDEX idx_revenue (total_revenue DESC)
);

CREATE TABLE category_discount_map
(
    id              INT AUTO_INCREMENT PRIMARY KEY,
    category        VARCHAR(100)   NOT NULL,
    avg_discount    DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    min_discount    DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    max_discount    DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    median_discount DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    total_orders    INT            NOT NULL DEFAULT 0,
    total_revenue   DECIMAL(15, 2) NOT NULL DEFAULT 0,
    discount_impact DECIMAL(15, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_avg_discount (avg_discount DESC)
);

CREATE TABLE anomaly_records
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    order_id         VARCHAR(50)                                                                                 NOT NULL,
    product_name     VARCHAR(255),
    category         VARCHAR(100),
    region           VARCHAR(100),
    quantity         INT,
    unit_price       DECIMAL(10, 2),
    discount_percent DECIMAL(6, 4),
    revenue          DECIMAL(15, 2)                                                                              NOT NULL,
    sale_date        VARCHAR(50),
    customer_email   VARCHAR(255),
    anomaly_type     ENUM ('HIGH_REVENUE', 'HIGH_QUANTITY', 'HIGH_DISCOUNT', 'NEGATIVE_VALUES', 'PRICE_ANOMALY') NOT NULL,
    anomaly_score    DECIMAL(8, 4),
    percentile       DECIMAL(5, 2),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_anomaly_type (anomaly_type),
    INDEX idx_revenue (revenue DESC)
);