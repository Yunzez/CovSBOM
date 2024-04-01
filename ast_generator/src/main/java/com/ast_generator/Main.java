package com.ast_generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class Main {
    private List<String> dependenciesTree;
    private static Map<String, String> dependencyMap;
    private static Path astPath;

    private static String rootDirectoryPath;
    private static String inferredPomPath;
    private static String outputProjectFolderPath = "";
    private static String outputFolderName = "CovSBOM_output";

    private static boolean test = true;
    public static void main(String[] args) throws IOException {

        // ! Check for --process-directory argument
        if (args.length > 0 && "--process-directory".equals(args[0])) {
            // Assuming args[1] is sourcePath, args[2] is outputPath, and args[3] is
            // --separate
            if (args.length >= 2) {
                System.out.println("Processing directory: " + args[1]);
                rootDirectoryPath = Paths.get(args[1]).toString();
                Path rootPath = Paths.get(rootDirectoryPath);
                // Infer the path to pom.xml
                inferredPomPath = rootPath.resolve("pom.xml").toString();
                System.out.println("Inferred path to pom.xml: " + inferredPomPath);

                // Path outputPath = Paths.get(args[2]);
                // boolean separateFiles = (args.length == 4 && "--separate".equals(args[3]));
                // DirectoryProcessor processor = new DirectoryProcessor(sourcePath, outputPath,
                // separateFiles);
                // processor.processDirectory();
            } else {
                System.out.println(
                        "Usage: java Main --process-directory <source directory>");
            }
            // return; // Exit after processing directory
        } else {
            Scanner scanner = new Scanner(System.in);

            // Delete existing ast.json file if it exists
            System.out.print("-------Initializing-------\n");
            astPath = Paths.get("CovSBOM_output/main");
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

            rootDirectoryPath = scanner.nextLine().trim();

            // Dennis: Only for testing
            System.out.println("rootDirectoryPath: " + System.getProperty("user.dir"));

            if (rootDirectoryPath.isEmpty()) {
                rootDirectoryPath = "Application/spark-master";
            }

            // Validate and process the directory
            Path rootPath = Paths.get(rootDirectoryPath);
            // Infer the path to pom.xml
            inferredPomPath = rootPath.resolve("pom.xml").toString();
            System.out.println("Inferred path to pom.xml: " + inferredPomPath);

            // Check if the inferred path is correct
            System.out.print("Is this path correct? (yes/no): ");
            String response = scanner.nextLine();
            if ("no".equalsIgnoreCase(response)) {
                System.out.print("Please enter the correct path to the pom.xml: ");
                inferredPomPath = scanner.nextLine();
            }

            scanner.close();
        }

        outputProjectFolderPath = rootDirectoryPath.split("/")[rootDirectoryPath.split("/").length - 1];

        System.out.println("installing source code: " + rootDirectoryPath);
        Utils.mavenInstallSources(rootDirectoryPath);

        // ! loading settings:
        Properties settings = new Properties();
        try (InputStream input = new FileInputStream("settings.properties")) {
            settings.load(input);
            int recursiveLayers = Integer.parseInt(settings.getProperty("recursiveLayers", "2")); // Default to 2 if not
            // set
            boolean restrictRecursion = Boolean.parseBoolean(settings.getProperty("restrictRecursion", "true"));
            Settings.setRestrictDepth(restrictRecursion);
            Settings.setMaxMethodCallDepth(recursiveLayers);
            boolean ignoreTestAttributes = Boolean.parseBoolean(settings.getProperty("ignoreTestAttributes", "false"));
            Settings.setIgnoreTest(ignoreTestAttributes);
            System.out.println("recursiveLayers: " + recursiveLayers);
            // System.out.println("ignoreTestAttributes: " + ignoreTestAttributes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Map<String, Dependency> dependencyMap = new HashMap<String,Dependency>();
        Map<String, String> moduleList = DependencyProcessor.parsePomForModules(inferredPomPath);
        System.out.println("java version: " + DependencyProcessor.getJavaVersion());
        Settings.setJavaVersion(DependencyProcessor.getJavaVersion());
       

        System.out.println("moduleList: " + moduleList.toString());
        
        List<String> mavenTree = MavenDependencyTree.runMavenDependencyTree(rootDirectoryPath);
        Dependency packageInfo = DependencyProcessor.getPackageInfo();
        MavenDependencyTree.updateDependencyMapWithTreeOutput(mavenTree, dependencyMap, packageInfo);

        // if(test) {
        //     return;
        // }

        System.out.println("total dependencies: " + dependencyMap.size());
        for(String key: dependencyMap.keySet()){
            System.out.println("key: " + key + " value: " + dependencyMap.get(key).toString());
        }
        
      
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
                methodCallReporter, moduleList);

        // * process the directory
        processor.processDirectory();

        // importManager.printImports();
        methodCallReporter.generateThirdPartyTypeJsonReport(
            "CovSBOM_output/analysis/" + outputProjectFolderPath
                    + "/method_calls.json");
        System.out.println(" ------- end processing directory, start analyzing dependencies -------");

        /*
         * +------------------+
         * | Analysis section |
         * +------------------+
         */

        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(dependencyMap,
                methodCallReporter);

        dependencyAnalyzer.analyze();

        methodCallReporter.generateThirdPartyTypeJsonReport(
                "CovSBOM_output/analysis/" + outputProjectFolderPath
                        + "/final_report_file_based.json");

        methodCallReporter
                .generateThirdPartyTypeJsonReportBasedonPackage(
                    "CovSBOM_output/analysis/" + outputProjectFolderPath
                    + "/final_report_package_based.json");

        System.out.println("End of analysis");
    }
}
