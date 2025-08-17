package com.blurr.pipeline.models;

// Ingestion result
public class IngestionResult {
    private final long processedRows;
    private final long processingTimeMs;

    public IngestionResult(long processedRows, long processingTimeMs) {
        this.processedRows = processedRows;
        this.processingTimeMs = processingTimeMs;
    }

    public long getProcessedRows() {
        return processedRows;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public double getRowsPerSecond() {
        return processingTimeMs > 0 ? (processedRows * 1000.0) / processingTimeMs : 0;
    }

    @Override
    public String toString() {
        return String.format("Processed %d rows in %.2f s (%.2f rows/sec)",
                processedRows, (float) processingTimeMs / 1000, getRowsPerSecond());
    }
}
