package com.blurr.pipeline.core;

// Main ingestion orchestrator class

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.models.DataBatch;
import com.blurr.pipeline.models.IngestionResult;
import com.blurr.pipeline.models.ProcessingStrategy;
import com.blurr.pipeline.util.FileMergerUtil;
import com.blurr.pipeline.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ScalableCSVIngestion {
    private final IngestionConfig config;
    private final ExecutorService executorService;
    private final BlockingQueue<DataBatch> processingQueue;
    private final AtomicLong processedRows = new AtomicLong(0);

    public ScalableCSVIngestion(IngestionConfig config) {
        this.config = config;
        this.executorService = new ThreadPoolExecutor(
                config.getCoreThreads(),
                config.getMaxThreads(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = new Thread(r, "CSV-Processor-" + counter.incrementAndGet());
                        t.setDaemon(false);
                        return t;
                    }
                }
        );
        this.processingQueue = new LinkedBlockingQueue<>(config.getQueueCapacity());
    }

    public IngestionResult ingestFile(String filePath) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Start consumer threads
        List<Future<?>> consumers = startConsumers();

        // Start producer (file reader)
        Future<?> producer = startProducer(filePath);

        // Wait for completion
        producer.get();

        // Signal completion to consumers
        for (int i = 0; i < config.getProcessorThreads(); i++) {
            processingQueue.offer(DataBatch.POISON_PILL);
        }

        // Wait for all consumers to finish
        for (Future<?> consumer : consumers) {
            consumer.get();
        }

        boolean allDone = consumers.stream().allMatch(Future::isDone);

        if (ProcessingStrategy.FILE_OUTPUT.equals(config.getStrategy())) {
            List<Path> processedTempFiles = FileUtil.findPathsWithPrefix("output", "temp_output_");
            FileMergerUtil.mergeFiles(processedTempFiles, "final_output_" + processedRows.get() + "_rows.csv");
        }

        long endTime = System.currentTimeMillis();
        return new IngestionResult(processedRows.get(), endTime - startTime);
    }

    private Future<?> startProducer(String filePath) {
        return executorService.submit(() -> {
            try {
                readFileInChunks(filePath);
            } catch (Exception e) {
                throw new RuntimeException("Producer failed", e);
            }
        });
    }

    private List<Future<?>> startConsumers() {
        List<Future<?>> consumers = new ArrayList<>();
        for (int i = 0; i < config.getProcessorThreads(); i++) {
            consumers.add(executorService.submit(new DataProcessor(processingQueue, processedRows, config)));
        }
        return consumers;
    }

    private void readFileInChunks(String filePath) throws IOException, InterruptedException {
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(filePath),
                StandardCharsets.UTF_8)) {

            String line;
            List<String[]> batch = new ArrayList<>(config.getBatchSize());
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header if configured
                if (isFirstLine && config.isSkipHeader()) {
                    isFirstLine = false;
                    continue;
                }

                String[] row = parseCsvLine(line);
                batch.add(row);

                if (batch.size() >= config.getBatchSize()) {
                    processingQueue.put(new DataBatch(new ArrayList<>(batch)));
                    batch.clear();
                }
            }

            // Process remaining rows
            if (!batch.isEmpty()) {
                processingQueue.put(new DataBatch(batch));
            }
        }
    }

    private String[] parseCsvLine(String line) {
        // Optimized CSV parsing - handles quotes and escapes
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
