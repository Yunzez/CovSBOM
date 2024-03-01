package com.ast_generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.*;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ast.body.MethodDeclaration;

public class SourceJarAnalyzer {
    private Path jarPath;
    private Dependency dependency;
    private String decompiledPath;
    private Collection<String> targetPackages;
    private Path decompressDir;
    CombinedTypeSolver combinedSolver;
    MethodCallReporter methodCallReporter;

    private Map<String, Set<MethodDeclarationInfo>> singleJarAnalysis = new HashMap<String, Set<MethodDeclarationInfo>>();

    // ! analysis statcs
    int totalCount = 0;
    int successCount = 0;
    int failCount = 0;

    // Unified constructor
    public SourceJarAnalyzer(Dependency dependency, Collection<String> targetPackages, MethodCallReporter reporter,
            String decompressDir) {
        this.dependency = dependency;
        this.jarPath = Paths.get(dependency.getSourceJarPath());
        this.targetPackages = targetPackages; // Accepts any Collection<String>
        this.decompressDir = Paths.get(decompressDir);
        this.methodCallReporter = reporter;
    }

    public void analyze() throws IOException {
        // Decompress the JAR file
        decompressJar();

        // Process the decompressed directory
        processDecompressedDirectory();
        System.out.println("Total third party method calls: " + totalCount + ", Success: " + successCount + ", Fail: "
                + failCount);

    }

    public void deleteMetaInfDirectory(Path metaInfPath) throws IOException {
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

    private void decompressJar() throws IOException {
        // Extract the JAR file name without the extension to use as the directory name
        String jarFileName = jarPath.toString();
        System.out.println(jarFileName);
        String pathAfterRepository = dependency.getBasePackageName(); // jarFileName.split("/repository/")[1];
        System.out.println(pathAfterRepository);
        // Replace slashes with underscores (or another preferred character) to flatten
        // the directory structure

        // * create a path name for each jar decompressed directory
        String decompressSubDirName = pathAfterRepository.replace('/', '_');
        decompiledPath = decompressSubDirName;
        // Create a path for the new directory under decompressDir using the JAR file
        // name

        Path jarSpecificDecompressDir = decompressDir.resolve(decompressSubDirName);
        Path metaInfPath = Paths.get(jarSpecificDecompressDir.toString(), "META-INF");
        if (!Files.exists(jarSpecificDecompressDir)) {
            Files.createDirectories(jarSpecificDecompressDir);
        }
        System.out.println(jarSpecificDecompressDir.toString());
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarPath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = jarSpecificDecompressDir.resolve(entry.getName());
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

        deleteMetaInfDirectory(metaInfPath);
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

        // Walk through each directory directly under the decompressed directory
        String fullPath = decompressDir.toString() + "/" + decompiledPath;

        System.out.println("Source root: " + fullPath);
        // For parsing and symbol solving
        Path projectRoot = Paths.get(fullPath);
        ProjectRoot projectRootForSolving = new SymbolSolverCollectionStrategy().collect(projectRoot);

        initCombinedSolver(fullPath, projectRootForSolving);
        analyzeDecompressedJar(projectRootForSolving); // A new method to process Java files in
        // each subdirectory
    }

    private void analyzeDecompressedJar(ProjectRoot projectRoot) throws IOException {
        // Iterate over all SourceRoots within the ProjectRoot
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            // Attempt to parse all .java files found in this SourceRoot
            List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();

            // Process each ParseResult to get CompilationUnit
            for (ParseResult<CompilationUnit> parseResult : parseResults) {

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    // cu.getStorage().ifPresent(storage -> System.out.println("------ Checking Java
                    // file: " + storage.getFileName() + " --------"));
                    String currentPath = cu.getStorage().get().getPath().toString();
                    currentPath = currentPath.split(decompiledPath)[1].substring(1);

                    String packageLikePath = currentPath.replace(File.separator, ".") // Replace file separators with
                                                                                      // dots
                            .replaceAll(".java$", "");

                    if (packageLikePath.contains("junit")) {
                        System.out.println("Current packageLike Path : " + packageLikePath);
                        String cuPackage = cu.getPackageDeclaration().map(PackageDeclaration::getNameAsString)
                                .orElse("");
                        System.out.println("Current file package: " + cuPackage);
                        System.out.println(
                                "Target packages: " + targetPackages.toString() + " | Current package: " + cuPackage);
                    }
                    targetPackages.stream().forEach(targetPackage -> {
                        if (packageLikePath.startsWith(targetPackage)) {
                            System.out.println("Processing relevant file: " + cu.getStorage().get().getPath()
                                    + " | Package: " + targetPackage);
                            processCompilationUnit(cu);
                        }
                    });
                }
            }
        }
        System.out.println("------ Completed processing of project source roots with roots number: "
                + projectRoot.getSourceRoots().size() + "------");
    }

    private void processCompilationUnit(CompilationUnit cu) {
        String packageName = cu.getPackageDeclaration()
                .map(PackageDeclaration::getNameAsString)
                .orElse(""); // Default to an empty string if no package is declared

        String fullPath = cu.getStorage().get().getPath().toString();
        String currentPath = cu.getStorage().get().getPath().toString();
        currentPath = currentPath.split(decompiledPath)[1].substring(1);

        String packageLikePath = currentPath.replace(File.separator, ".") // Replace file separators with
                                                                          // dots
                .replaceAll(".java$", "");

        Set<MethodDeclarationInfo> methodDeclarationInfos = new HashSet<MethodDeclarationInfo>();
        singleJarAnalysis.put(packageName, methodDeclarationInfos);
        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);

        // System.out.println("Package: " + packageName);
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            // Initialize MethodDeclarationInfo for the current method declaration
            int startLine = methodDeclaration.getBegin().map(pos -> pos.line).orElse(-1);
            int endLine = methodDeclaration.getEnd().map(pos -> pos.line).orElse(-1);
            String name = methodDeclaration.getName().asString();
            // System.out.println(" MethodDeclaration: " + name + " start: " + startLine + "
            // end: " + endLine);
            MethodDeclarationInfo currentDeclarationInfo = new MethodDeclarationInfo(fullPath, startLine, endLine,
                    name);

            List<MethodCallExpr> methodCalls = methodDeclaration.findAll(MethodCallExpr.class);
            totalCount += methodCalls.size();

            for (MethodCallExpr methodCall : methodCalls) {
                try {
                    ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                    int lineNumber = methodCall.getBegin().map(pos -> pos.line).orElse(-1);
                    String fullExpression = methodCall.toString();

                    String functionCallType = resolvedMethod.declaringType().getQualifiedName();
                    if (functionCallType.startsWith("java.") || functionCallType.startsWith("javax.")) {
                        continue;
                    } else {
                        currentDeclarationInfo.addInnerMethodCall(new MethodCallEntry(
                                resolvedMethod.declaringType().getQualifiedName(),
                                methodCall.getNameAsString(),
                                lineNumber,
                                fullExpression,
                                null // Since this is an inner call, the last parameter might need rethinking
                        ));
                    }
                    // System.out.println(
                    // "Method call: " + methodCall.getName() + " in method: " +
                    // methodDeclaration.getName());
                    // System.out.println("Declaring type: " +
                    // resolvedMethod.declaringType().getQualifiedName());
                    successCount++; // Increment success counter
                } catch (Exception e) {
                    failCount++;
                    if (packageLikePath.contains("junit")) {
                        System.err.println("Failed to resolve method call: " + methodCall.getName() +
                                " in method: " + methodDeclaration.getName());
                    }
                    continue;
                }

            }
            // System.out.println("Adding method declaration info for method: " + name + "
            // in type: " + packageLikePath);
            methodCallReporter.addDeclarationInfoForMethodinType(packageLikePath,
                    currentDeclarationInfo);
        }
    }

    /*
     * Different java project might have different verison thus different syntax,
     * currentlly we will
     * pass whichever reports an error (but it's very likely a version issue)
     */

    private void initCombinedSolver(String fullPath, ProjectRoot projectRoot) {
        System.out.println("Init combined solver for root path " + fullPath + " with roots number "
                + projectRoot.getSourceRoots().size());

        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();

        combinedSolver.add(new ReflectionTypeSolver());
        // combinedSolver.add(new JavaParserTypeSolver(new File(fullPath)));
        projectRoot.getSourceRoots()
                .forEach(sourceRoot -> combinedSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot())));

        ParserConfiguration configuration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedSolver));
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);

        StaticJavaParser.setConfiguration(configuration);

    }

}
