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

    private Map<DependencyNode, Set<String>> cache = new HashMap<>();
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
                    
                    if (matchedDependency != null) {
                        cache.computeIfAbsent(matchedDependency, k -> new HashSet<>()).add(declaringType);
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

        return dependecies.stream()
                .filter(dependency -> decompressedPath.contains(dependency.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    public DependencyNode getDependencyForDeclaringType(String declaringType) {
        // Call findJarPathForType first to ensure the mappings are updated
      
        findJarPathForType(declaringType);

        // Iterate through the cache to find the corresponding DependencyNode
        for (Map.Entry<DependencyNode, Set<String>> entry : cache.entrySet()) {
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

    // Getter for cache to access the resolved mappings
    public Map<DependencyNode, Set<String>> getCache() {
        return cache;
    }
}
