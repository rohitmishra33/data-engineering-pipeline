package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseBatchHandler implements DataHandler {
    private final Connection connection;
    private final PreparedStatement insertIgnoreStatement;

    // Use ArrayDeque for O(1) removals instead of ArrayList
    private final ArrayDeque<ProcessedRecord> pendingRecords = new ArrayDeque<>();

    // Pre-allocated batch list to avoid repeated allocations
    private final List<ProcessedRecord> reusableBatch = new ArrayList<>(BATCH_SIZE);

    private static final int BATCH_SIZE = 100;
    private static final int MAX_DEADLOCK_RETRIES = 3;
    private static final Random random = new Random();

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong deadlockRetries = new AtomicLong(0);
    private final AtomicLong successfulBatches = new AtomicLong(0);
    private long initialRowCount = 0;

    public DatabaseBatchHandler() {
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/dev_db?" +
                            "useServerPrepStmts=true&" +
                            "rewriteBatchedStatements=true&" +
                            "useLocalSessionState=true&" +
                            "sessionVariables=transaction_isolation='READ-COMMITTED'&" +
                            "useLocalTransactionState=true",
                    "dev",
                    "dev_12345"
            );

            this.connection.setAutoCommit(false);
            this.connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            String sql = "INSERT IGNORE INTO sales_data (order_id," +
                    "product_name, category, quantity, unit_price," +
                    "discount_percent, region, sale_date, customer_email, revenue) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            this.insertIgnoreStatement = connection.prepareStatement(sql);
            this.initialRowCount = getCurrentRowCount();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database handler", e);
        }
    }

    @Override
    public void handle(List<ProcessedRecord> records) {
        // Add all records to pending queue - O(1) for each add
        pendingRecords.addAll(records);
        totalProcessed.addAndGet(records.size());

        // Process pending records in batches
        while (pendingRecords.size() >= BATCH_SIZE) {
            // Clear and reuse the same batch list
            reusableBatch.clear();

            // Extract exactly BATCH_SIZE records - O(1) removal from front
            for (int i = 0; i < BATCH_SIZE && !pendingRecords.isEmpty(); i++) {
                reusableBatch.add(pendingRecords.removeFirst()); // O(1) operation
            }

            processBatchReliably(reusableBatch);
        }
    }

    private void processBatchReliably(List<ProcessedRecord> batchRecords) {
        int retryCount = 0;
        boolean batchProcessed = false;

        while (!batchProcessed && retryCount < MAX_DEADLOCK_RETRIES) {
            try {
                // Prepare the entire batch once per attempt
                for (ProcessedRecord record : batchRecords) {
                    setParameters(record);
                    insertIgnoreStatement.addBatch();
                }

                // Execute batch
                insertIgnoreStatement.executeBatch();
                connection.commit();
                insertIgnoreStatement.clearBatch();

                batchProcessed = true;
                successfulBatches.incrementAndGet();

                if (retryCount > 0) {
                    System.out.println(Thread.currentThread().getName() +
                            " - Batch succeeded after " + retryCount + " retries");
                }

            } catch (SQLException ex) {
                // Clean up batch state immediately
                try {
                    connection.rollback();
                    insertIgnoreStatement.clearBatch();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }

                if (isDeadlockException(ex) && retryCount < MAX_DEADLOCK_RETRIES - 1) {
                    retryCount++;
                    deadlockRetries.incrementAndGet();

                    try {
                        long backoffMs = 25 + random.nextInt(25); // Shorter backoff
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during deadlock retry", ie);
                    }
                } else {
                    // Max retries reached or non-deadlock error
                    System.err.println(Thread.currentThread().getName() +
                            " - Batch failed: " + ex.getMessage());
                    batchProcessed = true; // Skip this batch to avoid infinite loop
                }
            }
        }
    }

    // Extract parameter setting to reduce code duplication
    private void setParameters(ProcessedRecord record) throws SQLException {
        insertIgnoreStatement.setString(1, record.getOrderId());
        insertIgnoreStatement.setString(2, record.getProductName());
        insertIgnoreStatement.setString(3, record.getCategory());
        insertIgnoreStatement.setInt(4, record.getQuantity());
        insertIgnoreStatement.setDouble(5, record.getUnitPrice());
        insertIgnoreStatement.setDouble(6, record.getDiscountPercent());
        insertIgnoreStatement.setString(7, record.getRegion());
        insertIgnoreStatement.setString(8, record.getSaleDate());
        insertIgnoreStatement.setString(9, record.getCustomerEmail());
        insertIgnoreStatement.setDouble(10, record.getRevenue());
    }

    private boolean isDeadlockException(SQLException ex) {
        int errorCode = ex.getErrorCode();
        return errorCode == 1213 || errorCode == 1205 ||
                ex.getMessage().toLowerCase().contains("deadlock") ||
                ex.getMessage().toLowerCase().contains("lock wait timeout");
    }

    private long getCurrentRowCount() {
        try (PreparedStatement countStmt = connection.prepareStatement("SELECT COUNT(*) FROM sales_data");
             ResultSet rs = countStmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Failed to get row count: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public void close() {
        try {
            // Process any remaining pending records
            while (!pendingRecords.isEmpty()) {
                reusableBatch.clear();
                int batchSize = Math.min(BATCH_SIZE, pendingRecords.size());

                for (int i = 0; i < batchSize; i++) {
                    reusableBatch.add(pendingRecords.removeFirst()); // O(1)
                }

                processBatchReliably(reusableBatch);
            }

            long finalRowCount = getCurrentRowCount();

            System.out.println("\n=== Database Insertion Summary ===");
            System.out.println("Thread Name: " + Thread.currentThread().getName());
            System.out.println("Total records processed: " + totalProcessed.get());
            System.out.println("Successful batches: " + successfulBatches.get());
            System.out.println("Total deadlock retries: " + deadlockRetries.get());
            System.out.println("Initial row count: " + initialRowCount);
            System.out.println("Final row count: " + finalRowCount);

            if (insertIgnoreStatement != null) insertIgnoreStatement.close();
            if (connection != null) connection.close();

        } catch (SQLException e) {
            System.err.println("Error closing database handler: " + e.getMessage());
        }
    }
}
