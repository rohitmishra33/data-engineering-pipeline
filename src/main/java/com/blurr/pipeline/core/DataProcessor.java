package com.blurr.pipeline.core;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.handlers.impl.DatabaseBatchHandler;
import com.blurr.pipeline.handlers.impl.InMemoryStoreHandler;
import com.blurr.pipeline.handlers.impl.ThreadLocalFileOutputHandler;
import com.blurr.pipeline.models.DataBatch;
import com.blurr.pipeline.models.ProcessedRecord;
import com.blurr.pipeline.models.ProcessingStrategy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

// Data processor with multiple strategies
public class DataProcessor implements Runnable {
    private final BlockingQueue<DataBatch> queue;
    private final AtomicLong processedRows;
    private final DataHandler dataHandler;
    private final int MAX_DISCOUNT_PERCENT = 70;

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
            ProcessedRecord processedRecord = ProcessedRecord.builder()
                    .orderId(row[0])
                    .productName(cleanProductName(row[1]))
                    .category(normalizeCategory(row[2]))
                    .quantity(parseQuantity(row[3]))
                    .unitPrice(parseDouble(row[4]))
                    .discountPercent(validateDiscount(parseDouble(row[5])))
                    .region(normalizeRegion(row[6]))
                    .saleDate(parseDate(row[7]))
                    .customerEmail(validateEmail(row[8]))
                    .build();
            processedRecord.setRevenue(calculateRevenue(processedRecord.getQuantity(),
                    processedRecord.getUnitPrice(),
                    processedRecord.getDiscountPercent()));
            return processedRecord;
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
            return Math.max(1, qty); // Ensure non-negative and at least 1 unit for a valid order
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
        if (discount < 0) {
            return 0.0;
        } else if (discount > MAX_DISCOUNT_PERCENT) {
            return MAX_DISCOUNT_PERCENT;
        } else {
            return discount;
        }
    }

    private String normalizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return "unknown";
        }
        switch (Character.toLowerCase(region.charAt(0))) {
            case 'n':
                return "North";
            case 'e':
                return "East";
            case 'w':
                return "West";
            case 's':
                return "South";
        }
        return region.trim().toLowerCase();
    }

    private String parseDate(String dateStr) {
        // Return null for null or empty input
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Define all possible input formats
        String[] possibleFormats = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM-dd-yyyy",
                "MMMM d, yyyy"
        };

        // Try each format until one works
        for (String format : possibleFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false); // Strict parsing - rejects invalid dates like "2024-13-40"
                Date date = sdf.parse(dateStr.trim());

                // Return in standardized format (or whatever format you need)
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(date);

            } catch (ParseException ignored) {
                // Continue to next format
            }
        }

        // If no format worked, return null or original string
        return dateStr; // or return dateStr; to keep original
    }

    private String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.contains("@") && email.contains(".com") ? email.trim() : null;
    }

    private double calculateRevenue(int quantity, double unitPrice, double discountPercent) {
        return Math.round(quantity * unitPrice * (100 - discountPercent)) / 100.0;
    }
}