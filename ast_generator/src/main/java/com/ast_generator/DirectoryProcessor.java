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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
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
    private static Map<String, DependencyNode> dependencyMap;
    private ImportManager importManager;
    private static boolean separateFiles;
    CombinedTypeSolver combinedSolver;
    MethodCallReporter methodReporter;
    Map<String, String> moduleList;

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

    /**
     * 
     * @param directoryPath  the path to the directory to process
     * @param astPath        the path to the directory to save the AST files
     * @param dependencyMap  the map of dependencies to resolve
     * @param importManager  the ImportManager to store the imports
     * @param methodReporter the MethodCallReporter to store the method calls
     * @param moduleList     the list of modules to process, this can be null
     */
    public DirectoryProcessor(String directoryPath, Path astPath, Map<String, DependencyNode> dependencyMap,
            ImportManager importManager, MethodCallReporter methodReporter, Map<String, String> moduleList) {
        this.directoryPath = directoryPath;
        DirectoryProcessor.astPath = astPath;
        DirectoryProcessor.dependencyMap = dependencyMap;
        this.importManager = importManager;
        this.methodReporter = methodReporter;
        this.moduleList = moduleList;
        initCombinedSolver();
    }

    private void initCombinedSolver() {
        this.combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver()); // Still useful for resolving standard Java types

        // Loop through each dependency and add it to the CombinedTypeSolver
        for (String dependencyPath : dependencyMap.keySet()) {
            DependencyNode dependency = dependencyMap.get(dependencyPath);
            Path jarPath = Paths.get(dependency.getJarPath());

            if (Files.exists(jarPath)) {
                try {
                    System.out.println("Adding dependency: " + jarPath);
                    // Add JarTypeSolver for each JAR file (external dependency)
                    combinedSolver.add(new JarTypeSolver(jarPath.toString()));
                } catch (Exception e) {
                    System.out.println("Failed to add dependency: " + dependencyPath);
                    e.printStackTrace();
                }
            } else {
                System.out.println("Jar file does not exist, skipping: " + jarPath);
                dependency.setIsValid(false);
            }
        }

        // Add the project's source code to the CombinedTypeSolver
        Path mainSrcPath = Paths.get(directoryPath, "src", "main", "java");
        if (Files.exists(mainSrcPath) && Files.isDirectory(mainSrcPath)) {
            try {
                combinedSolver.add(new JavaParserTypeSolver(mainSrcPath.toFile()));
                System.out.println("Added project source directory: " + mainSrcPath);
            } catch (Exception e) {
                System.out.println("Failed to add project source directory: " + mainSrcPath);
                e.printStackTrace();
            }
        } else {
            System.out.println("Main project does not have a 'main/src/java' directory. Skipping: " + mainSrcPath);
        }


        if (moduleList.size() > 0) {
            for (String module : moduleList.keySet()) {
                String pomPath = moduleList.get(module);
                // Assuming modulePath points to the pom.xml, we get the module's directory by
                // navigating up.
                String moduleDirPath = new File(pomPath).getParent();
                try {
                    String sourceDirPath = moduleDirPath + "/src/main/java";
                    File sourceDir = new File(sourceDirPath);
                    // Check if the source directory actually exists before adding it to the solver
                    if (sourceDir.exists() && sourceDir.isDirectory()) {
                        combinedSolver.add(new JavaParserTypeSolver(sourceDir));
                        System.out.println("Added module source directory: " + sourceDirPath);
                    } else {
                        System.out.println("Source directory does not exist or is not a directory: " + sourceDirPath);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to add module source directory: " + moduleDirPath);
                    e.printStackTrace();
                }
            }
        }  else {
            System.out.println("No modules to process, skipping");
        }

        // Configure the JavaSymbolSolver with the CombinedTypeSolver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);

        // configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);
        ParserConfiguration.LanguageLevel languageLevel = Utils.getLanguageLevelFromVersion(Settings.JAVA_VERSION);

        StaticJavaParser.getParserConfiguration().setLanguageLevel(languageLevel);
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
    }

    private void processDirectory(Path directory) throws IOException {
        final int[] count = {0}; // Declare count as final or effectively final
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    processSingleJavaFile(file.toString());
                    count[0]++; // Increment count
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Processed " + count[0] + " Java files.");
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
            // String astJson = convertASTObjecttoJson(cu, path);

            // ! save AST JSON to file
            // saveASTJsonToFile(path.toString(), astJson);

        } catch (IOException e) {
            System.out.println("Error parsing file: " + path);
            e.printStackTrace();
        } catch (ParseProblemException e) {
            System.out.print("Possible template file, Parse problem in file: " + sourceFilePath);
            // e.printStackTrace();
            // System.out.println(" Skipped");
        }

    }

    /*
     * this method analyze single AST object
     */
    private void analyzeSingleASTObject(CompilationUnit cu, Path path) {
        System.out.println("Analyzing AST object: " + path);
        if (importManager != null) {
            analyzeASTObjectImport(cu, path);
        } else {
            System.out.println("ImportManager is null");
        }
        analyzeASTObjectFunctionCall(cu, path);
    }

    /*
     * this method analyze an AST object import, and save the result to
     * ImportManager
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
     * this method analyze an AST object function call, and save the result to
     * MethodCallReport
     */
    private void analyzeASTObjectFunctionCall(CompilationUnit cu, Path path) {

        final String[] packageName = { "" }; // Use array to bypass final/effectively final requirement
        cu.getPackageDeclaration()
                .ifPresent(packageDeclaration -> packageName[0] = packageDeclaration.getName().asString());
        methodReporter.setParentPackageName(packageName[0]);
        // System.out.println("Package: " + packageName[0]);
        Map<MethodSignatureKey, MethodCallEntry> uniqueMethodCalls = new HashMap<>();
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            try {
                // methodCall.
                ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                if (resolvedMethod.getName().toString().equals("setTrustStorePassword")) {
                    System.out.println("setTrustStorePassword after resolved: " + methodCall.getName()
                            + resolvedMethod.getQualifiedName());
                }

                String currentSignature = resolvedMethod.getSignature().toString();
                int lineNumber = methodCall.getBegin().map(pos -> pos.line).orElse(-1);
                String fullExpression = methodCall.toString();
                String functionCallType = resolvedMethod.declaringType().getQualifiedName();
                
                MethodSignatureKey key = new MethodSignatureKey(functionCallType, currentSignature);
                if (!functionCallType.startsWith("java.") && !functionCallType.startsWith("javax.")) {
                    MethodCallEntry existingEntry = uniqueMethodCalls.get(key);
                    if (existingEntry == null) {
                        // Add new entry if it doesn't exist
                        MethodCallEntry newEntry = new MethodCallEntry(
                                functionCallType,
                                methodCall.getNameAsString(),
                                lineNumber,
                                fullExpression,
                                currentSignature,
                                null // Consider how to handle inner calls
                        );
                        uniqueMethodCalls.put(key, newEntry);
                    } else {
                        // Update existing entry with new line number
                        existingEntry.addLineNumber(lineNumber); // Ensure MethodCallEntry has a setter for lineNumber
                    }
                }
            } catch (Exception e) {
                if (methodCall.getName().toString().equals("setTrustStorePassword")) {
                    System.out.println("setTrustStorePassword: " + methodCall.getName());
                    // this.methodReporter.addEntry(path.toString(), "unknown_delcare_type",
                    // methodCall.getNameAsString());
                    System.err.println("Failed to resolve method call: " + methodCall.getName());
                    e.printStackTrace();
                }

            } catch (StackOverflowError e) {
                System.err.println("Stack overflow error caught. Consider reviewing recursive methods.");
                // Log the error or take corrective action here.
                // Be cautious about the JVM's state.
            }
        });

        methodReporter.addEntries(path.toString(), new ArrayList<>(uniqueMethodCalls.values()));
    }

    // private static String convertASTObjecttoJson(CompilationUnit cu, Path path)
    // throws IOException {
    // StringWriter stringWriter = new StringWriter();
    // try (JsonGenerator jsonGenerator = Json.createGenerator(stringWriter)) {
    // JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
    // serializer.serialize(cu, jsonGenerator);
    // }

    // String astJson = stringWriter.toString();
    // return astJson;
    // }

    // private static void saveASTJsonToFile(String sourceFilePath, String astJson)
    // throws IOException {

    // // if (separateFiles) {
    // // Replace .java extension with .json
    // Path dirPath = Path.of("CovSBOM_output/main");
    // if (!Files.exists(dirPath)) {
    // Files.createDirectories(dirPath);
    // }
    // Path filePath = dirPath.resolve(sourceFilePath.substring(0,
    // sourceFilePath.length() - 5) + ".json");
    // if (!Files.exists(filePath.getParent())) {
    // Files.createDirectories(filePath.getParent());
    // }

    // if (Files.exists(filePath)) {
    // Files.delete(filePath);
    // } else {
    // Files.createFile(filePath);
    // }

    // // Convert the new JSON object to string and write to the file
    // // System.out.println("AST: " + astJson.length());
    // Files.writeString(filePath, astJson.toString(), StandardOpenOption.CREATE,
    // StandardOpenOption.WRITE);
    // }
}
