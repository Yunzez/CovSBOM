package com.ast_generator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
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
import java.util.Set;

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
        System.out.print("-------Initializing-------\n");
        astPath = Paths.get("asts/main");
        if (Files.exists(astPath)) {
            try {
                Files.delete(astPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Files.createDirectories(astPath.getParent());
        }

        System.out.print("Please enter the path to the Java source file: ");

        // String rootDirectoryPath = scanner.nextLine().trim();

        // Dennis: Only for testing
        System.out.println("rootDirectoryPath: " + System.getProperty("user.dir"));
        String rootDirectoryPath = "Application/spark-master";

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

        System.out.println("installing source code: " + rootDirectoryPath);
        Utils.mavenInstallSources(rootDirectoryPath);

        Map<String, Dependency> dependencyMap = DependencyProcessor.parsePomForDependencies(inferredPomPath);

        // print out dependecy map in a easy to read format
        System.out.println("---------------------------- dependency map ----------------------------");
        dependencyMap.forEach((k, v) -> System.out.println(k + " : " + v));
        System.out.println("---------------------------- dependency map ----------------------------");

        // ! generate ASTs for all java files in the application
        /*
         * +--------------------+
         * | Generation section |
         * +--------------------+
         */

        // * create an instance of import manager
        ImportManager importManager = new ImportManager(dependencyMap, astPath);
        MethodCallReporter methodCallReporter = new MethodCallReporter();
        // * create an instance of directory processor
        DirectoryProcessor processor = new DirectoryProcessor(rootDirectoryPath, astPath, dependencyMap, importManager,
                methodCallReporter);

        // * process the directory
        processor.processDirectory();

        // importManager.printImports();
        methodCallReporter.generateThirdPartyTypeJsonReport("asts/analysis/method_calls.json");
        System.out.println(" ------- end processing directory, start analyzing dependencies -------");

        /*
         * +------------------+
         * | Analysis section |
         * +------------------+
         */

        // // ! test
        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(dependencyMap, methodCallReporter);

        dependencyAnalyzer.analyze();

        methodCallReporter.generateThirdPartyTypeJsonReport("asts/analysis/final_report_file_based.json");

        methodCallReporter.generateThirdPartyTypeJsonReportBasedonPackage("asts/analysis/final_report_package_based.json");
        // // ! process dependencies
        // DependencyProcessor.processDependencies(inferredPomPath, importManager,
        // dependencyMap);

        scanner.close();
    }
}
