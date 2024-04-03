package com.ast_generator;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import com.github.javaparser.resolution.TypeSolver;
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
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration.Signature;

public class SourceJarAnalyzer {
    private Path jarPath;
    private DependencyNode dependency;
    private List<DependencyNode> allDependencies;
    private String decompressedPath;
    private Collection<String> targetPackages;
    private List<ParseResult<CompilationUnit>> currentParseResults;
    private String packageName;
    // * this is the packages we will be tracking again when we loop thru internal
    // methods
    private Path decompressDir;
    MethodCallReporter methodCallReporter;

    // ! max recursion depth for digging into method calls
    final int MAX_DIG_DEPTH = Settings.MAX_METHOD_CALL_DEPTH;
    final boolean RESTRICT_DEPTH = Settings.RESTRICT_DEPTH;
    // ! analysis statcs
    int totalCount = 0;
    int successCount = 0;
    int failCount = 0;

    int declarationResolveFailureCount = 0;

    // Unified constructor
    public SourceJarAnalyzer(DependencyNode dependency, List<DependencyNode> allDependencies,
            Collection<String> targetPackages, MethodCallReporter reporter,
            String decompressDir) {
        this.dependency = dependency;
        this.jarPath = Paths.get(dependency.getSourceJarPath());
        this.packageName = dependency.getGroupId();
        this.targetPackages = targetPackages; // Accepts any Collection<String>
        this.decompressDir = Paths.get(decompressDir);
        this.methodCallReporter = reporter;
        this.allDependencies = allDependencies;
    }

    public void analyze() throws IOException {
        decompressedPath = this.dependency.getSourceDecompressedPath();
        System.out.println(" - - - - - - - - - - - - - ");
        System.out.println("Analyze decompressed path of used dependency: " + decompressedPath);
        // Process the decompressed directory
        processDecompressedDirectory();
        System.out.println("Total third party method calls: " + totalCount + ", Success: " + successCount + ", Fail: "
                + failCount);

        System.out.println("Total declaration resolve failure: " + declarationResolveFailureCount);
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

                // if (!functionCallType.startsWith("java.") &&
                // !functionCallType.startsWith("javax.")) {
                // if (!functionCallType.startsWith(dependency.getGroupId())) {
                // System.out.println("interesting functionCallType: " + functionCallType + " "
                // + dependency.getGroupId());
                // }
                // }
                // if (functionCallType.startsWith("com.google.guava")) {
                // System.out.println("Guava is calling: " + functionCallType + ": " +
                // fullExpression);
                // }

                MethodSignatureKey key = new MethodSignatureKey(functionCallType, currentSignature);
                if (!functionCallType.startsWith("java.") && !functionCallType.startsWith("javax.")) {
                    if (!functionCallType.contains(dependency.getGroupId())) {
                        System.out.println("external functionCallType: " + functionCallType + " "
                                + dependency.getGroupId());
                    }
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
                        existingEntry.addLineNumber(lineNumber);
                    }
                }
            } catch (UnsolvedSymbolException e) {
                // System.out.println("Info: Unresolved method call to '" + e.getName() + "
                // might be external.");
                if (e.getName().contains("setDefaultSocketConfig")) {
                    System.out.println("cannot resolve setDefaultSocketConfig: " + e.getName());
                }
            } catch (UnsupportedOperationException e) {
                // Log the issue but do not treat as critical error
                // System.out.println("Warning: UnsupportedOperationException encountered.
                // Method may not be supported for resolution: " + e.getMessage());

            } catch (IllegalStateException e) {
                // System.err.println("Warning: Failed to resolve a type due to an
                // IllegalStateException. " +
                // "This may indicate a complex type usage not fully supported. " +
                // "Details: " + e.getMessage());
                failCount++;
            } catch (RuntimeException e) {
                // if (e.getMessage().contains("cannot be resolved")) {
                // // Log but do not increment failCount for unresolved external methods
                // System.out.println("Info: The method '" + e.getMessage().split("'")[1] + "'
                // cannot be resolved, possibly due to being an external dependency.");
                // } else {
                // // For other RuntimeExceptions, log as error and increment failCount
                // System.err.println("Error: Unexpected RuntimeException encountered: " +
                // e.getMessage());
                // failCount++;
                // }
            }
        }

        // Convert the map values to a list to return
        return new ArrayList<>(uniqueMethodCalls.values());
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
                if (currentDeclarationSignature.contains("javax")) {
                    System.out.println(currentDeclarationSignature + " " + fullPath);
                }
                int startLine = methodDeclaration.getBegin().map(pos -> pos.line).orElse(-1);
                int endLine = methodDeclaration.getEnd().map(pos -> pos.line).orElse(-1);
                String name = methodDeclaration.getName().asString();

                MethodDeclarationInfo currentDeclarationInfo = new MethodDeclarationInfo(fullPath, startLine, endLine,
                        name, currentDeclarationSignature);

                // * extract method calls from the method declaration
                List<MethodCallEntry> currentCallEntries = extractMethodCallsFromDeclaration(methodDeclaration);
                for (MethodCallEntry callEntry : currentCallEntries) {
                    callEntry.setCurrentLayer(1);
                }

                // * add the method calls to the currentDeclarationInfo
                currentDeclarationInfo.addInnerMethodCalls(currentCallEntries);

                Boolean pass = methodCallReporter.addDeclarationInfoForMethodinType(packageLikePath,
                        currentDeclarationInfo);

                if (pass) {
                    // * start digging
                    Set<MethodSignatureKey> currentClassSignatureContext = new HashSet<>();
                    digFunctionCallEntries(currentDeclarationInfo, 1, currentClassSignatureContext);
                }
            } catch (UnsolvedSymbolException e) {
                declarationResolveFailureCount++;
                // * when we fail to resolve, it means there are certain
                System.out.println(
                        "Warning: Could not resolve method declaration: " + packageLikePath + ", " +
                                methodDeclaration.getNameAsString() + " at: " + fullPath);
                System.out.println(" declaration: " + methodDeclaration.getDeclarationAsString());
                System.out.println(e.getMessage());
                // if (methodDeclaration.getDeclarationAsString()
                // .contains("addMapping(PathSpec pathSpec, WebSocketCreator creator)")
                // || methodDeclaration.getDeclarationAsString()
                // .contains("setToAttribute(ServletContext context, String key)")) {
                // e.printStackTrace();
                // }
            }
        }
    }

    /**
     * Dig into the internal method calls for recursive searching
     */
    private void digFunctionCallEntries(MethodDeclarationInfo currentDeclarationInfo, int depth,
            Set<MethodSignatureKey> currentClassSignatureContext) {

        if (depth > MAX_DIG_DEPTH && RESTRICT_DEPTH) {
            return;
        }
        depth++;

        if (depth > 50 && depth < 55) {
            System.out.println("Warning, Depth reached: " + depth);
            System.out.println(currentDeclarationInfo.toString());
        }

        if (depth == 55) {
            System.out.println(currentDeclarationInfo.toString());
            System.out.println("Paused, possible inifite recursion. Depth reached: " + depth);
            System.out.println("----------");
            return;
        }

        List<MethodCallEntry> internalTargetCalls = currentDeclarationInfo.getInnerMethodCalls();

        Set<String> targetPackages = currentDeclarationInfo.getAllDeclaringTypes();

        for (ParseResult<CompilationUnit> parseResult : this.currentParseResults) {
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                continue;
            }
            if (!parseResult.getResult().isPresent()) {
                continue;
            }

            CompilationUnit cu = parseResult.getResult().get();

            String packageLikePath = getPackageLikePathFromCU(cu);

            // int finalDepth = depth;
            List<MethodCallEntry> lookForCalls = new ArrayList<>();
            targetPackages.stream().forEach(targetPackage -> {
                if (packageLikePath.startsWith(targetPackage)) {
                    lookForCalls.addAll(filterCallsForPackage(targetPackage, internalTargetCalls));
                }
            });

            List<TypeDeclaration<?>> types = cu.getTypes();

            if (types.size() > 1) {
                for (TypeDeclaration<?> typeDeclaration : types) {
                    CompilationUnit newCompilationUnit = new CompilationUnit();
                    newCompilationUnit.addType(typeDeclaration.clone());
                    digFunctionCallEntriesHelper(cu, lookForCalls, depth, currentClassSignatureContext);
                }
            } else {
                digFunctionCallEntriesHelper(cu, lookForCalls, depth, currentClassSignatureContext);
            }

        }
    }

    /**
     * Helper function to dig into the internal method calls
     * it loops thru the method declarations to find match
     */
    private void digFunctionCallEntriesHelper(CompilationUnit cu, List<MethodCallEntry> lookForCalls, int depth,
            Set<MethodSignatureKey> currentClassSignatureContext) {

        if (lookForCalls.size() == 0) {
            return;
        }

        if (depth > 50 && depth < 55) {
            System.out.println("look for calls: ");
            System.out.println(lookForCalls.toString());
        }

        if (depth == 55) {
            System.out.println(lookForCalls.toString());
            System.out.println("Paused, possible inifite recursion. Depth reached: " + depth);
            System.out.println("----------");
            return;
        }

        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);

        String fullPath = cu.getStorage().get().getPath().toString();
        String currentPath = cu.getStorage().get().getPath().toString();
        currentPath = currentPath.split(decompressedPath)[1].substring(1);

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
                String methodKey = lookForCall.getDeclaringType() + "." + lookForCall.getMethodName()
                        + lookForCall.getMethodSignature();

                if (lookForCall.getMethodName().equals(name)
                        && lookForCall.getMethodSignature().equals(currentDeclarationSignature)) {

                    MethodDeclarationInfo currentDeclarationInfo = new MethodDeclarationInfo(fullPath, startLine,
                            endLine,
                            name,
                            currentDeclarationSignature);
                    lookForCall.setDeclarationInfo(currentDeclarationInfo);

                    // * we found the method we are looking for
                    // * we need to add the method declaration to the currentDeclarationInfo
                    List<MethodCallEntry> currentCallEntries = extractMethodCallsFromDeclaration(methodDeclaration);
                    List<MethodCallEntry> filteredCalls = new ArrayList<>();
                    for (MethodCallEntry callEntry : currentCallEntries) {
                        callEntry.setCurrentLayer(depth);
                        if (!currentClassSignatureContext.contains(callEntry.getMethodSignatureKey())) {
                            filteredCalls.add(callEntry);
                            currentClassSignatureContext.add(callEntry.getMethodSignatureKey());
                        }
                    }
                    currentDeclarationInfo.addInnerMethodCalls(filteredCalls);

                    // * if there are more inner method calls, we need to dig into them again
                    if (currentDeclarationInfo.getInnerMethodCalls().size() > 0) {
                        // * create a new context for the next level of method calls
                        Set<MethodSignatureKey> clonedContext = new HashSet<>(currentClassSignatureContext);
                        digFunctionCallEntries(currentDeclarationInfo, depth, clonedContext);
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
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(new ReflectionTypeSolver(true));
        // Loop through each dependency and add it to the CombinedTypeSolver

        List<DependencyNode> dependencyNodes = dependency.getChildren();
       

        // for (DependencyNode dependency : dependencyNodes) {
        // Path jarPath = Paths.get(dependency.getJarPath());

        // if (Files.exists(jarPath)) {
        // try {
        // System.out.println("Adding sub dependency: " + jarPath);
        // // Add JarTypeSolver for each JAR file (external dependency)
        // combinedSolver.add(new JarTypeSolver(jarPath.toString()));
        // } catch (Exception e) {
        // System.out.println("Failed to add dependency: " + jarPath.toString());
        // e.printStackTrace();
        // }
        // } else {
        // System.out.println("Jar file does not exist, skipping: " + jarPath);
        // dependency.setIsValid(false);
        // }
        // }

        // * here we are adding all dependencies due to maven tree's structure
        System.out.println("Adding sub dependencies: " + allDependencies.size());
        for (DependencyNode dependency : allDependencies) {
            Path jarPath = Paths.get(dependency.getJarPath());

            if (Files.exists(jarPath)) {
                try {
                    // System.out.println("Adding dependency: " + jarPath);
                    // Add JarTypeSolver for each JAR file (external dependency)
                    combinedSolver.add(new JarTypeSolver(jarPath.toString()));
                } catch (Exception e) {
                    System.out.println("Failed to add dependency: " + jarPath.toString());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Jar file does not exist, skipping: " + jarPath);
                dependency.setIsValid(false);
            }
        }

        projectRoot.getSourceRoots().forEach(sourceRoot -> {
            ParserConfiguration sourceRootConfiguration = sourceRoot.getParserConfiguration();
            ParserConfiguration.LanguageLevel languageLevel = Utils.getLanguageLevelFromVersion(Settings.JAVA_VERSION);
            sourceRootConfiguration.setLanguageLevel(languageLevel);

            combinedSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot()));
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);

            sourceRootConfiguration.setSymbolResolver(symbolSolver);

            // sourceRoot.setParserConfiguration(sourceRootConfiguration);
        });

        // * add the combined solver to the static parser in case we need to resolve
        // StaticJavaParser.setConfiguration(new
        // ParserConfiguration().setSymbolResolver(new
        // JavaSymbolSolver(combinedSolver)));
        System.out.println("Combined solver initialized.");

        // ! testing purposes only
        // try {
        //     Field field = CombinedTypeSolver.class.getDeclaredField("elements");
        //     field.setAccessible(true);
        //     List<TypeSolver> solvers = (List<TypeSolver>) field.get(combinedSolver);

        //     for (TypeSolver solver : solvers) {
        //         if (solver instanceof JarTypeSolver) {
        //             System.out.println("JarTypeSolver: " + ((JarTypeSolver) solver).getClass());
        //         } else {
        //             System.out.println("TypeSolver: " + solver.getClass());
        //         }
        //     }
        // } catch (NoSuchFieldException | IllegalAccessException e) {
        //     e.printStackTrace();
        // }

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
