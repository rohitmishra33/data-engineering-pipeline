package com.blurr.pipeline.handlers.impl;

import com.blurr.pipeline.handlers.DataHandler;
import com.blurr.pipeline.models.ProcessedRecord;

import java.util.ArrayList;
import java.util.List;

// In-memory store handler
public class InMemoryStoreHandler implements DataHandler {
    private final List<ProcessedRecord> store = new ArrayList<>();

    @Override
    public void handle(List<ProcessedRecord> records) {
        synchronized (store) {
            store.addAll(records);
        }
    }

    @Override
    public void close() {
        System.out.println("Total records stored in memory: " + store.size());
    }

    public List<ProcessedRecord> getStore() {
        return new ArrayList<>(store);
    }
}
