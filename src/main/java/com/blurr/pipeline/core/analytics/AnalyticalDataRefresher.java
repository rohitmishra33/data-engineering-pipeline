package com.blurr.pipeline.core.analytics;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class AnalyticalDataRefresher {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private Connection connection;

    // Statistics tracking
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong tablesRefreshed = new AtomicLong(0);

    public AnalyticalDataRefresher() {
        Dotenv dotenv = Dotenv.configure().load();

        String databaseHost = dotenv.get("DB_HOST");
        int databasePort = Integer.parseInt(Objects.requireNonNull(dotenv.get("DB_PORT")));
        String databaseName = dotenv.get("DB_DATABASE_NAME");

        this.jdbcUrl = "jdbc:mysql://" + databaseHost + ":" + databasePort + "/" + databaseName + "?" +
                "useServerPrepStmts=true&" +
                "rewriteBatchedStatements=true&" +
                "useLocalSessionState=true&" +
                "sessionVariables=transaction_isolation='READ-COMMITTED'&" +
                "useLocalTransactionState=true";
        this.username = dotenv.get("DB_USERNAME");
        this.password = dotenv.get("DB_PASSWORD");
    }

    public AnalyticalDataRefresher(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Main method to refresh all analytical tables
     */
    public void refreshAllAnalyticalTables() {
        long startTime = System.currentTimeMillis();

        System.out.println("=== Starting Analytical Data Refresh at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===");

        try {
            initializeConnection();

            // Refresh all tables in sequence
            refreshMonthlySalesSummary();
            refreshTopProducts();
            refreshRegionWisePerformance();
            refreshCategoryDiscountMap();
            refreshAnomalyRecords();

            // Generate summary report
            generateSummaryReport();

            long totalTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.set(totalTime);

            System.out.println("\n=== Analytical Data Refresh Completed Successfully ===");
            System.out.println("Total processing time: " + (double) totalTime / 1000 + " s");
            System.out.println("Tables refreshed: " + tablesRefreshed.get());

        } catch (SQLException e) {
            System.err.println("Error during analytical data refresh: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void initializeConnection() throws SQLException {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, username, password);
            this.connection.setAutoCommit(false);
            this.connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            System.out.println("Database connection established successfully");
        } catch (SQLException e) {
            throw new SQLException("Failed to establish database connection", e);
        }
    }

    private void refreshMonthlySalesSummary() throws SQLException {
        System.out.println("Refreshing monthly_sales_summary...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        executeUpdate("TRUNCATE TABLE monthly_sales_summary");

        // Insert new data
        int rowsInserted = executeUpdate(QueryStore.MONTHLY_SALES_ANALYTICS_QUERY);

        connection.commit();
        tablesRefreshed.incrementAndGet();

        System.out.println("monthly_sales_summary refreshed: " + rowsInserted + " rows inserted (" +
                (double) (System.currentTimeMillis() - startTime) / 1000 + " s)");
    }

    private void refreshTopProducts() throws SQLException {
        System.out.println("Refreshing top_products...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        executeUpdate("TRUNCATE TABLE top_products");

        // Insert new data
        int rowsInserted = executeUpdate(QueryStore.TOP_PRODUCTS_ANALYTICS_QUERY);

        connection.commit();
        tablesRefreshed.incrementAndGet();

        System.out.println("top_products refreshed: " + rowsInserted + " rows inserted (" +
                (double) (System.currentTimeMillis() - startTime) / 1000 + " s)");
    }

    private void refreshRegionWisePerformance() throws SQLException {
        System.out.println("Refreshing region_wise_performance...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        executeUpdate("TRUNCATE TABLE region_wise_performance");

        // Insert new data
        int rowsInserted = executeUpdate(QueryStore.REGION_WISE_PERFORMANCE_ANALYTICS_QUERY);

        connection.commit();
        tablesRefreshed.incrementAndGet();

        System.out.println("region_wise_performance refreshed: " + rowsInserted + " rows inserted (" +
                (double) (System.currentTimeMillis() - startTime) / 1000 + " s)");
    }

    private void refreshCategoryDiscountMap() throws SQLException {
        System.out.println("Refreshing category_discount_map...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        executeUpdate("TRUNCATE TABLE category_discount_map");

        // Insert new data
        int rowsInserted = executeUpdate(QueryStore.CATEGORY_DISCOUNT_MAP_ANALYTICS_QUERY);

        connection.commit();
        tablesRefreshed.incrementAndGet();

        System.out.println("category_discount_map refreshed: " + rowsInserted + " rows inserted (" +
                (double) (System.currentTimeMillis() - startTime) / 1000 + " s)");
    }

    private void refreshAnomalyRecords() throws SQLException {
        System.out.println("Refreshing anomaly_records...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        executeUpdate("TRUNCATE TABLE anomaly_records");

        // Insert anomaly records in steps for MySQL compatibility
        executeUpdate(QueryStore.HIGH_REVENUE_ANOMALIES_QUERY);
        executeUpdate(QueryStore.HIGH_QUANTITY_ANOMALIES_QUERY);
        executeUpdate(QueryStore.HIGH_DISCOUNT_ANOMALIES_QUERY);
        executeUpdate(QueryStore.NEGATIVE_VALUE_ANOMALIES_QUERY);
        executeUpdate(QueryStore.PRICING_ANOMALIES_QUERY);

        connection.commit();
        tablesRefreshed.incrementAndGet();

        int totalAnomalies = getTableRowCount("anomaly_records");
        System.out.println("anomaly_records refreshed: " + totalAnomalies + " rows inserted (" +
                (double) (System.currentTimeMillis() - startTime) / 1000 + " s)");
    }

    private void generateSummaryReport() throws SQLException {
        System.out.println("\n=== ANALYTICAL TABLES SUMMARY REPORT ===");

        String[] tables = {"monthly_sales_summary", "top_products", "region_wise_performance",
                "category_discount_map", "anomaly_records"};

        for (String tableName : tables) {
            int rowCount = getTableRowCount(tableName);
            System.out.println(tableName + ": " + rowCount + " rows");
        }
    }

    private int executeUpdate(String query) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            return stmt.executeUpdate();
        }
    }

    private int getTableRowCount(String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Getters for statistics
    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    public long getTablesRefreshed() {
        return tablesRefreshed.get();
    }

    /**
     * Static method to run the refresh process
     */
    public static void main(String[] args) {
        AnalyticalDataRefresher refresher = new AnalyticalDataRefresher();
        refresher.refreshAllAnalyticalTables();
    }

    /**
     * Method to be called from your main ingestion process
     */
    public static void refreshAnalyticsAfterIngestion() {
        System.out.println("\nStarting post-ingestion analytics refresh...");
        AnalyticalDataRefresher refresher = new AnalyticalDataRefresher();
        refresher.refreshAllAnalyticalTables();
    }
}
