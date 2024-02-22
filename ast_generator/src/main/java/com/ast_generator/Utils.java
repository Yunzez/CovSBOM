package com.ast_generator;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
}

