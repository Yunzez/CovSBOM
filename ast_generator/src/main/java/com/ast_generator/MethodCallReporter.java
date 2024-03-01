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
    public void addEntry(String fileName, String declaringType, String methodName, int lineNumber, String fullExpression, String newPackageName) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        reportMap.get(fileName).add(new MethodCallEntry(declaringType, methodName, lineNumber, fullExpression));
        System.out.println("added entry");
        if (parentPackageName == null || (newPackageName.length() < parentPackageName.length()
                && parentPackageName.startsWith(newPackageName))) {
            parentPackageName = newPackageName;
        }
    }

    public void addDeclarationInfoForMethodinType(String declaringType, MethodDeclarationInfo declarationInfo) {
        String methodName = declarationInfo.getMethodName();
        if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")) {
            return;
        }

        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                if (entry.getDeclaringType().equals(declaringType)) {
                    // System.out.println("looking for method: " + methodName + " in type: " + declaringType);
                    if (entry.getMethodName().equals(methodName)) {
                        entry.setDeclarationInfo(declarationInfo);
                    }
                }
            }
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


        // ! Print out the entries with methodDeclarationInfo
        // filteredReportMap.forEach((fileName, methodCallEntries) -> {
        //     System.out.println("Entries in file: " + fileName);
        //     for (MethodCallEntry entry : methodCallEntries) {
        //         System.out.println("Declaring Type: " + entry.getDeclaringType());
        //         System.out.println("Method Name: " + entry.getMethodName());
        //         System.out.println("Line Number: " + entry.getLineNumber());
        //         System.out.println("Full Expression: " + entry.getFullExpression());
        //         System.out.println("Declaration Info: " + entry.getDeclarationInfo());
        //         System.out.println();
        //     }
        // });


        reportMap.forEach((fileName, methodCallEntries) -> {
            List<MethodCallEntry> filteredEntries = new ArrayList<>();
            for (MethodCallEntry entry : methodCallEntries) {
                if (!(entry.getDeclaringType().startsWith("java.") ||
                        entry.getDeclaringType().startsWith("javax.") ||
                        entry.getDeclaringType().startsWith(parentPackageName))) {
                    // if (entry.getDeclarationInfo() != null) {
                        filteredEntries.add(entry);
                    // }
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
