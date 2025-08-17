package com.blurr.pipeline.config;

import com.blurr.pipeline.models.ProcessingStrategy;

public class IngestionConfig {
    private final int batchSize;
    private final int coreThreads;
    private final int maxThreads;
    private final int processorThreads;
    private final int queueCapacity;
    private final boolean skipHeader;
    private final ProcessingStrategy strategy;

    public static class Builder {
        private int batchSize = 10000;
        private int coreThreads = Runtime.getRuntime().availableProcessors();
        private int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
        private int processorThreads = Runtime.getRuntime().availableProcessors();
        private int queueCapacity = 1000;
        private boolean skipHeader = true;
        private ProcessingStrategy strategy = ProcessingStrategy.DATABASE_BATCH;

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder coreThreads(int coreThreads) {
            this.coreThreads = coreThreads;
            return this;
        }

        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public Builder processorThreads(int processorThreads) {
            this.processorThreads = processorThreads;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder skipHeader(boolean skipHeader) {
            this.skipHeader = skipHeader;
            return this;
        }

        public Builder strategy(ProcessingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public IngestionConfig build() {
            return new IngestionConfig(this);
        }
    }

    private IngestionConfig(Builder builder) {
        this.batchSize = builder.batchSize;
        this.coreThreads = builder.coreThreads;
        this.maxThreads = builder.maxThreads;
        this.processorThreads = builder.processorThreads;
        this.queueCapacity = builder.queueCapacity;
        this.skipHeader = builder.skipHeader;
        this.strategy = builder.strategy;
    }

    // Getters
    public int getBatchSize() {
        return batchSize;
    }

    public int getCoreThreads() {
        return coreThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getProcessorThreads() {
        return processorThreads;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public ProcessingStrategy getStrategy() {
        return strategy;
    }
}
