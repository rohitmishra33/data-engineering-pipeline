package com.blurr.pipeline.util;

import com.blurr.pipeline.handlers.impl.FileOutputHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MergeCoordinator {
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