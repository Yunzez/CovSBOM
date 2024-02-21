package com.ast_generator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class Main {
    private static Map<String, String> dependencyMap;
    private static Path astPath;
    private static Map<String, String> libraryAstJsonMap;

    public static void main(String[] args) throws IOException {

        // ! Check for --process-directory argument
        if (args.length > 0 && "--process-directory".equals(args[0])) {
            // Assuming args[1] is sourcePath, args[2] is outputPath, and args[3] is
            // --separate
            if (args.length >= 3) {
                String sourcePath = Paths.get(args[1]).toString();
                Path outputPath = Paths.get(args[2]);
                boolean separateFiles = (args.length == 4 && "--separate".equals(args[3]));
                DirectoryProcessor processor = new DirectoryProcessor(sourcePath, outputPath, separateFiles);
                processor.processDirectory();
            } else {
                System.out.println(
                        "Usage: java Main --process-directory <source directory> <AST output path> [--separate]");
            }
            return; // Exit after processing directory
        }

        Scanner scanner = new Scanner(System.in);
        libraryAstJsonMap = new HashMap<>();
        // Delete existing ast.json file if it exists
        System.out.print("initializing");
        astPath = Paths.get("asts/main/ast.json");
        if (Files.exists(astPath)) {
            try {
                Files.delete(astPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Files.createDirectories(astPath.getParent());
        }

        System.out.print("Please enter the path to the Java source file (hit enter for default): ");
        String rootDirectoryPath = scanner.nextLine().trim();

        if (rootDirectoryPath.isEmpty()) {
            // ! default source file path:
            rootDirectoryPath = "java/Encryption-test";
        }

        // Validate and process the directory
        Path rootPath = Paths.get(rootDirectoryPath);

        // Infer the path to pom.xml
        String inferredPomPath = rootPath.resolve("pom.xml").toString();
        System.out.println("Inferred path to pom.xml: " + inferredPomPath);

        // Check if the inferred path is correct
        System.out.print("Is this path correct? (yes/no): ");
        String response = scanner.nextLine();
        if ("no".equalsIgnoreCase(response)) {
            System.out.print("Please enter the correct path to the pom.xml: ");
            inferredPomPath = scanner.nextLine();
        }

        Map<String, Dependency> dependencyMap = DependencyProcessor.parsePomForDependencies(inferredPomPath);

        // * import manager manage imports line to share imports between files
        ImportManager importManager = new ImportManager();

        // print out dependecy map in a easy to read format
        System.out.println("---------------------------- dependency map ----------------------------");
        dependencyMap.forEach((k, v) -> System.out.println(k + " : " + v));
        System.out.println("---------------------------- dependency map ----------------------------");

        // ! process directory (local java file)
        DirectoryProcessor processor = new DirectoryProcessor(rootDirectoryPath, astPath, dependencyMap);

        // ! add import manager to processor before processing directory
        processor.addImportMaganer(importManager);

        // ! turn this on to process directory
        processor.processDirectory();

        importManager.printImports();
        // // ! process dependencies
        // DependencyProcessor.processDependencies(inferredPomPath, importManager);

        scanner.close();
    }

    private static void appendAllASTsToJsonFile() throws IOException {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        if (Files.exists(astPath)) {
            String existingContent = Files.readString(astPath);
            JsonObject existingJson = Json.createReader(new StringReader(existingContent)).readObject();
            jsonBuilder = Json.createObjectBuilder(existingJson);
        }

        libraryAstJsonMap.forEach(jsonBuilder::add);

        try (JsonWriter jsonWriter = Json.createWriter(
                Files.newBufferedWriter(astPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            jsonWriter.writeObject(jsonBuilder.build());
        }
    }

}
