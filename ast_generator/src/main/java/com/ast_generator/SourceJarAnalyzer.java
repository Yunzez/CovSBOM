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
import com.github.javaparser.resolution.UnsolvedSymbolException;
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
import com.github.javaparser.ast.body.CallableDeclaration.Signature;

public class SourceJarAnalyzer {
    private Path jarPath;
    private Dependency dependency;
    private String decompressedPath;
    private Collection<String> targetPackages;
    private List<ParseResult<CompilationUnit>> currentParseResults;
    // * this is the packages we will be tracking again when we loop thru internal
    // methods
    private Path decompressDir;
    CombinedTypeSolver combinedSolver;
    MethodCallReporter methodCallReporter;

    // ! max recursion depth for digging into method calls
    final int MAX_DIG_DEPTH = Settings.MAX_METHOD_CALL_DEPTH;

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
        // decompressJar();
        decompressedPath = this.dependency.getSourceDecompressedPath();
        System.out.println("Decompressed path: " + decompressedPath);
        // Process the decompressed directory
        processDecompressedDirectory();
        System.out.println("Total third party method calls: " + totalCount + ", Success: " + successCount + ", Fail: "
                + failCount);

    }

    /*
     * Process the decompressed directory to find Java files and analyze them
     */
    private void processDecompressedDirectory() throws IOException {
        // * Initialize the combined type solver

        // Walk through each directory directly under the decompressed directory
        String fullPath = decompressDir.toString() + "/" + decompressedPath;

        Path projectRoot = Paths.get(fullPath);
        ProjectRoot projectRootForSolving = new SymbolSolverCollectionStrategy().collect(projectRoot);

        initCombinedSolver(fullPath, projectRootForSolving);

        analyzeDecompressedJar(projectRootForSolving);
    }

    private void analyzeDecompressedJar(ProjectRoot projectRoot) throws IOException {
        // Iterate over all SourceRoots within the ProjectRoot
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            // Attempt to parse all .java files found in this SourceRoot
            List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
            iterateParseResults(parseResults);
            // Process each ParseResult to get CompilationUnit
        }
        // System.out.println("------ Completed processing of project source roots with
        // roots number: "
        // + projectRoot.getSourceRoots().size() + "------");
    }

    private void iterateParseResults(List<ParseResult<CompilationUnit>> parseResults) {
        this.currentParseResults = parseResults; // ! remember this current list of results
        for (ParseResult<CompilationUnit> parseResult : parseResults) {

            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    // cu.getStorage().ifPresent(storage -> System.out.println("------ Checking Java
                    // file: " + storage.getFileName() + " --------"));
                    String currentPath = cu.getStorage().get().getPath().toString();
                    currentPath = currentPath.split(decompressedPath)[1].substring(1);

                    String packageLikePath = currentPath.replace(File.separator, ".") // Replace file separators with
                                                                                      // dots
                            .replaceAll(".java$", "");

                    targetPackages.stream().forEach(targetPackage -> {
                        if (packageLikePath.startsWith(targetPackage)) {
                            processCompilationUnit(cu);
                        }
                    });
                }
            }
        }
    }

    /*
     * Process the first level of method calls
     */
    private void processCompilationUnit(CompilationUnit cu) {
        String fullPath = cu.getStorage().get().getPath().toString();
        String packageLikePath = getPackageLikePathFromCU(cu);

        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);

        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            // Initialize MethodDeclarationInfo for the current method declaration
            try {
                // * we have to resolve the method declaration to get the Qualified signature,
                // * qualified signature can be compared with method call qualified signature,
                // * providing higher accuracy
                ResolvedMethodDeclaration resolvedDeclaration = methodDeclaration.resolve();
                String currentDeclarationSignature = resolvedDeclaration.getSignature().toString();

                int startLine = methodDeclaration.getBegin().map(pos -> pos.line).orElse(-1);
                int endLine = methodDeclaration.getEnd().map(pos -> pos.line).orElse(-1);
                String name = methodDeclaration.getName().asString();

                MethodDeclarationInfo currentDeclarationInfo = new MethodDeclarationInfo(fullPath, startLine, endLine,
                        name, currentDeclarationSignature);

                // * extract method calls from the method declaration
                List<MethodCallEntry> currentCallEntries = extractMethodCallsFromDeclaration(methodDeclaration);

                // * add the method calls to the currentDeclarationInfo
                currentDeclarationInfo.addInnerMethodCalls(currentCallEntries);

                Boolean pass = methodCallReporter.addDeclarationInfoForMethodinType(packageLikePath,
                        currentDeclarationInfo);

                if (pass) {
                    // * start digging
                    digFunctionCallEntries(currentDeclarationInfo, 0);
                }
            } catch (UnsolvedSymbolException e) {
                // System.out.println(
                // "Warning: Could not resolve method declaration: " +
                // methodDeclaration.getNameAsString());
            }
        }
    }

    /*
     * Extract method calls from the method declaration
     */
    private List<MethodCallEntry> extractMethodCallsFromDeclaration(MethodDeclaration methodDeclaration) {
        Map<MethodSignatureKey, MethodCallEntry> uniqueMethodCalls = new HashMap<>();
        List<MethodCallExpr> methodCalls = methodDeclaration.findAll(MethodCallExpr.class);
        totalCount += methodCalls.size();

        for (MethodCallExpr methodCall : methodCalls) {
            try {
                ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                int lineNumber = methodCall.getBegin().map(pos -> pos.line).orElse(-1);
                String currentSignature = resolvedMethod.getSignature();
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
                        successCount++;
                    } else {
                        // Update existing entry with new line number
                        existingEntry.addLineNumber(lineNumber); // Ensure MethodCallEntry has a setter for lineNumber
                    }
                }
            } catch (UnsolvedSymbolException e) {
                // Log unresolved method calls; these might be external methods which are acceptable
                System.out.println("Info: Unresolved method call to '" + e.getName() + "' might be external and is therefore acceptable.");
            }
            catch (UnsupportedOperationException e) {
                // Log the issue but do not treat as critical error
                System.out.println("Warning: UnsupportedOperationException encountered. Method may not be supported for resolution: " + e.getMessage());
            }
            catch (IllegalStateException e) {
                System.err.println("Warning: Failed to resolve a type due to an IllegalStateException. " +
                        "This may indicate a complex type usage not fully supported. " +
                        "Details: " + e.getMessage());
                failCount++;
            }
            catch (RuntimeException e) {
                if (e.getMessage().contains("cannot be resolved")) {
                    // Log but do not increment failCount for unresolved external methods
                    System.out.println("Info: The method '" + e.getMessage().split("'")[1] + "' cannot be resolved, possibly due to being an external dependency.");
                } else {
                    // For other RuntimeExceptions, log as error and increment failCount
                    System.err.println("Error: Unexpected RuntimeException encountered: " + e.getMessage());
                    failCount++;
                }
            }
        }

        // Convert the map values to a list to return
        return new ArrayList<>(uniqueMethodCalls.values());
    }

    /**
     * Dig into the internal method calls for recursive searching
     */
    private void digFunctionCallEntries(MethodDeclarationInfo currentDeclarationInfo, int depth) {

        if (depth > MAX_DIG_DEPTH) {
            return;
        }
        depth++;

        List<MethodCallEntry> internalTargetCalls = currentDeclarationInfo.getInnerMethodCalls();
        Set<String> targetPackages = new HashSet<>();

        for (MethodCallEntry callEntry : internalTargetCalls) {
            targetPackages.add(callEntry.getDeclaringType());
        }

        for (ParseResult<CompilationUnit> parseResult : this.currentParseResults) {
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                continue;
            }
            if (!parseResult.getResult().isPresent()) {
                continue;
            }
            CompilationUnit cu = parseResult.getResult().get();
            String packageLikePath = getPackageLikePathFromCU(cu);

            final int finalDepth = depth;
            targetPackages.stream().forEach(targetPackage -> {
                if (packageLikePath.startsWith(targetPackage)) {
                    List<MethodCallEntry> lookForCalls = filterCallsForPackage(targetPackage, internalTargetCalls);
                    // System.out.println("all internal calls: " + internalTargetCalls.toString());
                    // System.out.println("---------");
                    digFunctionCallEntriesHelper(cu, lookForCalls, finalDepth);
                }
            });

        }
    }

    /**
     * Helper function to dig into the internal method calls
     * it loops thru the method declarations to find match
     */
    private void digFunctionCallEntriesHelper(CompilationUnit cu, List<MethodCallEntry> lookForCalls, int depth) {

        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);

        String fullPath = cu.getStorage().get().getPath().toString();
        String currentPath = cu.getStorage().get().getPath().toString();
        currentPath = currentPath.split(decompressedPath)[1].substring(1);

        String packageLikePath = currentPath.replace(File.separator, ".") // Replace file separators with
                                                                          // dots
                .replaceAll(".java$", "");

        // * looping thru method declaration to find match
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            // Initialize MethodDeclarationInfo for the current method declaration

            int startLine = methodDeclaration.getBegin().map(pos -> pos.line).orElse(-1);
            int endLine = methodDeclaration.getEnd().map(pos -> pos.line).orElse(-1);
            String name = methodDeclaration.getName().asString();

            // ! we have to resolve the method declaration to get the Qualified signature,
            ResolvedMethodDeclaration resolvedDeclaration = methodDeclaration.resolve();
            String currentDeclarationSignature = resolvedDeclaration.getSignature().toString();

            for (MethodCallEntry lookForCall : lookForCalls) {

                if (lookForCall.getMethodName().equals(name)
                        && lookForCall.getMethodSignature().equals(currentDeclarationSignature)) {
                    // System.out.println("Found method: " + name + " in type: " + packageLikePath);
                    MethodDeclarationInfo currentDeclarationInfo = new MethodDeclarationInfo(fullPath, startLine,
                            endLine,
                            name,
                            currentDeclarationSignature);
                    lookForCall.setDeclarationInfo(currentDeclarationInfo);

                    // * we found the method we are looking for
                    // * we need to add the method declaration to the currentDeclarationInfo
                    List<MethodCallEntry> currentCallEntries = extractMethodCallsFromDeclaration(methodDeclaration);
                    currentDeclarationInfo.addInnerMethodCalls(currentCallEntries);

                    // * if there are more inner method calls, we need to dig into them again
                    if (currentDeclarationInfo.getInnerMethodCalls().size() > 0) {
                        digFunctionCallEntries(currentDeclarationInfo, depth);
                    }
                }
            }
        }
    }

    // info: ----- Helper functions: -----

    /*
     * Different java project might have different verison thus different syntax,
     * currentlly we will
     * pass whichever reports an error (but it's very likely a version issue)
     */

    private void initCombinedSolver(String fullPath, ProjectRoot projectRoot) {
        // System.out.println("Init combined solver for root path " + fullPath + " with
        // roots number "
        // + projectRoot.getSourceRoots().size());

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

    // * seperate the package like path from the compilation unit
    private String getPackageLikePathFromCU(CompilationUnit cu) {
        return cu.getStorage().map(storage -> storage.getPath().toString().split(decompressedPath)[1].substring(1)
                .replace(File.separator, ".")
                .replaceAll(".java$", ""))
                .orElse("");
    }

    /*
     * filter the internal method calls for the target package
     * 
     * @param targetPackage: the package to filter for
     * 
     * @param internalTargetCalls: the internal method calls to filter
     */
    private List<MethodCallEntry> filterCallsForPackage(String targetPackage,
            List<MethodCallEntry> internalTargetCalls) {
        return internalTargetCalls.stream()
                .filter(call -> targetPackage.equals(call.getDeclaringType()))
                .collect(Collectors.toList());
    }
}
