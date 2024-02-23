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
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.serialization.JavaParserJsonSerializer;
import java.io.File;

/*
 * this process local directory and generate AST for each java file
 */
public class DirectoryProcessor {
    private String directoryPath;
    private static Path astPath;
    private static Map<String, Dependency> dependencyMap;
    private ImportManager importManager;
    private static boolean separateFiles;
    CombinedTypeSolver combinedSolver;
    MethodCallReporter methodReporter;

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

    public DirectoryProcessor(String directoryPath, Path astPath, Map<String, Dependency> dependencyMap,
            ImportManager importManager, MethodCallReporter methodReporter) {
        this.directoryPath = directoryPath;
        DirectoryProcessor.astPath = astPath;
        DirectoryProcessor.dependencyMap = dependencyMap;
        this.importManager = importManager;
        this.methodReporter = methodReporter;

        initCombinedSolver();
    }

    private void initCombinedSolver() {
        this.combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver());
        for (String dependencyPath : dependencyMap.keySet()) {
            Dependency dependency = dependencyMap.get(dependencyPath);
            try {
                // System.out.println("Adding dependency: " + dependency.getJarPath());
                String jarPathString = dependency.getJarPath();
                // Convert the String to a Path
                Path jarPath = Paths.get(jarPathString);
                combinedSolver.add(new JarTypeSolver(new File(jarPathString)));
            } catch (Exception e) {
                // System.out.println("Failed to add dependency: " + dependencyPath);
                e.printStackTrace();
            }
        }

        System.out.println("combinedSolver: " + combinedSolver.getRoot().toString());
        // Initialize JavaParserTypeSolver with the source directory, not a specific
        // Java file
        combinedSolver.add(new JavaParserTypeSolver(new File(directoryPath)));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
        // Update to use the current method as per JavaParser's version if
        // getConfiguration() is deprecated
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

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

          // output method call report
          outputAnalysisReport();
    }

    private void outputAnalysisReport() {
        // output method call report
        try {
            methodReporter.generateThirdPartyTypeJsonReport("asts/analysis/method_calls.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    processSingleJavaFile(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

    }

    private void processSingleJavaFile(String sourceFilePath) {

        // Configure parser
        // ParserConfiguration config = new ParserConfiguration();
        // StaticJavaParser.setConfiguration(config);
        Path path = Paths.get(sourceFilePath);

        try {
            // ! generate AST object
            CompilationUnit cu = StaticJavaParser.parse(path);

            if (dependencyMap != null) {
                analyzeSingleASTObject(cu, path);
            }

            // ! convert AST object to JSON
            String astJson = convertASTObjecttoJson(cu, path);

            // ! save AST JSON to file
            saveASTJsonToFile(path.toString(), astJson);

        } catch (IOException e) {
            System.out.println("Error parsing file: " + path);
            e.printStackTrace();
        } catch (ParseProblemException e) {
            System.out.print("Parse problem in file: " + sourceFilePath);
            e.printStackTrace();
            // System.out.println(" Skipped");
        }
    }

    /*  
     * this method analyze single AST object
     */
    private void analyzeSingleASTObject(CompilationUnit cu, Path path) {
        if (importManager != null) {
            analyzeASTObjectImport(cu, path);
        } else {
            System.out.println("ImportManager is null");
        }

        analyzeASTObjectFunctionCall(cu, path);
    }

    /*
     * this method analyze an AST object import, and save the result to ImportManager
     */
    private void analyzeASTObjectImport(CompilationUnit cu, Path path) {
        FunctionSignatureExtractor extractor = new FunctionSignatureExtractor(
                dependencyMap != null ? dependencyMap : null);
        extractor.extractThirdPartyImports(cu);
        Set<String> thirdPartyImports = extractor.getThirdPartyImports();

        // Store the imports in ImportManager

        importManager.addImports(thirdPartyImports);
    }

    /*
     * this method analyze an AST object function call, and save the result to MethodCallReport
     */
    private void analyzeASTObjectFunctionCall(CompilationUnit cu, Path path) {
        final String[] packageName = {""}; // Use array to bypass final/effectively final requirement
        cu.getPackageDeclaration().ifPresent(packageDeclaration -> packageName[0] = packageDeclaration.getName().asString());
        // System.out.println("Package: " + packageName[0]);

        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            try {
                ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                // System.out.println("Method call: " + methodCall.getName());
                // System.out.println("Declaring type: " + resolvedMethod.declaringType().getQualifiedName());
                this.methodReporter.addEntry(path.toString(), resolvedMethod.declaringType().getQualifiedName(),
                        methodCall.getNameAsString(), packageName[0]);
            } catch (Exception e) {
                // this.methodReporter.addEntry(path.toString(), "unknown_delcare_type",
                // methodCall.getNameAsString());
                System.err.println("Failed to resolve method call: " + methodCall.getName());
            }
        });
    }

    private static String convertASTObjecttoJson(CompilationUnit cu, Path path) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (JsonGenerator jsonGenerator = Json.createGenerator(stringWriter)) {
            JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
            serializer.serialize(cu, jsonGenerator);
        }

        String astJson = stringWriter.toString();
        return astJson;
    }

    private static void saveASTJsonToFile(String sourceFilePath, String astJson)
            throws IOException {

        // if (separateFiles) {
        // Replace .java extension with .json
        Path dirPath = Path.of("asts/main");
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        Path filePath = dirPath.resolve(sourceFilePath.substring(0, sourceFilePath.length() - 5) + ".json");
        if (!Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }

        if (Files.exists(filePath)) {
            Files.delete(filePath);
        } else {
            Files.createFile(filePath);
        }

        // Convert the new JSON object to string and write to the file
        System.out.println("Writing to file: " + filePath);
        // System.out.println("AST: " + astJson.length());
        Files.writeString(filePath, astJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
}
