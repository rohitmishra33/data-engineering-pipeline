package com.blurr.pipeline.app;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.core.analytics.AnalyticalDataRefresher;
import com.blurr.pipeline.core.ScalableCSVIngestion;
import com.blurr.pipeline.models.IngestionResult;
import com.blurr.pipeline.models.ProcessingStrategy;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;

public class Main {
    public static final Dotenv dotenv = Dotenv.configure().load();

    public static void main(String[] args) {
        // Configure Ingestion
        IngestionConfig config = new IngestionConfig.Builder()
                .batchSize(Integer.parseInt(Objects.requireNonNull(dotenv.get("BATCH_SIZE"))))
                .coreThreads(Integer.parseInt(Objects.requireNonNull(dotenv.get("CORE_THREADS"))))
                .maxThreads(Integer.parseInt(Objects.requireNonNull(dotenv.get("MAX_THREADS"))))
                .processorThreads(Integer.parseInt(Objects.requireNonNull(dotenv.get("PROCESSOR_THREADS"))))
                .queueCapacity(Integer.parseInt(Objects.requireNonNull(dotenv.get("QUEUE_CAPACITY"))))
                .skipHeader(Boolean.getBoolean(dotenv.get("SKIP_HEADER")))
                .strategy(ProcessingStrategy.valueOf(dotenv.get("STRATEGY")))
                .build();

        ScalableCSVIngestion ingestion = new ScalableCSVIngestion(config);

        try {
            System.out.println("Starting ingestion of 100M rows...");
            IngestionResult result = ingestion.ingestFile("sample_data_10000000_rows.csv");
            System.out.println("Ingestion completed: " + result);
        } catch (Exception e) {
            System.err.println("Ingestion failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ingestion.shutdown();
        }

        // Run Analytics
        if (ProcessingStrategy.DATABASE_BATCH.equals(ProcessingStrategy.valueOf(dotenv.get("STRATEGY")))) {
            AnalyticalDataRefresher.refreshAnalyticsAfterIngestion();
        }
    }
}