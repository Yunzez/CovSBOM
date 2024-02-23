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

public class MethodCallReporter {
    private Map<String, List<MethodCallEntry>> reportMap = new HashMap<>();
    private String parentPackageName;

    // Add a method call entry
    public void addEntry(String fileName, String declaringType, String methodName, String newPackageName) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        reportMap.get(fileName).add(new MethodCallEntry(declaringType, methodName));

        if (parentPackageName == null || (newPackageName.length() < parentPackageName.length()
                && parentPackageName.startsWith(newPackageName))) {
            parentPackageName = newPackageName;
        }
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

    public void generateThirdPartyTypeJsonReport(String toFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Prepare a filtered report map excluding standard Java library types
        Map<String, List<MethodCallEntry>> filteredReportMap = new HashMap<>();

        reportMap.forEach((fileName, methodCallEntries) -> {
            List<MethodCallEntry> filteredEntries = new ArrayList<>();
            for (MethodCallEntry entry : methodCallEntries) {
                if (!(entry.getDeclaringType().startsWith("java.") ||
                        entry.getDeclaringType().startsWith("javax.") ||
                        entry.getDeclaringType().startsWith(parentPackageName))) {
                    filteredEntries.add(entry);
                }
            }
            if (!filteredEntries.isEmpty()) {
                filteredReportMap.put(fileName, filteredEntries);
            }
        });

        // Convert the string path to a Path object
        Path path = Paths.get(toFilePath);

        // Ensure that the parent directories exist
        if (Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        // Convert the filteredReportMap to JSON and write directly to the specified
        // file
        // This method creates the file if it does not exist
        mapper.writeValue(path.toFile(), filteredReportMap);
        System.out.println("parentPackageName: " + parentPackageName);
    }

    public Map<String, List<MethodCallEntry>> getReportMap() {
        return reportMap;
    }
}
