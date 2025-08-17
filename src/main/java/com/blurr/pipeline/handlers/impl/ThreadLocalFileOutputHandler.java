package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;
import com.blurr.pipeline.util.MergeCoordinator;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
