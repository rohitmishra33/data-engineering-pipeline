package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileOutputHandler implements Closeable, DataHandler {
    private static final String OUTPUT_DIR = "output";

    private BufferedWriter writer;
    private Path tempFilePath;
    private final AtomicLong rowCount = new AtomicLong(0);
    private volatile boolean headerWritten = false;
    private volatile boolean closed = false;
    private String threadIdentifier;

    public FileOutputHandler() {
    }

    public void initializeFileWriter() {
        // Get the current thread info when the handler is actually used
        this.threadIdentifier = Thread.currentThread().getName();

        try {
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Create temp file with unique thread identifier
            this.tempFilePath = outputDir.resolve("temp_output_" + threadIdentifier + ".csv");
            this.writer = new BufferedWriter(new FileWriter(tempFilePath.toFile(), false));

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize FileOutputHandler for thread " + threadIdentifier, e);
        }
    }

    public synchronized void handle(List<ProcessedRecord> records) {
        if (writer == null) {
            initializeFileWriter();
        }
        if (closed) {
            throw new IllegalStateException("Handler is already closed for thread " + threadIdentifier);
        }

        try {
            if (!headerWritten) {
                writeHeader();
                headerWritten = true;
            }

            for (ProcessedRecord record : records) {
                writer.write(toCsvRow(record));
                writer.newLine();
            }

            writer.flush();
            rowCount.addAndGet(records.size());

        } catch (IOException e) {
            throw new RuntimeException("Failed to write batch in thread " + threadIdentifier, e);
        }
    }

    private void writeHeader() throws IOException {
        String header = "order_id,product_name,category,quantity,unit_price," +
                "discount_percent,region,sale_date,customer_email,revenue";
        writer.write(header);
        writer.newLine();
    }

    private String toCsvRow(ProcessedRecord record) {
        return escapeCsv(record.getOrderId()) + "," +
                escapeCsv(record.getProductName()) + "," +
                escapeCsv(record.getCategory()) + "," +
                record.getQuantity() + "," +
                record.getUnitPrice() + "," +
                record.getDiscountPercent() + "," +
                escapeCsv(record.getRegion()) + "," +
                escapeCsv(record.getSaleDate()) + "," +
                escapeCsv(record.getCustomerEmail()) + "," +
                record.getRevenue();
    }

    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }

    public Path getTempFilePath() {
        return tempFilePath;
    }

    public long getTotalProcessedRows() {
        return rowCount.get();
    }

    public String getThreadIdentifier() {
        return threadIdentifier;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("Failed to close output handler for thread " + threadIdentifier + ": " + e.getMessage());
            }
        }
    }
}
