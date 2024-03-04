package com.ast_generator;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            processBuilder.command("mvn", "dependency:sources");

            // Start the process
            Process process = processBuilder.start();
            
            // Read the output and error streams
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
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
     * @param dependency - the dependency to decompress
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
}

