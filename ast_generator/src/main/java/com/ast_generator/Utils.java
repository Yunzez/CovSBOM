package com.ast_generator;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.javaparser.ParserConfiguration;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Set;
import java.lang.Object;

public class Utils {

    // Private constructor to prevent instantiation
    private Utils() {
        throw new AssertionError("Utility class cannot be instantiated.");
    }

    public static List<Path> traverseFiles(Path rootDir, String fileExtension) {
        List<Path> fileList = new ArrayList<>();
        try {
            Files.walk(rootDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(fileExtension))
                    .forEach(fileList::add);
        } catch (IOException e) {
            System.err.println("Error traversing directory: " + rootDir);
            e.printStackTrace();
        }
        return fileList;
    }

    public static JsonObject readAst(String filePath) {
        try (InputStream is = new FileInputStream(filePath);
                JsonReader reader = Json.createReader(is)) {
            return reader.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
        } catch (Exception e) {
            System.err.println("Error reading AST from file: " + filePath);
            e.printStackTrace();
        }
        return null; // Consider alternative error handling based on your application's needs
    }

    public static void mavenInstallSources(String rootDirectoryPath) {
        // Convert the root directory path to an absolute path
        Path rootPath = Paths.get(rootDirectoryPath).toAbsolutePath();

        try {
            // Create a process builder to run the mvn command
            ProcessBuilder processBuilder = new ProcessBuilder();
            // Set the directory to run the command in
            processBuilder.directory(rootPath.toFile());
            // Set the command to run

            processBuilder.command("mvn", "dependency:resolve");
            Process processResolve = processBuilder.start();
            BufferedReader readerResolve = new BufferedReader(new InputStreamReader(processResolve.getInputStream()));
            String line;
            while ((line = readerResolve.readLine()) != null) {
                System.out.println(line);
            }
            int exitValResolve = processResolve.waitFor();
            if (exitValResolve == 0) {
                System.out.println("Maven dependency resolution completed successfully.");
            } else {
                System.out.println("Maven dependency resolution encountered an error.");
                return; // Exit if dependency resolution fails
            }

            processBuilder.command("mvn", "dependency:sources -fn");
            Process process = processBuilder.start();
            // Read the output and error streams
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to complete and check the exit value
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("Maven install sources completed successfully.");
            } else {
                System.out.println("Maven install sources encountered an error.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * decompress singale jar files
     * 
     * @param dependency - the dependency to decompress
     * 
     * @param decompressDir - the directory to write the decompressed files to
     */

    public static String decompressSingleJar(Dependency dependency, Path decompressDir) throws IOException {
        // Extract the JAR file name without the extension to use as the directory name
        Path jarPath = Paths.get(dependency.getSourceJarPath());
        String pathAfterRepository = dependency.getBasePackageName(); // jarFileName.split("/repository/")[1];

        // * create a path name for each jar decompressed directory
        String decompressSubDirName = pathAfterRepository.replace('/', '_');

        Path jarSpecificDecompressDir = decompressDir.resolve(decompressSubDirName);
        Path metaInfPath = Paths.get(jarSpecificDecompressDir.toString(), "META-INF");
        if (!Files.exists(jarSpecificDecompressDir)) {
            Files.createDirectories(jarSpecificDecompressDir);
        }
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarPath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = jarSpecificDecompressDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    Files.createDirectories(filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
        deleteMetaInfDirectory(metaInfPath);
        return decompressSubDirName;
    }

    /**
     * Decompresses all jar files and updates the {@code sourceDecompressedPath} for
     * each dependency after decompressing.
     *
     * @param collection the collection of dependencies to decompress
     * @param stringPath the directory to write the decompressed files to
     * @return A list of strings representing the paths to the decompressed
     *         subdirectories for each dependency
     */
    public static List<String> decompressAllJars(Collection<DependencyNode> collection, String stringPath) {
        List<String> decompressSubDirName = new ArrayList<String>();
        Path decompressedParentPath = Paths.get(stringPath);

        for (Dependency dependency : collection) {
            try {
                String decompressedPath = decompressSingleJar(dependency, decompressedParentPath);
                dependency.setSourceDecompressedPath(decompressedPath);
                decompressSubDirName.add(decompressedPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return decompressSubDirName;

    }

    public static void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent()); // Ensure directory exists
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public static void deleteMetaInfDirectory(Path metaInfPath) throws IOException {
        if (Files.exists(metaInfPath)) {
            // Use walk to find all files and directories under META-INF
            try (Stream<Path> walk = Files.walk(metaInfPath)) {
                // Sort in reverse order so directories are deleted after their contents
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete " + path + ": " + e.getMessage());
                            }
                        });
            }
        } else {
            System.out.println("META-INF directory does not exist or has already been deleted.");
        }
    }

    /**
     * Checks if the declaring type is part of the given dependency.
     *
     * @param declaringType The full package name of the type being checked.
     * @param dependency    The dependency object to compare against.
     * @return true if the declaring type is part of the dependency; false
     *         otherwise.
     */
    public static boolean isTypePartOfDependency(String declaringType, Dependency dependency) {
        // Convert groupId and declaringType to a base form for comparison
        String baseGroupId = getBasePackage(dependency.getGroupId());
        String baseGroupIdPart = getBasePackage(declaringType);

        // Perform the comparison using the base package name
        return baseGroupIdPart.equals(baseGroupId) || declaringType.contains(baseGroupId);
    }

    /**
     * Extracts the base package name from a longer package name or groupId.
     *
     * @param packageName The full package name or groupId.
     * @return The base package name.
     */
    private static String getBasePackage(String packageName) {
        Set<String> commonPrefixes = new HashSet<>(Arrays.asList("com", "org", "net", "io")); // Add more as needed
        String[] parts = packageName.split("\\.");

        // Determine starting index based on common prefixes
        int startIndex = (parts.length > 1 && commonPrefixes.contains(parts[0])) ? 1 : 0;

        // Construct the package name starting from the determined index
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (i > startIndex)
                sb.append("."); // Add dot separator between package segments
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    public static Dependency findDependencyWithMaxProbability(String unresolvedType,
            Collection<Dependency> dependencies) {
        String[] unresolvedParts = unresolvedType.split("\\.");
        Dependency bestMatch = null;
        int maxMatches = -1;

        for (Dependency dependency : dependencies) {
            // Combine groupId and artifactId for comparison and split them into parts
            String combinedDependencyIdentifier = dependency.getGroupId() + "." + dependency.getArtifactId();
            String[] dependencyParts = combinedDependencyIdentifier.split("\\.");

            int matches = countMatches(unresolvedParts, dependencyParts);

            if (matches > maxMatches) {
                maxMatches = matches;
                bestMatch = dependency;
            }
        }
        System.out.println("Best match for unresolved type " + unresolvedType + " is: " + bestMatch.getGroupId() + " "
                + bestMatch.getArtifactId() + " with " + maxMatches + " matches.");
        return bestMatch;
    }

    private static int countMatches(String[] unresolvedParts, String[] dependencyParts) {
        int matches = 0;
        for (int i = 0; i < unresolvedParts.length && i < dependencyParts.length; i++) {
            if (unresolvedParts[i].equals(dependencyParts[i])) {
                matches++;
            } else {
                // Stop counting at the first non-match
                break;
            }
        }
        return matches;
    }

    public static ParserConfiguration.LanguageLevel getLanguageLevelFromVersion(String version) {
        switch (version) {
            case "1.8":
                return ParserConfiguration.LanguageLevel.JAVA_8;
            case "9":
                return ParserConfiguration.LanguageLevel.JAVA_9;
            case "10":
                return ParserConfiguration.LanguageLevel.JAVA_10;
            case "11":
                return ParserConfiguration.LanguageLevel.JAVA_11;
            case "12":
                return ParserConfiguration.LanguageLevel.JAVA_12;
            case "13":
                return ParserConfiguration.LanguageLevel.JAVA_13;
            case "14":
                return ParserConfiguration.LanguageLevel.JAVA_14;
            case "15":
                return ParserConfiguration.LanguageLevel.JAVA_15;
            case "16":
                return ParserConfiguration.LanguageLevel.JAVA_16;
            case "17":
                return ParserConfiguration.LanguageLevel.JAVA_17;
            // Add more cases for newer Java versions as JavaParser supports them
            default:
                return ParserConfiguration.LanguageLevel.JAVA_8; // Default to JAVA_8 if not specified or recognized
        }
    }


    /**
     * Collects all dependencies of a given root node and returns them in a set.
     */
    public class DependencyCollector {

        public static Set<DependencyNode> collectAllDependencies(DependencyNode rootNode) {
            Set<DependencyNode> allDependencies = new HashSet<>();
            collectDependenciesRecursive(rootNode, allDependencies);
            return allDependencies;
        }
    
        private static void collectDependenciesRecursive(DependencyNode node, Set<DependencyNode> allDependencies) {
            if (node == null || allDependencies.contains(node)) {
                return; // Base case: node is null or already processed
            }
    
            // Process current node
            allDependencies.add(node);
    
            // Recursively process all children
            for (DependencyNode child : node.getChildren()) {
                collectDependenciesRecursive(child, allDependencies);
            }
        }
    }

}
