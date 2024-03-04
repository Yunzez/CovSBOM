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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodCallReporter {
    private Map<String, List<MethodCallEntry>> reportMap = new HashMap<>();
    private String parentPackageName;

    // Add a method call entry
    public void addEntry(String fileName, String declaringType, String methodName, int lineNumber,
            String fullExpression, String singature, String newPackageName) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        reportMap.get(fileName)
                .add(new MethodCallEntry(declaringType, methodName, lineNumber, fullExpression, singature));
        if (parentPackageName == null || (newPackageName.length() < parentPackageName.length()
                && parentPackageName.startsWith(newPackageName))) {
            parentPackageName = newPackageName;
        }
    }

    public boolean addDeclarationInfoForMethodinType(String declaringType, MethodDeclarationInfo declarationInfo) {
        String methodName = declarationInfo.getMethodName();
        if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")) {
            return false;
        }

        Boolean ret = false;
        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                if (entry.getDeclaringType().equals(declaringType)) {
                    // System.out.println("looking for method: " + methodName + " in type: " +
                    // declaringType);

                    if (entry.getMethodName().equals(methodName)) {
                        if (entry.getMethodName().equals("assertTrue")) {
                            System.out.println("checking in reporter: " + entry.getMethodSignature() + " "
                                    + declarationInfo.getDeclarationSignature());
                        }
                        entry.setDeclarationInfo(declarationInfo);
                        ret = true;
                    }
                }
            }
        }
        return ret;

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

    public Map<String, List<MethodCallEntry>> getReportMap() {
        return reportMap;
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

    public void generateThirdPartyTypeJsonReportBasedonPackage(String toFilePath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        Map<MethodSignatureKey, MethodCallEntry> uniqueMethodDeclarations = new HashMap<>();

        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                if (!(entry.getDeclaringType().startsWith("java.") ||
                        entry.getDeclaringType().startsWith("javax.") ||
                        entry.getDeclaringType().startsWith(parentPackageName))) {

                    MethodSignatureKey key = new MethodSignatureKey(entry.getDeclaringType(),
                            entry.getMethodSignature());
                    uniqueMethodDeclarations.putIfAbsent(key, entry);
                }
            }
        }

        // Now you have a map of unique method declarations, you can convert it to a
        // list or directly use it for JSON generation
        List<MethodCallEntry> allMethodDeclarationInfos = new ArrayList<>(uniqueMethodDeclarations.values());

        // Generate JSON
        mapper.writeValue(new File(toFilePath), allMethodDeclarationInfos);

    }

}
