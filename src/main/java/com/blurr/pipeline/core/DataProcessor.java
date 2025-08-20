package com.blurr.pipeline.core;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.handlers.impl.DatabaseBatchHandler;
import com.blurr.pipeline.handlers.impl.FileOutputHandler;
import com.blurr.pipeline.handlers.impl.InMemoryStoreHandler;
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
    private static final int MAX_DISCOUNT_PERCENT = 70;
    private static final String[] POSSIBLE_FORMATS = {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM-dd-yyyy",
            "MMMM d, yyyy"
    };
    private static final ThreadLocal<SimpleDateFormat[]> THREAD_LOCAL_FORMATTERS = ThreadLocal.withInitial(() -> {
        SimpleDateFormat[] sdfs = new SimpleDateFormat[POSSIBLE_FORMATS.length];
        for (int i = 0; i < POSSIBLE_FORMATS.length; i++) {
            sdfs[i] = new SimpleDateFormat(POSSIBLE_FORMATS[i]);
            sdfs[i].setLenient(false);
        }
        return sdfs;
    });
    private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_OUTPUT_FORMATTER = ThreadLocal.withInitial(() -> {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        return sdf;
    });

    public DataProcessor(BlockingQueue<DataBatch> queue, AtomicLong processedRows, IngestionConfig config) {
        this.queue = queue;
        this.processedRows = processedRows;
        this.dataHandler = createDataHandler(config.getStrategy());
    }

    private DataHandler createDataHandler(ProcessingStrategy strategy) {
        return switch (strategy) {
            case DATABASE_BATCH -> new DatabaseBatchHandler();
            case FILE_OUTPUT -> new FileOutputHandler();
            case IN_MEMORY_STORE -> new InMemoryStoreHandler();
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
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
                processedRecords.add(record);
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

    public double validateDiscount(double discount) {
        if (discount < 0) {
            return 0.0;
        } else if (discount > MAX_DISCOUNT_PERCENT) {
            return MAX_DISCOUNT_PERCENT;
        } else {
            return discount;
        }
    }

    public String normalizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return "unknown";
        }
        return switch (Character.toLowerCase(region.charAt(0))) {
            case 'n' -> "North";
            case 'e' -> "East";
            case 'w' -> "West";
            case 's' -> "South";
            default -> region.trim().toLowerCase();
        };
    }

    public String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.contains("@") && email.contains(".com") ? email.trim() : null;
    }

    public double calculateRevenue(int quantity, double unitPrice, double discountPercent) {
        return Math.round(quantity * unitPrice * (100 - discountPercent)) / 100.0;
    }

    public String parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String trimmedDateStr = dateStr.trim();
        SimpleDateFormat[] sdfs = THREAD_LOCAL_FORMATTERS.get();
        for (SimpleDateFormat sdf : sdfs) {
            try {
                Date date = sdf.parse(trimmedDateStr);
                return THREAD_LOCAL_OUTPUT_FORMATTER.get().format(date);
            } catch (ParseException ignored) {
                // try next format
            }
        }
        return null;
    }
}