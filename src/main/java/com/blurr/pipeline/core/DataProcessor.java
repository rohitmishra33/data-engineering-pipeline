package com.blurr.pipeline.core;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.handlers.impl.DatabaseBatchHandler;
import com.blurr.pipeline.handlers.impl.InMemoryStoreHandler;
import com.blurr.pipeline.handlers.impl.ThreadLocalFileOutputHandler;
import com.blurr.pipeline.models.DataBatch;
import com.blurr.pipeline.models.ProcessedRecord;
import com.blurr.pipeline.models.ProcessingStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

// Data processor with multiple strategies
public class DataProcessor implements Runnable {
    private final BlockingQueue<DataBatch> queue;
    private final AtomicLong processedRows;
    private final DataHandler dataHandler;

    public DataProcessor(BlockingQueue<DataBatch> queue, AtomicLong processedRows, IngestionConfig config) {
        this.queue = queue;
        this.processedRows = processedRows;
        this.dataHandler = createDataHandler(config.getStrategy());
    }

    private DataHandler createDataHandler(ProcessingStrategy strategy) {
        switch (strategy) {
            case DATABASE_BATCH:
                return new DatabaseBatchHandler();
            case FILE_OUTPUT:
                return new ThreadLocalFileOutputHandler();
            case IN_MEMORY_STORE:
                return new InMemoryStoreHandler();
//            case CUSTOM_HANDLER:
//                return new CustomDataHandler();
            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                DataBatch batch = queue.take();

                if (batch.isPoisonPill()) {
                    break;
                }

                processBatch(batch);
                processedRows.addAndGet(batch.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException("Data processor failed", e);
        } finally {
            dataHandler.close();
        }
    }

    private void processBatch(DataBatch batch) {
        List<ProcessedRecord> processedRecords = new ArrayList<>(batch.size());

        for (String[] row : batch.getRows()) {
            try {
                ProcessedRecord record = validateAndTransform(row);
                if (record != null) {
                    processedRecords.add(record);
                }
            } catch (Exception e) {
                // Log error and continue processing
                System.err.println("Error processing row: " + Arrays.toString(row) + " - " + e.getMessage());
            }
        }

        if (!processedRecords.isEmpty()) {
            dataHandler.handle(processedRecords);
        }
    }

    private ProcessedRecord validateAndTransform(String[] row) {
        if (row.length < 10) {
            throw new IllegalArgumentException("Row has insufficient columns");
        }

        try {
            return ProcessedRecord.builder()
                    .orderId(row[0])
                    .productName(cleanProductName(row[1]))
                    .category(normalizeCategory(row[1]))
                    .quantity(parseQuantity(row[2]))
                    .unitPrice(parseDouble(row[3]))
                    .discountPercent(validateDiscount(parseDouble(row[4])))
                    .region(normalizeRegion(row[5]))
                    .saleDate(parseDate(row[6]))
                    .customerEmail(validateEmail(row[7]))
                    .revenue(parseDouble(row[8]))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to transform row", e);
        }
    }

    // Data validation and transformation methods
    private String cleanProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return "Unknown Product";
        }
        return productName.trim().toLowerCase();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "uncategorized";
        }
        return category.trim().toLowerCase()
                .replaceAll("applicance", "appliance")
                .replaceAll("electronic.*", "electronics");
    }

    private int parseQuantity(String quantity) {
        try {
            int qty = Integer.parseInt(quantity.trim());
            return Math.max(0, qty); // Ensure non-negative
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double validateDiscount(double discount) {
        return Math.min(Math.max(0.0, discount), 1.0);
    }

    private String normalizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return "unknown";
        }
        return region.trim().toLowerCase();
    }

    private String parseDate(String dateStr) {
        // Implement date parsing logic for various formats
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        // Add date parsing implementation
        return dateStr;
    }

    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.contains("@") ? email.trim() : null;
    }
}