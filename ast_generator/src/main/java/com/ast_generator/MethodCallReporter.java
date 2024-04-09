package com.ast_generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for storing and generating the method call report
 * for a given project
 */
public class MethodCallReporter {

    // Map of file name to list of method call entries
    private Map<String, List<MethodCallEntry>> reportMap = new HashMap<>();
    private Map<MethodSignatureKey, MethodCallEntry> uniqueMethodCalls = new HashMap<>();
    private String parentPackageName;
    private Map<DependencyNode, Set<String>> typeToJarReference;

    /**
     * Add a method call entry to the report map
     * 
     * @param fileName       The name of the file where the method call is made
     * @param declaringType  The type that declares the method
     * @param methodName     The name of the method
     * @param lineNumber     The line number where the method call is made
     * @param fullExpression The full expression of the method call
     * @param singature      The signature of the method
     * @param newPackageName The package name of the project
     * @see MethodCallEntry
     */
    public void addEntry(String fileName, String declaringType, String methodName, int lineNumber,
            String fullExpression, String singature, String newPackageName) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        MethodCallEntry entry = new MethodCallEntry(declaringType, methodName, lineNumber, fullExpression, singature);
        uniqueMethodCalls.putIfAbsent(new MethodSignatureKey(declaringType, singature), entry);

        reportMap.get(fileName).add(entry);
    }

    /**
     * Add a method call entry to the report map
     * 
     * @param fileName The name of the file where the method call is made
     * @param entry    The method call entry object
     * @see MethodCallEntry
     */
    public void addEntry(String fileName, MethodCallEntry entry) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        uniqueMethodCalls.putIfAbsent(new MethodSignatureKey(entry.getDeclaringType(), entry.getMethodSignature()),
                entry);
        // reportMap.get(fileName).add(uniqueMethodDeclarations.get(lookupKey));
        reportMap.get(fileName).add(entry);
    }

    /**
     * Add a list of method call entries to the report map
     * 
     * @param fileName The name of the file where the method call is made
     * @param entries  The list of method call entries
     * @see MethodCallEntry
     */
    public void addEntries(String fileName, List<MethodCallEntry> entries) {
        reportMap.putIfAbsent(fileName, new ArrayList<>());
        for (MethodCallEntry entry : entries) {
            this.addEntry(fileName, entry);
        }
    }

    /**
     * Set the package name of the project
     * 
     * @param parentPackageName
     */
    public void setParentPackageName(String parentPackageName) {
        this.parentPackageName = parentPackageName;
    }

    /**
     * Add a declaration info for a method in a type in the first layer of the
     * reporter
     * 
     * @param declaringType   The type that declares the method
     * @param declarationInfo The declaration info object of the method
     * @see MethodDeclarationInfo
     * @return True if the declaration info is added successfully, false otherwise
     */
    public boolean addDeclarationInfoForMethodinType(String declaringType, MethodDeclarationInfo declarationInfo) {
        String declarationSignature = declarationInfo.getDeclarationSignature();
        if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")) {
            return false;
        }

        Boolean ret = false;

        // checking if the method is already in the reportMap, we only analyze function
        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                // System.out.println("test type check in reporter: " +
                // entry.getDeclaringType());
                if (entry.getDeclaringType().equals(declaringType)) {

                    if (entry.getMethodSignature().equals(declarationSignature)) {
                        entry.setDeclarationInfo(declarationInfo);
                        ret = true;
                    }
                }
            }
        }
        return ret;
    }

    public void setTypeToJarReference(Map<DependencyNode, Set<String>> typeToJarReference) {
        this.typeToJarReference = typeToJarReference;
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

    /**
     * get the main project name
     * 
     * @return The main package name of the project
     */
    public String getParentPackageName() {
        return parentPackageName;
    }

    /**
     * Get a list of unique types from the report map in the First Layer (Main
     * project)
     * 
     * @return List of unique types
     */
    public List<String> getUniqueTypes() {
        Set<String> uniqueTypes = new HashSet<>();
        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                uniqueTypes.add(entry.getDeclaringType());
            }
        }
        return new ArrayList<>(uniqueTypes);
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

        // if (typeToJarReference == null) {
        Map<MethodSignatureKey, MethodCallEntry> uniqueMethodCalls = new HashMap<>();
        for (List<MethodCallEntry> entries : reportMap.values()) {
            for (MethodCallEntry entry : entries) {
                if (!(entry.getDeclaringType().startsWith("java.") ||
                        entry.getDeclaringType().startsWith("javax.") ||
                        entry.getDeclaringType().startsWith(parentPackageName))) {

                    MethodSignatureKey key = new MethodSignatureKey(entry.getDeclaringType(),
                            entry.getMethodSignature());
                    uniqueMethodCalls.putIfAbsent(key, entry);
                }
            }
        }

        System.out.println("uniqueMethodDeclarations: " + uniqueMethodCalls.size());
        // Now you have a map of unique method declarations, you can convert it to a
        // list or directly use it for JSON generation
        List<MethodCallEntry> allMethodDeclarationInfos = new ArrayList<>(uniqueMethodCalls.values());

        // Generate JSON
        if (typeToJarReference == null) {
            mapper.writeValue(new File(toFilePath), allMethodDeclarationInfos);
            System.out.println("no typeToJarReference, generate file based on declaring types");
        } else {

            Map<Dependency, List<MethodCallEntry>> dependencyToMethodCallEntries = new HashMap<>();
            for (MethodCallEntry entry : allMethodDeclarationInfos) {
                Dependency matchedDependency = null;
                for (Map.Entry<DependencyNode, Set<String>> entry1 : typeToJarReference.entrySet()) {

                    if (entry1.getValue().contains(entry.getDeclaringType())) {
                        matchedDependency = entry1.getKey();
                        // System.out.println("typetoJarReference.getValue(): " + entry1.getValue() + "
                        // allMethod.getDeclaringType(): " + entry.getDeclaringType());
                        break;
                    }
                }
                if (matchedDependency != null) {
                    if (dependencyToMethodCallEntries.get(matchedDependency) == null) {
                        dependencyToMethodCallEntries.put(matchedDependency, new ArrayList<>());
                    }
                    dependencyToMethodCallEntries.get(matchedDependency).add(entry);
                }
            }
            // Generate JSON
            System.out.println("Write to " + toFilePath);
            mapper.writeValue(new File(toFilePath), dependencyToMethodCallEntries);
            System.out.println("detected typeToJarReference, generate file based on dependencies");
        }
    }

}
