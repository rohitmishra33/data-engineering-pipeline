package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

// Database batch handler
public class DatabaseBatchHandler implements DataHandler {
    private final Connection connection;
    private final PreparedStatement preparedStatement;
    private int batchCount = 0;
    private static final int BATCH_SIZE = 5000;

    public DatabaseBatchHandler() {
        try {
            // Initialize database connection
            this.connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/yourdb",
                    "username",
                    "password"
            );
            this.connection.setAutoCommit(false);

            String sql = "INSERT INTO sales_data (order_id, product_name, category, quantity, unit_price, discount_percent,region, sale_date, customereail, revenue) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            this.preparedStatement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database handler", e);
        }
    }

    @Override
    public void handle(List<ProcessedRecord> records) {
        try {
            for (ProcessedRecord record : records) {
                preparedStatement.setString(1, record.getOrderId());
                preparedStatement.setString(2, record.getProductName());
                preparedStatement.setString(3, record.getCategory());
                preparedStatement.setInt(4, record.getQuantity());
                preparedStatement.setDouble(5, record.getUnitPrice());
                preparedStatement.setDouble(6, record.getDiscountPercent());
                preparedStatement.setString(7, record.getRegion());
                preparedStatement.setString(8, record.getSaleDate());
                preparedStatement.setString(9, record.getCustomerEmail());
                preparedStatement.setDouble(10, record.getRevenue());

                preparedStatement.addBatch();
                batchCount++;

                if (batchCount >= BATCH_SIZE) {
                    preparedStatement.executeBatch();
                    connection.commit();
                    batchCount = 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to handle database batch", e);
        }
    }

    @Override
    public void close() {
        try {
            if (batchCount > 0) {
                preparedStatement.executeBatch();
                connection.commit();
            }
            if (preparedStatement != null) preparedStatement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing database handler: " + e.getMessage());
        }
    }
}
