package com.blurr.pipeline.util;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class FileMergerUtil {

    private static final String OUTPUT_DIR = "output";

    /**
     * Merge multiple CSV temp files into one final CSV file with header once.
     * Deletes temp files after merging.
     * @param tempFiles List of temp file Paths to merge
     * @param finalFileName final output CSV file name (including directory)
     * @throws IOException on I/O errors
     */
    public static void mergeFiles(List<Path> tempFiles, String finalFileName) throws IOException {
        if (tempFiles == null || tempFiles.isEmpty()) {
            throw new IllegalArgumentException("No temp files provided for merging");
        }

        Path outputDir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        Path finalPath = outputDir.resolve(finalFileName);

        try (BufferedWriter writer = Files.newBufferedWriter(finalPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            boolean headerWritten = false;

            for (Path tempFile : tempFiles) {
                try (BufferedReader reader = Files.newBufferedReader(tempFile)) {
                    String line;
                    boolean isFirstLine = true;
                    while ((line = reader.readLine()) != null) {
                        // Write the header only for the first file
                        if (isFirstLine) {
                            isFirstLine = false;
                            if (headerWritten) {
                                continue; // skip header line on subsequent files
                            } else {
                                headerWritten = true;
                            }
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
            writer.flush();
        }

        // Clean up temp files
        for (Path tempFile : tempFiles) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: failed to delete temp file " + tempFile + ": " + e.getMessage());
            }
        }
        System.out.println("Merged output file created: " + finalPath.toString());
    }
}
