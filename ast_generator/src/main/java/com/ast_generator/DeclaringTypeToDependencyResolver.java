package com.ast_generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.HashSet;

public class DeclaringTypeToDependencyResolver {

    private Map<DependencyNode, Set<String>> cache = new HashMap<>();
    private Set<String> unresolvedTypes = new HashSet<>();
    private List<DependencyNode> dependecies; // Map from artifactId to DependencyNode
    private List<String> jarDecompressedPaths; // Paths to decompressed JAR directories
    private Map<String, DependencyNode> declaringTypeToDependency;
    private Dependency packageInfo;

    public DeclaringTypeToDependencyResolver(List<DependencyNode> dependecies,
            List<String> jarDecompressedPaths) {
        this.dependecies = dependecies;
        this.jarDecompressedPaths = jarDecompressedPaths;
        this.declaringTypeToDependency = new HashMap<>();
    }

    public DeclaringTypeToDependencyResolver(List<DependencyNode> dependecies,
            List<String> jarDecompressedPaths, Dependency packageInfo) {
        this.dependecies = dependecies;
        this.jarDecompressedPaths = jarDecompressedPaths;
        this.declaringTypeToDependency = new HashMap<>();
        this.packageInfo = packageInfo;
    }

    /**
     * Attempts to find the jar path for a given declaring type.
     * 
     * @param declaringType The fully qualified name of the type.
     */

    private void findJarPathForType(String declaringType) {

        String[] parts = declaringType.split("\\.");
        boolean matchFound = false;
        Set<String> lastValidPath = new HashSet<>();

        while (!matchFound && parts.length > 2) {
            // Attempt to construct the directory path from parts without the last element
            String directoryPath = String.join("/", Arrays.copyOf(parts, parts.length - 1));

            for (String jarDecompressedPath : jarDecompressedPaths) {
                Path potentialDirectoryPath = Paths.get("decompressed", jarDecompressedPath, directoryPath);
                if (Files.exists(potentialDirectoryPath) && Files.isDirectory(potentialDirectoryPath)) {
                    // Construct potential file path by joining parts with '/'
                    String filePath = directoryPath + "/" + parts[parts.length - 1] + ".java";
                    Path potentialPath = potentialDirectoryPath.resolve(parts[parts.length - 1] + ".java");

                    if (Files.exists(potentialPath)) {
                        if (declaringType.equals("org.powermock.api.mockito.expectation.WithAnyArguments")) {
                            System.out.println("check WithAnyArguments type at " + potentialPath.toString());
                        }
                        DependencyNode matchedDependency = findDependencyForDecompressedPath(potentialPath.toString());

                        if (matchedDependency != null) {
                            cache.computeIfAbsent(matchedDependency, k -> new HashSet<>()).add(declaringType);
                            unresolvedTypes.remove(declaringType); // Mark as resolved
                            declaringTypeToDependency.put(declaringType, matchedDependency);
                            matchFound = true;
                            break; // Stop searching once matched
                        }
                    }
                }
                lastValidPath.add(potentialDirectoryPath.toString());
            }

            // Reduce parts array by removing the last element for the next iteration
            parts = Arrays.copyOf(parts, parts.length - 1);
        }

        if (!matchFound && lastValidPath.size() > 0) {
            System.out.println("Failed to find a direct match, start using java parser for complex matching for : "
                    + declaringType);
            // Attempt to find using JavaParser within the last valid directory path
            String foundPath = attemptToFindTypeWithJavaParser(declaringType);
            if (foundPath != null) {
                DependencyNode matchedDependency = findDependencyForDecompressedPath(foundPath);
                if (matchedDependency != null) {
                    cache.computeIfAbsent(matchedDependency, k -> new HashSet<>()).add(declaringType);
                    declaringTypeToDependency.put(declaringType, matchedDependency);
                    unresolvedTypes.remove(declaringType);
                    matchFound = true;
                }
                System.out.println("Found type using JavaParser: " + declaringType + " in " + foundPath);
            }
        }

        if (!matchFound) {
            unresolvedTypes.add(declaringType);
            System.out.println("Ultimately failed to find a match for: " + declaringType
                    + ", possibly an interface or abstract class");
        }
    }

    /**
     * Attempts to find the type using JavaParser within the last valid directory
     * path.
     * 
     * @param declaringType The fully qualified name of the type.
     * @param lastValidPath The last valid directory path to search in.
     * @return
     */
    private String attemptToFindTypeWithJavaParser(String declaringType) {
        // Split the declaring type to separate the directory path from the type name
        String[] parts = declaringType.split("\\.");
        if (parts.length <= 1) {
            return null; // Not enough information to determine a directory
        }
        String directoryPathPart = String.join("/", Arrays.copyOf(parts, parts.length - 1));

        // Check each decompressed path
        for (String baseDecompressedPath : jarDecompressedPaths) {
            Path directoryPath = Paths.get("decompressed", baseDecompressedPath, directoryPathPart);
            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                continue;
            }
            System.out.println("Attempting to find type in: " + directoryPath);

            // Process each Java file in the directory
            try (Stream<Path> files = Files.walk(directoryPath)) {
                Optional<Path> foundFile = files
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(javaFilePath -> {
                            try {
                                return doesFileContainType(javaFilePath, declaringType);
                            } catch (Exception e) {
                                System.err.println("Error parsing file: " + javaFilePath);
                                return false;
                            }
                        })
                        .findFirst();

                if (foundFile.isPresent()) {
                    return foundFile.get().toString();
                }
            } catch (IOException e) {
                System.err.println("Error accessing files in directory: " + directoryPath);
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param javaFilePath The path to the Java file to check.
     * @param typeName     The name of the type to check for.
     * @return True if the file contains the type, false otherwise.
     * @throws IOException
     */
    private boolean doesFileContainType(Path javaFilePath, String typeName) throws IOException {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(javaFilePath);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            List<TypeDeclaration> types = cu.findAll(TypeDeclaration.class); // Use Node for broader compatibility
            for (Node node : types) {
                if (node instanceof TypeDeclaration) {
                    TypeDeclaration<?> type = (TypeDeclaration<?>) node;
                    // System.out.println("Found type: " + type.getNameAsString() + " in file: " +
                    // javaFilePath);
                    if (type.getNameAsString().equals(typeName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Attempts to find a dependency node for a given decompressed path.
     * 
     * @param decompressedPath The path to the java file that is a potential match.
     * @return The dependency node if found, null otherwise.
     */
    private DependencyNode findDependencyForDecompressedPath(String decompressedPath) {
        String normalizedPath = decompressedPath.replace('/', '.').toLowerCase();
        // Attempt to find a matching dependency node
        return dependecies.stream()
                .filter(dependency -> {
                    String expectedPathFragment = ("." + dependency.getArtifactId() + ".")
                            .toLowerCase();
                    // Ensure the path fragment expected is indeed part of the normalized path
                    return normalizedPath.contains(expectedPathFragment);
                })
                .findFirst()
                .orElse(null);
    }

    public DependencyNode getDependencyForDeclaringType(String declaringType) {
       
        if (declaringType.contains(packageInfo.getGroupId())) {
            System.out.println("Type is part of the current package file: " + declaringType);
            return null;
        }
        if (unresolvedTypes.contains(declaringType)) {
            // System.out.println("Type is unresolved: " + declaringType + " , potentially a
            // java parser issue or missing dependency ");
            return null; // Return null if the type is still unresolved
        }

        // Call findJarPathForType first to ensure the mappings are updated
        if (!declaringTypeToDependency.containsKey(declaringType)) {
            findJarPathForType(declaringType);
        }

        // Iterate through the cache to find the corresponding DependencyNode

        return declaringTypeToDependency.get(declaringType); // Return null if no matching dependency is found
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
