package com.blurr.pipeline.models;

public enum ProcessingStrategy {
    DATABASE_BATCH,
    FILE_OUTPUT,
    IN_MEMORY_STORE,
    CUSTOM_HANDLER
}
