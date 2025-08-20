# Sales Analytics Platform

A complete end-to-end analytics platform for processing and visualizing sales data. Features a high-performance Java ingestion pipeline for processing millions of records and a modern web dashboard for real-time analytics visualization.

## 🚀 Project Overview

This platform consists of two main components:
1. **High-Performance Data Ingestion Pipeline** - Processes CSV sales data into MySQL analytical tables
2. **Interactive Analytics Dashboard** - Spring Boot REST API with HTML/Chart.js frontend

---
## Sample Data Generation
Run `java SampleDataGenerator 10000` to generate a file containing 10k lines of sample data that can be ingested via the ingestion pipeline.

---
## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- MySQL 8.0+

### 1. Setup MySQL
Create your schema and analytical tables using provided SQL scripts (or see below).


### 2. Configure Ingestion and Database
Edit `.env` file with and configure your system specific parameters.
```
Change the configurations below as per your system hardware and requirements

# Ingestion Config
INGESTION_FILE={path_to_the_input_file} // Relative to Project Root
BATCH_SIZE=5000
CORE_THREADS=8
MAX_THREADS=10
PROCESSOR_THREADS=6
QUEUE_CAPACITY=500
SKIP_HEADER=true
INGESTION_STRATEGY=DATABASE_BATCH

# Database Credentials [MySQL]
DB_HOST=localhost
DB_PORT=3306
DB_DATABASE_NAME={database_name}
DB_USERNAME={username}
DB_PASSWORD={password}
```

### 3. Configure Spring Boot
Edit `src/main/resources/application.properties` file with your DB credentials:
- spring.datasource.url=jdbc:mysql://localhost:3306/{database_name}
- spring.datasource.username={username}
- spring.datasource.password={passowrd}
- server.port=8080

---

## Data Ingestion Pipeline

### Compile:
`mvn clean install`

### Run:
- Run `java -Xmx8g com.blurr.pipeline.app.Main` to run the ingestion pipeline which will ingest the file data and populate the analytical tables

---
## Analytics Dashboard

### Start the Service
`mvn spring-boot run`

### Open Frontend
Visit [http://localhost:8080/dashboard.html](http://localhost:8080/dashboard.html) in your browser to open the dashboard.

### REST API Endpoints
- `/api/dashboard/summary`
- `/api/dashboard/monthly-sales`
- `/api/dashboard/top-products`
- `/api/dashboard/region-performance`
- `/api/dashboard/category-discounts`

---

## Database Schema
Run `src/main/sql_scripts/table_creation.sql` script before ingesting any file to make sure that the table schema are created and ready for data ingestion and analytics calculations.

---

## License & Credits

MIT License © 2025 Rohit Mishra  
Built with Java, Spring Boot, MySQL, HTML, JavaScript & Chart.js.

---

*Contributions and suggestions welcome!*


