package com.ast_generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodCallReport {
    private Map<String, List<MethodCallEntry>> reportMap = new HashMap<>();

    // Add a method call entry
    public void addEntry(String fileName, String declaringType, String methodName) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        reportMap.get(fileName).add(new MethodCallEntry(declaringType, methodName));
    }

    // Generate JSON report
    public void generateJsonReport(String toFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Convert the string path to a Path object
        Path path = Paths.get(toFilePath);

        // Ensure that the parent directories exist
        if (Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        // Convert the reportMap to JSON and write directly to the specified file
        // This method creates the file if it does not exist
        mapper.writeValue(path.toFile(), reportMap);
    }
}
