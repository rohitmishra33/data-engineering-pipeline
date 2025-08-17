package com.blurr.pipeline.models;

import java.util.Collections;
import java.util.List;

// Data batch container
public class DataBatch {
    public static final DataBatch POISON_PILL = new DataBatch(Collections.emptyList());

    private final List<String[]> rows;
    private final long timestamp;

    public DataBatch(List<String[]> rows) {
        this.rows = rows;
        this.timestamp = System.currentTimeMillis();
    }

    public List<String[]> getRows() {
        return rows;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isPoisonPill() {
        return this == POISON_PILL;
    }

    public int size() {
        return rows.size();
    }
}
