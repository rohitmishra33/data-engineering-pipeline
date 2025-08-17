package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;
import com.blurr.pipeline.util.FileMergerUtil;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ThreadLocalFileOutputHandler implements DataHandler, Closeable {

    private static final String OUTPUT_DIR = "output";

    private final Map<String, FileOutputHandler> handlers = new ConcurrentHashMap<>();

    // Static merge coordinator shared across all instances
    private static final MergeCoordinator mergeCoordinator = new MergeCoordinator();

    private final ThreadLocal<FileOutputHandler> threadLocalHandler = ThreadLocal.withInitial(() -> {
        String threadId = Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + "-" + System.nanoTime();
        FileOutputHandler handler = new FileOutputHandler(threadId);
        handlers.put(threadId, handler);

        // Register this handler with the coordinator
        mergeCoordinator.registerHandler(handler);

        return handler;
    });

    @Override
    public void handle(List<ProcessedRecord> records) {
        threadLocalHandler.get().handle(records);
    }

    @Override
    public void close() {
        // Just close this thread's handler, don't attempt to merge
        FileOutputHandler handler = threadLocalHandler.get();
        if (handler != null) {
            handler.close();
            // Signal to coordinator that this handler is done
            mergeCoordinator.markHandlerComplete(handler);
        }
    }

    // Static method to trigger final merge (call this once from anywhere after processing)
    public static void performFinalMerge() {
        mergeCoordinator.performMerge();
    }
}

// Separate coordinator class
class MergeCoordinator {
    private final Set<FileOutputHandler> registeredHandlers = ConcurrentHashMap.newKeySet();
    private final Set<FileOutputHandler> completedHandlers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean merged = new AtomicBoolean(false);

    public void registerHandler(FileOutputHandler handler) {
        registeredHandlers.add(handler);
    }

    public void markHandlerComplete(FileOutputHandler handler) {
        completedHandlers.add(handler);

        // Automatic merge if all registered handlers are complete
        if (completedHandlers.size() == registeredHandlers.size() && !registeredHandlers.isEmpty()) {
            performMerge();
        }
    }

    public void performMerge() {
        if (!merged.compareAndSet(false, true)) {
            return; // Already merged
        }

        try {
            // Ensure all handlers are closed
            registeredHandlers.forEach(h -> {
                try { h.close(); } catch (Exception ignored) {}
            });

            List<Path> tempFiles = registeredHandlers.stream()
                    .map(FileOutputHandler::getTempFilePath)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!tempFiles.isEmpty()) {
                long totalRows = registeredHandlers.stream()
                        .mapToLong(FileOutputHandler::getTotalProcessedRows)
                        .sum();

                String finalFileName = "processed_output_" + totalRows + "_rows.csv";
                FileMergerUtil.mergeFiles(tempFiles, finalFileName);
                System.out.println("Files merged by coordinator: " + finalFileName);
            }
        } catch (Exception e) {
            System.err.println("Error during merge: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
