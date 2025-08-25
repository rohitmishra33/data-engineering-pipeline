package com.blurr.pipeline.app;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.core.ScalableCSVIngestion;
import com.blurr.pipeline.core.analytics.AnalyticalDataRefresher;
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
                .strategy(ProcessingStrategy.valueOf(dotenv.get("INGESTION_STRATEGY")))
                .build();

        ScalableCSVIngestion ingestion = new ScalableCSVIngestion(config);

        try {
            System.out.println("Starting CSV ingestion...");
            IngestionResult result = ingestion.ingestFile(dotenv.get("INGESTION_FILE"));
            System.out.println("Ingestion completed: " + result);
        } catch (Exception e) {
            System.err.println("Ingestion failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ingestion.shutdown();
        }

        // Run Analytics
        if (!Boolean.parseBoolean(dotenv.get("SKIP_ANALYTICS_UPDATE"))
                && ProcessingStrategy.DATABASE_BATCH.equals(ProcessingStrategy.valueOf(dotenv.get("INGESTION_STRATEGY")))) {
            AnalyticalDataRefresher.refreshAnalyticsAfterIngestion();
        }
    }
}