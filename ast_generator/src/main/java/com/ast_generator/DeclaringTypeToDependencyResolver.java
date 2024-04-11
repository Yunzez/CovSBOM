package com.ast_generator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeclaringTypeToDependencyResolver {

    private Map<DependencyNode, Set<String>> typeToJarLookup = new HashMap<>();
    private Set<String> unresolvedTypes = new HashSet<>();
    private List<DependencyNode> dependecies; // Map from artifactId to DependencyNode
    private List<String> jarDecompressedPaths; // Paths to decompressed JAR directories

    public DeclaringTypeToDependencyResolver(List<DependencyNode> dependecies,
            List<String> jarDecompressedPaths) {
        this.dependecies = dependecies;
        this.jarDecompressedPaths = jarDecompressedPaths;
    }

    /**
     * Attempts to find the jar path for a given declaring type.
     * 
     * @param declaringType The fully qualified name of the type.
     */
    // private void findJarPathForType(String declaringType) {
    // String filePath = declaringType.replace('.', '/') + ".java"; // Convert
    // package name to file path

    // for (String jarDecompressedPath : jarDecompressedPaths) {
    // Path potentialPath = Paths.get("decompressed/" + jarDecompressedPath,
    // filePath);
    // // System.out.println("Checking path: " + potentialPath.toString());

    // if (Files.exists(potentialPath)) {
    // DependencyNode matchedDependency =
    // findDependencyForDecompressedPath(jarDecompressedPath);
    // if (declaringType.contains("org.slf4j.helpers.Util") &&
    // potentialPath.toString().contains("org.slf4j")) {
    // System.out.println("Checking path: " + potentialPath.toString() + " exist ");
    // System.out.println(matchedDependency.toShortString());
    // }

    // if (matchedDependency != null) {
    // typeToJarLookup.computeIfAbsent(matchedDependency, k -> new
    // HashSet<>()).add(declaringType);
    // unresolvedTypes.remove(declaringType); // Mark as resolved
    // break; // Stop searching once matched
    // } else {
    // // Log or handle the case where the dependency couldn't be found
    // }
    // }
    // }
    // if (!typeToJarLookup.containsKey(declaringType)) {
    // // Log or handle the case where the declaring type's source file couldn't be
    // // found
    // unresolvedTypes.add(declaringType);
    // }
    // }

    private void findJarPathForType(String declaringType) {
        String[] parts = declaringType.split("\\.");
        boolean matchFound = false;

        while (!matchFound && parts.length > 2) {
            // Construct potential file path by joining parts with '/'
            String filePath = String.join("/", parts) + ".java";
            for (String jarDecompressedPath : jarDecompressedPaths) {
                Path potentialPath = Paths.get("decompressed/" + jarDecompressedPath, filePath);
                // System.out.println("Checking path: " + potentialPath.toString());

                if (Files.exists(potentialPath)) {
                    DependencyNode matchedDependency = findDependencyForDecompressedPath(jarDecompressedPath);
                    if (declaringType.contains("org.apache.http.protocol")
                            && potentialPath.toString().contains("org.apache")) {
                        System.out.println("Checking path: " + potentialPath.toString() + "  exist " );
                        // System.out.println(matchedDependency.toShortString());
                        System.out.println(jarDecompressedPath);
                        System.out.println("----");

                    }

                    if (matchedDependency != null) {
                        typeToJarLookup.computeIfAbsent(matchedDependency, k -> new HashSet<>()).add(declaringType);
                        unresolvedTypes.remove(declaringType); // Mark as resolved
                        matchFound = true;
                        break; // Stop searching once matched
                    }
                }
            }

            // Reduce parts array by removing the last element
            parts = Arrays.copyOf(parts, parts.length - 1);
        }

        if (!matchFound) {
            // Log or handle the case where the declaring type's source file couldn't be
            // found
            System.out.println("Failed to find a match for: " + declaringType);
            System.out.println( "Parts: " + Arrays.toString(parts));
            unresolvedTypes.add(declaringType);
        }
    }

    private DependencyNode findDependencyForDecompressedPath(String decompressedPath) {
        // Simplistic approach: Match based on the presence of artifactId in the
        // decompressedPath
        // Adjust logic as needed based on your decompression naming convention

        if (decompressedPath.contains("org.apache.httpcomponents.httpcore")) {
            System.out.println("decompressedPath: " + decompressedPath);
            // System.out.println(dependecies.values().toString());
            System.out.println(dependecies.size());
            for (DependencyNode dependency : dependecies) {
                System.out.println(dependency.getSourceDecompressedPath());
                if (dependency.getSourceDecompressedPath().contains(decompressedPath)) {
                    System.out.println("Matched: " + dependency.toShortString());
                }
            }
        }
        return dependecies.stream()
                .filter(dependency -> decompressedPath.contains(dependency.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    public DependencyNode getDependencyForDeclaringType(String declaringType) {
        // Call findJarPathForType first to ensure the mappings are updated
        findJarPathForType(declaringType);

        // Iterate through the typeToJarLookup to find the corresponding DependencyNode
        for (Map.Entry<DependencyNode, Set<String>> entry : typeToJarLookup.entrySet()) {
            if (entry.getValue().contains(declaringType)) {
                return entry.getKey(); // Return the DependencyNode that includes the declaringType
            }
        }
        return null; // Return null if no matching dependency is found
    }

    // Getter for unresolvedTypes to handle or log types that couldn't be resolved
    public Set<String> getUnresolvedTypes() {
        return unresolvedTypes;
    }
}
