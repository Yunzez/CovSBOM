package com.ast_generator;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
}

