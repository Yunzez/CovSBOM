package com.ast_generator;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.serialization.JavaParserJsonSerializer;
import java.io.File;

/*
 * this process local directory and generate AST for each java file
 */
public class DirectoryProcessor {
    private String directoryPath;
    private static Path astPath;
    private static Map<String, Dependency> dependencyMap;
    private static ImportManager importManager;
    private static boolean separateFiles;

    public DirectoryProcessor() {
    }

    public DirectoryProcessor(String directoryPath, Path astPath) {
        this.directoryPath = directoryPath;
        DirectoryProcessor.astPath = astPath;
    }

    public DirectoryProcessor(String directoryPath, Path astPath, Boolean separateFiles) {
        this.directoryPath = directoryPath;
        DirectoryProcessor.astPath = astPath;
        this.separateFiles = separateFiles;
    }

    public DirectoryProcessor(String directoryPath, Path astPath, Map<String, Dependency> dependencyMap) {
        this.directoryPath = directoryPath;
        DirectoryProcessor.astPath = astPath;
        DirectoryProcessor.dependencyMap = dependencyMap;
    }

    // Main method for command-line execution

    public void processDirectory() {
        System.out.println("Processing directory: " + directoryPath);
        // Validate and process the directory
        Path rootPath = Paths.get(directoryPath);
        if (Files.isDirectory(rootPath)) {
            try {
                processDirectory(rootPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid directory path.");
        }
    }

    public void addImportMaganer(ImportManager manager) {
        DirectoryProcessor.importManager = manager;
    }

    private static void processDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    generateAST(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void generateAST(String sourceFilePath) {
        // Configure parser
        // System.out.println("Configuring parser for file: " + sourceFilePath);
        ParserConfiguration config = new ParserConfiguration();
        StaticJavaParser.setConfiguration(config);
        Path path = Paths.get(sourceFilePath);

        try {
            CompilationUnit cu = StaticJavaParser.parse(path);

            // ! dependency can be null when we are processing directory without pom.xml or
            // ! calling this class by itself
            if (dependencyMap != null) {
                FunctionSignatureExtractor extractor = new FunctionSignatureExtractor(
                        dependencyMap != null ? dependencyMap : null);
                extractor.extractThirdPartyImports(cu);
                Set<String> thirdPartyImports = extractor.getThirdPartyImports();

                // Store the imports in ImportManager
                if (importManager != null) {
                    importManager.addImports(thirdPartyImports);
                } else {
                    System.out.println("ImportManager is null");
                    System.out.println("---------------------------- third party imports ----------------------------");
                    for (String signature : thirdPartyImports) {
                        System.out.println(signature);
                    }
                }

            }

            StringWriter stringWriter = new StringWriter();
            try (JsonGenerator jsonGenerator = Json.createGenerator(stringWriter)) {
                JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
                serializer.serialize(cu, jsonGenerator);
            }

            String astJson = stringWriter.toString();
            appendLocalASTToJsonFile(path.toString(), astJson);
        } catch (IOException e) {
            System.out.println("Error parsing file: " + path);
            e.printStackTrace();
        } catch (ParseProblemException e) {
            System.out.print("Parse problem in file: " + sourceFilePath);
            e.printStackTrace();
            // System.out.println(" Skipped");
        }
    }

    private static void appendLocalASTToJsonFile(String sourceFilePath, String astJson)
            throws IOException {

        if (separateFiles) {
            // Replace .java extension with .json
            String jsonFileName = new File(sourceFilePath).getName().replace(".java", ".json");
            Path individualAstPath = Paths.get(astPath.toString(), jsonFileName);
            try (JsonWriter jsonWriter = Json
                    .createWriter(Files.newBufferedWriter(individualAstPath, StandardOpenOption.CREATE))) {
                JsonObject newAst = Json.createReader(new StringReader(astJson)).readObject();
                jsonWriter.writeObject(newAst);
            }
        } else {

            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
            System.out.println("Appending AST to JSON file: " + astPath);
            if (!Files.exists(astPath)) {
                Files.createFile(astPath);
            }
            // Parse existing JSON and add new AST
            String existingContent = Files.readString(astPath);
            if (existingContent.trim().isEmpty()) {
                // File is empty, start with an empty JSON object
                jsonBuilder = Json.createObjectBuilder();
            } else {
                // File has content, parse it as JSON
                JsonObject existingJson = Json.createReader(new StringReader(existingContent)).readObject();
                jsonBuilder = Json.createObjectBuilder(existingJson);
            }

            // Add new AST
            JsonObject newAst = Json.createReader(new StringReader(astJson)).readObject();
            jsonBuilder.add(sourceFilePath, newAst);

            // Write back to file
            try (JsonWriter jsonWriter = Json.createWriter(
                    Files.newBufferedWriter(astPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
                jsonWriter.writeObject(jsonBuilder.build());
            }
        }
    }
}
