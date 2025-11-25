# Blurr - High-Performance Sales Analytics System

## 1. Overview

Blurr is a comprehensive sales analytics system designed to process and visualize large volumes of sales data. It consists of two main components:

1.  **A high-performance data ingestion pipeline** built with core Java, designed for scalability and efficiency in processing large CSV files.
2.  **An interactive web dashboard** powered by a Spring Boot backend and a Chart.js frontend, providing real-time analytical insights.

The system is engineered to handle everything from raw data ingestion and cleaning to automated analytical processing and rich data visualization, making it a powerful tool for business intelligence.

---

## 2. Key Features

### 2.1. Scalable Data Ingestion Pipeline

-   **High Throughput:** Utilizes a multi-threaded, producer-consumer architecture to ingest and process millions of records per minute.
-   **Data Validation & Transformation:** Automatically cleans and standardizes data during ingestion, including:
    -   Normalizing categories and regions.
    -   Cleaning product names.
    -   Validating data formats (dates, emails).
    -   Enforcing constraints (e.g., maximum discount percentage).
-   **Flexible Data Handling:** Supports multiple output strategies:
    -   **`DATABASE_BATCH`:** Efficiently loads data into a MySQL database using optimized batch inserts with deadlock retry logic.
    -   **`FILE_OUTPUT`:** Processes data and writes the clean output to a new CSV file.
    -   **`IN_MEMORY_STORE`:** Stores records in memory for testing and debugging.
-   **Configurable:** All key parameters, such as batch sizes, thread counts, and processing strategies, are easily configured through a `.env` file.

### 2.2. Automated Analytical Processing

-   **Pre-aggregated Summary Tables:** After data ingestion, the system automatically refreshes a set of analytical tables in the database. This ensures the dashboard remains fast and responsive, as it queries these optimized tables instead of the raw data.
-   **Comprehensive Analytics:** The system calculates and stores key business metrics, including:
    -   **Monthly Sales Trends:** Aggregates revenue, quantity, and order counts by month.
    -   **Top Products:** Ranks products by total revenue and units sold.
    -   **Regional Performance:** Calculates total revenue, market share, and top-selling products for each region.
    -   **Discount Impact Analysis:** Analyzes the average discount and its impact on revenue for each product category.
-   **Anomaly Detection:** Automatically identifies and flags unusual records, such as:
    -   Orders with extremely high revenue or quantity.
    -   Transactions with excessive discounts.
    -   Data quality issues like negative values.

### 2.3. Interactive Web Dashboard

-   **Rich Visualizations:** The dashboard provides a suite of charts and summary cards to visualize the analytical data, including:
    -   **Summary Cards:** At-a-glance view of total revenue, total orders, active regions, and top products.
    -   **Line Chart:** For tracking monthly sales trends.
    -   **Doughnut Chart:** For comparing market share across different regions.
    -   **Bar Charts:** For visualizing top products and category-wise discount percentages.
-   **REST API Backend:** The dashboard is powered by a robust Spring Boot application that exposes a REST API to serve the pre-aggregated analytical data.
-   **Self-Contained:** The entire application (backend and frontend) is served from a single Spring Boot instance, simplifying deployment.

---

## 3. System Architecture

The system is divided into two decoupled components that interact via the MySQL database.

```
+------------------+      +----------------------+      +--------------------+
|                  |      |                      |      |                    |
|  CSV Input File  |----->| Data Ingestion       |----->|   MySQL Database   |<-----.
| (Large Dataset)  |      | Pipeline (Core Java) |      |                    |      |
|                  |      |                      |      +--------------------+      |
+------------------+      +----------------------+                 ^                |
                                                                   | (Refreshes)    | (Reads)
                               +-----------------------------+     |                |
                               | Automated Analytics         |-----'                |
                               | Refresher (Java/SQL)        |                      |
                               +-----------------------------+                      |
                                                                                    |
                               +------------------------------+     +---------------+
                               |                              |     |
                               | Dashboard Backend (Spring    |-----'
                               | Boot) + Frontend (HTML/JS)   |
                               |                              |
                               +------------------------------+
```

1.  **Ingestion:** The **Data Ingestion Pipeline** reads a large CSV file, processes the data in parallel, and writes the clean records to the `sales_data` table in the database.
2.  **Analytics:** Once ingestion is complete, the **Analytical Data Refresher** runs a series of SQL queries to truncate and repopulate the summary tables (e.g., `monthly_sales_summary`, `top_products`).
3.  **Visualization:** The **Dashboard Application** (Spring Boot) runs continuously. Its backend API queries the summary tables, and the frontend fetches this data to render the charts and visualizations for the user.

---

## 4. Technical Stack

-   **Backend & Data Processing:**
    -   Java 21
    -   Spring Boot 3
    -   Core Java Concurrency Utilities
    -   Maven
-   **Database:**
    -   MySQL 8
-   **Frontend:**
    -   HTML5
    -   CSS3
    -   JavaScript (ES6+)
    -   Chart.js
-   **Configuration:**
    -   `java-dotenv`

---

## 5. Getting Started

### Prerequisites

-   Java 21 (or higher)
-   Apache Maven 3.6 (or higher)
-   MySQL Server 8.0 (or higher)
-   A large CSV file with sales data. A sample generator `SampleDataGenerator.java` is included in `src/main/java`.

### Step 1: Database Setup

1.  Create a new database in your MySQL server.
2.  Run the `src/main/sql_scripts/table_creation.sql` script to create the required tables and schema.

### Step 2: Configuration

This project uses two separate configuration files:
- **`.env`**: For the command-line data ingestion pipeline.
- **`application.properties`**: For the Spring Boot web dashboard.

#### 2.1. Configure the Data Ingestion Pipeline

1.  In the project root directory, create a `.env` file.
2.  Populate it with your database credentials and desired ingestion settings. The required variables are shown below.

    ```ini
    # .env file

    # Database Configuration
    DB_HOST=localhost
    DB_PORT=3306
    DB_DATABASE_NAME=your_database_name
    DB_USERNAME=your_username
    DB_PASSWORD=your_password

    # Ingestion Pipeline Configuration
    INGESTION_FILE=/path/to/your/sales_data.csv
    BATCH_SIZE=20000
    CORE_THREADS=8
    MAX_THREADS=16
    PROCESSOR_THREADS=8
    QUEUE_CAPACITY=1000
    SKIP_HEADER=true
    INGESTION_STRATEGY=DATABASE_BATCH # or FILE_OUTPUT, IN_MEMORY_STORE
    SKIP_ANALYTICS_UPDATE=false
    ```

#### 2.2. Configure the Spring Boot Dashboard

1.  Open the file `src/main/resources/application.properties`.
2.  Update the `spring.datasource` properties with your database credentials.

    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```

### Step 3: Run the Data Ingestion Pipeline

1.  Open the project in your IDE.
2.  Navigate to `src/main/java/com/blurr/pipeline/app/Main.java`.
3.  Run the `main` method.

This will start the ingestion process. The console will show progress and a final summary. After ingestion, it will automatically trigger the analytical data refresh.

### Step 4: Run the Dashboard Application

1.  Navigate to `src/main/java/com/blurr/dashboard/DashboardApplication.java`.
2.  Run the `main` method.
3.  Open your web browser and go to **http://localhost:8080**.

The dashboard will load and display the analytical insights from the data you ingested.

---

## 6. Project Structure

```
.
├── .env                    # Environment configuration (you need to create this)
├── pom.xml                 # Maven project configuration
└── src
    ├── main
    │   ├── java
    │   │   ├── com
    │   │   │   └── blurr
    │   │   │       ├── dashboard     # Spring Boot Dashboard Application
    │   │   │       │   ├── controller
    │   │   │       │   ├── dto
    │   │   │       │   ├── service
    │   │   │       │   └── DashboardApplication.java
    │   │   │       └── pipeline      # Data Ingestion Pipeline
    │   │   │           ├── app         # Main entry point for the pipeline
    │   │   │           ├── config
    │   │   │           ├── core        # Core ingestion and analytics logic
    │   │   │           ├── handlers    # Data handlers (DB, File, etc.)
    │   │   │           ├── models
    │   │   │           └── util
    │   │   └── SampleDataGenerator.java # Utility to generate sample CSV data
    │   ├── resources
    │   │   ├── static
    │   │   │   └── dashboard.html  # Frontend dashboard file
    │   │   └── application.properties # Spring Boot configuration
    │   └── sql_scripts
    │       ├── table_creation.sql  # DDL scripts for database schema
    │       └── ...
    └── test
```

---

## 7. Database Schema

The database consists of one main table for raw data and several summary tables for analytics.

-   **`sales_data`**: The primary table where all the cleaned, ingested sales records are stored.
-   **`monthly_sales_summary`**: Stores aggregated sales data for each month.
-   **`top_products`**: Stores the top-performing products based on revenue and units sold.
-   **`region_wise_performance`**: Stores key performance metrics for each sales region.
-   **`category_discount_map`**: Stores analysis of discounts and their impact by category.
-   **`anomaly_records`**: Stores records that were flagged as anomalous during the analytics phase.
