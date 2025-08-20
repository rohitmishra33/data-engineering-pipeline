package com.blurr.pipeline.util;

import java.nio.file.*;
import java.util.*;
import java.io.IOException;

public class FileUtil {
    /**
     * Returns a list of Paths in the given directory where filenames start with a prefix.
     * Non-recursive.
     *
     * @param directoryPath the path to the directory to search (as String)
     * @return list of matching Paths
     * @throws IOException if an I/O error occurs
     */
    public static List<Path> findPathsWithPrefix(String directoryPath, String prefix) throws IOException {
        Path dir = Paths.get(directoryPath);
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir,
                entry -> Files.isRegularFile(entry) && entry.getFileName().toString().startsWith(prefix))) {
            for (Path path : stream) {
                result.add(path);
            }
        }
        return result;
    }
}
