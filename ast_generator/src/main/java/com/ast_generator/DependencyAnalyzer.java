package com.ast_generator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyAnalyzer {
    private Map<String, DependencyNode> dependencyMap;
    private MethodCallReporter methodCallReporter;
    private Set<String> unresolvedTypes = new HashSet<String>();
    private List<String> jarDecompressedPaths = new ArrayList<String>();
    // * Map of jarPath to a set of types that are found in the jar
    // * Key: jarPath, Value: Set of types
    private Map<DependencyNode, Set<String>> typeToJarLookup = new HashMap<DependencyNode, Set<String>>();

    public DependencyAnalyzer() {
    }

    public DependencyAnalyzer(Map<String, DependencyNode> dependencyMap, MethodCallReporter methodCallReporter) {
        this.dependencyMap = dependencyMap;
        this.methodCallReporter = methodCallReporter;
    }

    public void analyze() {

        // this steps will decompressed all the jars iin the dependencyMap
        // we will also update the dependency's attribute sourceDecompressedPath

        List<DependencyNode> dependencies = new ArrayList<DependencyNode>();
        for (DependencyNode dependency : dependencyMap.values()) {
            dependencies.addAll(dependency.toList());
        }

        this.jarDecompressedPaths = Utils.decompressAllJars(dependencies, "decompressed");
        System.out.println("jarDecompressedPaths: " + jarDecompressedPaths.toString());
        // * find all required jar and save the results in typeToJarLookup
        findRequiredJars();

        System.out.println("typeToJarLookup: ");
        for (DependencyNode dependency : typeToJarLookup.keySet()) {
            System.out.println(dependency.toString() + ": " + typeToJarLookup.get(dependency).toString());
        }
        System.out.println("total unique types: " + methodCallReporter.getUniqueTypes().size());
        System.out.println("unresolved types: " + unresolvedTypes.size());
        System.out.println(unresolvedTypes.toString());

        // * only analyze jars that are used in the program
        for (DependencyNode dependency : typeToJarLookup.keySet()) {
            // * we get all the parth we need to analyze for this jar
            Set<String> types = typeToJarLookup.get(dependency);
            SourceJarAnalyzer sourceJarAnalyzer = new SourceJarAnalyzer(dependency,
                    types, methodCallReporter, "decompressed");

            try {
                sourceJarAnalyzer.analyze();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        methodCallReporter.setTypeToJarReference(typeToJarLookup);
    }

    /**
     * Find all required jars and save the results in typeToJarLookup
     */
    private void findRequiredJars() {
        List<String> uniqueTypes = methodCallReporter.getUniqueTypes();

        for (String declaringType : uniqueTypes) {
            if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")
                    || declaringType.startsWith(methodCallReporter.getParentPackageName())) {
                continue;
            }
            
            unresolvedTypes.add(declaringType);
            findJarPathForType(declaringType);
        }

    }

    /**
     * Create a map of jarPath to a set of types that are found in the jar
     * Key: jarPath, Value: Set of types
     */
    private void findJarPathForType(String declaringType) {

        String filePath = declaringType.replace('.', '/') + ".java"; // Convert package name to file path

        for (String jarDecompressedPath : jarDecompressedPaths) {
            Path potentialPath = Paths.get("decompressed/" + jarDecompressedPath, filePath);
            if (Files.exists(potentialPath)) {
                // Assuming dependencyMap keys are artifactIds and Dependency objects have a
                // method getArtifactId()
                DependencyNode matchedDependency = findDependencyForDecompressedPath(jarDecompressedPath);
                if (matchedDependency != null) {
                    if (typeToJarLookup.get(matchedDependency) == null) {
                        typeToJarLookup.put(matchedDependency, new HashSet<String>());
                    }
                    typeToJarLookup.get(matchedDependency).add(declaringType);
                    unresolvedTypes.remove(declaringType); // Mark as resolved
                    break; // Stop searching once matched
                }
            } else {
                // System.out.println("File not found: " + potentialPath.toAbsolutePath());
            }
        }
    }

    private DependencyNode findDependencyForDecompressedPath(String decompressedPath) {
        // Implement logic to find the matching Dependency object based on
        // decompressedPath
        // This might involve naming conventions or additional metadata stored during
        // decompression
        // Example return statement (replace with actual logic)
        return dependencyMap.values().stream()
                .filter(dependency -> decompressedPath.contains(dependency.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

}
