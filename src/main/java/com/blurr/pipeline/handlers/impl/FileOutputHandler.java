package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileOutputHandler implements DataHandler, Closeable {

    private static final String OUTPUT_DIR = "output";

    private BufferedWriter writer;
    private boolean headerWritten = false;
    private final Path tempFilePath;
    private AtomicLong totalProcessedRows = new AtomicLong(0);

    public FileOutputHandler(String threadId) {
        try {
            Path outputDir = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            this.tempFilePath = outputDir.resolve("temp_output_" + threadId + ".csv");
            this.writer = new BufferedWriter(new FileWriter(tempFilePath.toFile(), false));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize FileOutputHandler for thread " + threadId, e);
        }
    }

    @Override
    public synchronized void handle(List<ProcessedRecord> records) {
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
            totalProcessedRows.addAndGet(records.size());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write batch to file: " + tempFilePath, e);
        }
    }

    private void writeHeader() throws IOException {
        String header = "order_id,product_name,category,quantity,unit_price,discount_percent,region,sale_date,customer_email,revenue";
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
        return totalProcessedRows.get();
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException e) {
                System.err.println("Failed to close file writer: " + e.getMessage());
            }
        }
    }
}
