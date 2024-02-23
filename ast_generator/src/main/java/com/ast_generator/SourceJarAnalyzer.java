package com.ast_generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class SourceJarAnalyzer {
    private Path jarPath;
    private Collection<String> targetPackages;
    private Path decompressDir;
    CombinedTypeSolver combinedSolver;

    // ! analysis statcs
    int totalCount = 0;
    int successCount = 0;
    int failCount = 0;

    // Unified constructor
    public SourceJarAnalyzer(String jarPath, Collection<String> targetPackages, String decompressDir) {
        this.jarPath = Paths.get(jarPath);
        this.targetPackages = targetPackages; // Accepts any Collection<String>
        this.decompressDir = Paths.get(decompressDir);
    }

    public void analyze() throws IOException {
        // Decompress the JAR file
        decompressJar();

        // Process the decompressed directory
        processDecompressedDirectory();
    }

    private void decompressJar() throws IOException {
        if (!Files.exists(decompressDir)) {
            Files.createDirectories(decompressDir);
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarPath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = decompressDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    // Extract file
                    extractFile(zipIn, filePath);
                } else {
                    // Make directory
                    Files.createDirectories(filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent()); // Ensure directory exists
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    /*
     * Process the decompressed directory to find Java files and analyze them
     */
    private void processDecompressedDirectory() throws IOException {
        // * Initialize the combined type solver
        initCombinedSolver();
        Files.walk(decompressDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::processJavaFile);

        // * Print the analysis statistics
        System.out.println("Total third party method calls: " + totalCount + ", Success: " + successCount + ", Fail: "
                + failCount);
    }

    /*
     * Different java project might have different verison thus different syntax,
     * currentlly we will
     * pass whichever reports an error (but it's very likely a version issue)
     */

    private void processJavaFile(Path javaFilePath) {

        // Implement logic to check if the Java file is within the target packages
        // and process it accordingly.
        System.out.println("Processing Java file: " + javaFilePath);

        try {
            // ! generate AST object
            CompilationUnit cu = StaticJavaParser.parse(javaFilePath);
            List<MethodCallExpr> methodCalls = cu.findAll(MethodCallExpr.class);
            totalCount += methodCalls.size(); // Total number of method calls

            for (MethodCallExpr methodCall : methodCalls) {

                try {
                    ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                    int lineNumber = methodCall.getBegin().map(pos -> pos.line).orElse(-1);
                    String fullExpression = methodCall.toString();
                    System.out.println("Method call: " + methodCall.getName());
                    System.out.println("Declaring type: " + resolvedMethod.declaringType().getQualifiedName());
                    successCount++; // Increment success counter
                } catch (Exception e) {
                    // Log the failure to resolve the method call but allow the process to continue
                    System.err.println("Failed to resolve method call: " + methodCall.getName() + " at line "
                            + methodCall.getBegin().map(pos -> pos.line).orElse(-1));
                    failCount++;
                }
            }
            ;

        } catch (IOException e) {
            System.out.println("Error parsing file: " + javaFilePath);
            e.printStackTrace();
        }
    }

    private void initCombinedSolver() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);

        // Apply the configuration
        StaticJavaParser.setConfiguration(configuration);

        this.combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver());

        // Assuming you also want to use the type solver with the JavaSymbolSolver for
        // resolution
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
        configuration.setSymbolResolver(symbolSolver);
    }

}
