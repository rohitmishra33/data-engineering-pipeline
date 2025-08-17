package com.blurr.pipeline.handlers;

import com.blurr.pipeline.models.ProcessedRecord;

import java.util.List;

// Base data handler interface
public interface DataHandler {
    void handle(List<ProcessedRecord> records);

    void close();
}

