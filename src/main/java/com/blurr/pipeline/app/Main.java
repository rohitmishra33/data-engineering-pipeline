package com.blurr.pipeline.app;

import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.core.AnalyticalDataRefresher;
import com.blurr.pipeline.core.ScalableCSVIngestion;
import com.blurr.pipeline.models.IngestionResult;
import com.blurr.pipeline.models.ProcessingStrategy;

public class Main {
    public static void main(String[] args) {
        // Configure ingestion
        IngestionConfig config = new IngestionConfig.Builder()
                .batchSize(5000)  // Larger batches for 100M rows
                .coreThreads(8)
                .maxThreads(10)
                .processorThreads(6)
                .queueCapacity(500)
                .skipHeader(true)
                .strategy(ProcessingStrategy.FILE_OUTPUT)
                .build();

        ScalableCSVIngestion ingestion = new ScalableCSVIngestion(config);

//        new Scanner(System.in).nextLine();

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

        AnalyticalDataRefresher.refreshAnalyticsAfterIngestion();
    }
}