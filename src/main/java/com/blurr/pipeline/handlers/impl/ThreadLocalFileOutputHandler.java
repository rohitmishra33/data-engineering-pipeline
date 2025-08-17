package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.FileMergerUtil;
import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ThreadLocalFileOutputHandler implements DataHandler, Closeable {

    private static final String OUTPUT_DIR = "output";
    private final Map<Thread, FileOutputHandler> handlers = new ConcurrentHashMap<>();

    // ThreadLocal to lazily create a handler per thread
    private final ThreadLocal<FileOutputHandler> threadLocalHandler = ThreadLocal.withInitial(() -> {
        String threadId = Thread.currentThread().getName().replaceAll("[^a-zA-Z0-9]", "_");
        FileOutputHandler handler = new FileOutputHandler(threadId);
        handlers.put(Thread.currentThread(), handler);
        return handler;
    });

    @Override
    public void handle(List<ProcessedRecord> records) {
        // Delegate to thread-specific handler
        threadLocalHandler.get().handle(records);
    }

    @Override
    public void close() {
        // Close all per-thread handlers
        handlers.values().forEach(FileOutputHandler::close);

        // Collect all temp files paths
        List<Path> tempFiles = handlers.values().stream()
                .map(FileOutputHandler::getTempFilePath)
                .collect(Collectors.toList());

        // Sum all processed rows
        long totalRows = handlers.values().stream()
                .mapToLong(FileOutputHandler::getTotalProcessedRows)
                .sum();

        String finalFileName = "processed_output_" + totalRows + "_rows.csv";

        System.out.println(finalFileName);

        try {
            FileMergerUtil.mergeFiles(tempFiles, finalFileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge thread output files", e);
        }
    }
}
