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
import com.github.javaparser.resolution.SymbolResolver;
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
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class SourceJarAnalyzer {
    private Path jarPath;
    private JavaSymbolSolver symbolSolver;
    private DependencyNode dependency;
    private List<DependencyNode> allDependencies;
    private String decompressedPath;
    private Collection<String> targetPackages;
    private List<ParseResult<CompilationUnit>> currentParseResults;
    private String packageName;
    private MethodCallBuffer loadingBuffer;
    private MethodCallBuffer doneBuffer;

    private DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver;
    // * this is the packages we will be tracking again when we loop thru internal
    // methods
    private Path decompressDir;
    private boolean extendedAnalysis = false;
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
            MethodCallBuffer loadingBuffer,
            MethodCallBuffer doneBuffer,
            Collection<String> targetPackages,
            MethodCallReporter reporter,
            String decompressDir) {
        this.dependency = dependency;
        this.jarPath = Paths.get(dependency.getSourceJarPath());
        this.packageName = dependency.getGroupId();
        this.targetPackages = targetPackages; // Accepts any Collection<String>
        this.decompressDir = Paths.get(decompressDir);
        this.methodCallReporter = reporter;
        this.allDependencies = allDependencies;
        this.loadingBuffer = loadingBuffer;
        this.doneBuffer = doneBuffer;
    }

    public void setDeclaringTypeToDependencyResolver(
            DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver) {
        this.declaringTypeToDependencyResolver = declaringTypeToDependencyResolver;
    }

    public void analyze() throws IOException {
        decompressedPath = this.dependency.getSourceDecompressedPath();
        System.out.println(" - - - - - - - - - - - - - ");
        System.out.println("Analyze decompressed path of used dependency: " + decompressedPath);
        System.out.println(loadingBuffer.getMethodCalls(dependency).size() + " method calls in loading buffer.");

        // Process the decompressed directory
        processDecompressedDirectory();
        System.out.println("Total third party method calls: " + totalCount + ", Success: " + successCount + ", Fail: "
                + failCount);

        System.out.println("Total declaration resolve failure: " + declarationResolveFailureCount);
    }

    /**
     * Set the extended analysis flag, when set to true, the analyzer will skip
     * addDeclarationInfoForMethodinType in processMethodDeclarationForCUorTP
     * addDeclarationInfoForMethodinType should only be used in the first layer of
     * dependency analysis
     * for subdependency analysis, we should set this flag to true
     * 
     * @function processMethodDeclarationForCUorTP
     * @param extendedAnalysis
     */
    public void setExtendedAnalysis(boolean extendedAnalysis) {
        this.extendedAnalysis = extendedAnalysis;
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

        for (SourceRoot sourceRoot : projectRootForSolving.getSourceRoots()) {
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
                CompilationUnit cu = parseResult.getResult().get();
                String filePath = cu.getStorage().get().getPath().toString();

                // * check if the current path is in the target package
                filePath = filePath.split(decompressedPath)[1].substring(1);

                String basePackageLikePath = filePath.replace(File.separator, ".") // Replace file separators with
                                                                                   // dots
                        .replaceAll(".java$", "");

                // * we will get all the classes of this CU, and loop thru them to find the
                // * required packages, this help us finding all sub-class
                processTypes(cu.getTypes(), basePackageLikePath, cu.getStorage().get().getPath(), true);

            }
        }
    }

    private boolean matchesTargetPackage(String packageLikePath) {
        boolean hasMatch = targetPackages.stream()
                .anyMatch(targetPackage -> Utils.startsWithByDots(targetPackage.trim(), packageLikePath));
        return hasMatch;
    }

    /**
     * Recursively process inner classes for one file
     * 
     * @param types               - the list of types to process
     * @param basePackageLikePath - the package like path of the base type
     * @param filePath            - the path object of the file
     * @param isTopLevel          - whether the types are top level or inner classes
     */
    private void processTypes(List<TypeDeclaration<?>> types, String basePackageLikePath, Path filePath,
            boolean isTopLevel) {

        for (TypeDeclaration<?> type : types) {
            // Construct the package-like path for this type
            String typePath = isTopLevel ? basePackageLikePath : basePackageLikePath + "." + type.getNameAsString();

            if (matchesTargetPackage(typePath)) {
                if (dependency.toShortString().contains("org.apache.httpcomponents:httpcore:4.4.10")
                        && typePath.contains("http.util.TextUtils")) {
                    System.out.println(" process type org.apache.http.util.TextUtils");
                    System.out.println("  -----  ");
                }
                processTypeDeclaration(type, typePath, filePath.toString());
            }

            // Recursively process member types for inner classes
            if (!type.getMembers().isEmpty()) {
                type.getMembers().stream()
                        .filter(member -> member instanceof TypeDeclaration)
                        .map(member -> (TypeDeclaration<?>) member)
                        .forEach(memberType -> processTypes(Collections.singletonList(memberType), typePath, filePath,
                                false));
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
                // System.out.println(
                // "Warning: UnsupportedOperationException encountered. Method may not be
                // supported for resolution: "
                // + e.getMessage());

            } catch (IllegalStateException e) {
                // System.err.println("Warning: Failed to resolve a type due to an
                // IllegalStateException. " +
                // "This may indicate a complex type usage not fully supported. " +
                // "Details: " + e.getMessage());
                failCount++;
            } catch (RuntimeException e) {
                // Log but do not increment failCount for unresolved external methods
                if (e.getMessage().contains("cannot be resolved")) {
                    System.out.println("Warning: The method '" + e.getMessage().split("'")[1]
                            + "'cannot be resolved, possibly due to being an external dependency.");
                } else {
                    System.err.println("Error: Unexpected RuntimeException encountered: " +
                            e.getMessage());
                    failCount++;
                }
            }
        }
        return new ArrayList<>(uniqueMethodCalls.values());
    }

    private void processTypeDeclaration(TypeDeclaration<?> tp, String typePath, String fullPath) {
        List<MethodDeclaration> methodDeclarations = tp.findAll(MethodDeclaration.class);

        if (methodDeclarations.size() > 0) {
            processMethodDeclarationForCUorTP(methodDeclarations, fullPath, typePath);
        }
    }

    /**
     * Process the first level of method calls
     * 
     * @param cu       the compilation unit to process
     * @param typePath the package like path of the type (only if it's an inner
     *                 class, otherwise null)
     */
    private void processCompilationUnit(CompilationUnit cu) {
        String fullPath = cu.getStorage().get().getPath().toString();
        String packageLikePath = getPackageLikePathFromCU(cu);

        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
        processMethodDeclarationForCUorTP(methodDeclarations, fullPath, packageLikePath);
    }

    /**
     * Process the method declarations for the current compilation unit or type
     * 
     * @param methodDeclarations the list of method declarations to process
     * @param fullPath           the full path of the compilation unit
     * @param packageLikePath    the package like path of the compilation unit
     */
    private void processMethodDeclarationForCUorTP(List<MethodDeclaration> methodDeclarations, String fullPath,
            String packageLikePath) {
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
                for (MethodCallEntry callEntry : currentCallEntries) {
                    callEntry.setCurrentLayer(1);

                    // if (extendedAnalysis) {
                    // if (!doneBuffer.hasMethodCall(callEntry)) {
                    // loadingBuffer.addMethodCall(callEntry);
                    // }
                    // }

                }

                // * add the method calls to the currentDeclarationInfo
                currentDeclarationInfo.addInnerMethodCalls(currentCallEntries);

                boolean pass = false;
                // * if it's extended analysis, we shall not go in to methodCallReporter for
                // reference
                if (!extendedAnalysis) {
                    pass = methodCallReporter.addDeclarationInfoForMethodinType(packageLikePath,
                            currentDeclarationInfo);
                } else {
                    List<Boolean> passList = new ArrayList<>();
                    passList.add(pass);

                    List<MethodCallEntry> foundMethodCalls = new ArrayList<>();

                    loadingBuffer.getMethodCalls(dependency).stream().forEach(call -> {
                        if (call.getMethodSignatureKey().getMethodSignature().equals(currentDeclarationSignature)) {
                            passList.set(0, true);
                            call.setDeclarationInfo(currentDeclarationInfo);
                            foundMethodCalls.add(call);
                            // add the method to found
                        }
                    });

                    for (MethodCallEntry call : foundMethodCalls) {
                        doneBuffer.addMethodCall(call);
                        loadingBuffer.removeMethodCall(call);
                    }
                    pass = passList.get(0);
                }

                if (currentDeclarationInfo.getDeclarationSignature()
                        .equals("setServer(org.eclipse.jetty.server.Server)")
                        && fullPath.contains("org.eclipse.jetty.jetty-webapp/org/eclipse/jetty/webapp/WebAppContext")) {
                    System.out.println("Found setServer");
                    System.out.println(currentDeclarationInfo.toDebugString());
                    System.out.println("pass checked: " + pass);
                }

                if (pass) {
                    // * start digging
                    Set<MethodSignatureKey> currentClassSignatureContext = new HashSet<>();

                    digFunctionCallEntries(currentDeclarationInfo, 1, currentClassSignatureContext);
                }
            } catch (UnsolvedSymbolException e) {
                declarationResolveFailureCount++;
                // * when we fail to resolve, it means there are certain
                // System.out.println(
                // "Warning: Could not resolve method declaration: " + packageLikePath + ", " +
                // methodDeclaration.getNameAsString() + " at: " + fullPath);
                // System.out.println(" declaration: " +
                // methodDeclaration.getDeclarationAsString());
                // System.out.println(e.getMessage());

            }
        }
    }

    /**
     * Loop through parse results of one jar, find type that matches the functions
     * in currentDeclarationInfo
     * and analyze it in helper
     * 
     * @param currentDeclarationInfo
     * @param depth
     * @param currentClassSignatureContext
     */
    private void digFunctionCallEntries(MethodDeclarationInfo currentDeclarationInfo, int depth,
            Set<MethodSignatureKey> currentClassSignatureContext) {

        if (depth > MAX_DIG_DEPTH && RESTRICT_DEPTH) {
            return;
        }
        depth++;

        if (depth == 55) {
            System.out.println(currentDeclarationInfo.toString());
            System.out.println("Paused, possible inifite recursion. Depth reached: " + depth);
            System.out.println("----------");
            return;
        }

        if (currentDeclarationInfo.getInnerMethodCalls().size() == 0) {
            return;
        }

        Set<MethodCallEntry> unresolvedMethodEntries = new HashSet<>();

        Set<String> targetPackages = currentDeclarationInfo.getAllDeclaringTypes();
        List<MethodCallEntry> internalTargetCalls = currentDeclarationInfo.getInnerMethodCalls();
        if (currentDeclarationInfo.getDeclarationSignature()
                .equals("setServer(org.eclipse.jetty.server.Server)") &&
                currentDeclarationInfo.getDeclarationStartLine() == 1379 &&
                currentDeclarationInfo.getDeclarationEndLine() == 1383) {
            System.out.println("1 Continue Found setServer");
            System.out.println(currentDeclarationInfo.toDebugString());
            System.out.println("target packages: " + currentDeclarationInfo.getAllDeclaringTypes().toString());
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

            // * collect calls that can be found and put to done buffer
            // * put calls that are not in the target package into loading buffer
            List<MethodCallEntry> lookForCalls = new ArrayList<>();
            Path cuPath = cu.getStorage().get().getPath();

            // * we filter based on class declaring type, this help us get all the possible
            // declaring type for this
            Set<String> cuTypes = getAllPossibleDeclaringTypesFromCU(cu, packageLikePath, null);

            if (currentDeclarationInfo.getDeclarationSignature()
                    .equals("setServer(org.eclipse.jetty.server.Server)") &&
                    currentDeclarationInfo.getDeclarationStartLine() == 1379 &&
                    currentDeclarationInfo.getDeclarationEndLine() == 1383
                    && cuPath.toString().contains("jetty-server/org/eclipse/jetty/server/")) {
                System.out.println("2 Continue Found setServer related file");
                System.out.println("all sub types: " + cuTypes.toString());
            }
            // ! todo, I NEED TO FIGURE OUT WAY THE CALL GOT LOST
            targetPackages.stream().forEach(targetPackage -> {
                cuTypes.stream().forEach(internalType -> {

                    if (Utils.startsWithByDots(internalType, targetPackage)) {
                        if (currentDeclarationInfo.getDeclarationSignature()
                                .equals("setServer(org.eclipse.jetty.server.Server)") &&
                                targetPackage.toString().contains(".eclipse.jetty.server.handler.ContextHandler") &&
                                cuPath.toString().contains(
                                        "decompressed/org.eclipse.jetty.jetty-server/org/eclipse/jetty/server/handler/ContextHandler")) {
                            System.out.println("3 Continue Found setServer at it's file, pass start with dots");
                            System.out.println("look for calls:" + internalTargetCalls);
                        }
                        lookForCalls.addAll(filterCallsForPackage(targetPackage, internalTargetCalls));
                    }
                });
            });

            if (currentDeclarationInfo.getInnerMethodCalls().size() > 0 && lookForCalls.size() == 0) {
                // * if we cannot find the target package in the current CU, we need to skip
                // * this CU and add the target calls to the unresolvedMethodEntries
                unresolvedMethodEntries.addAll(internalTargetCalls);
                continue;
            } else {
                // * remove the calls we have found
                unresolvedMethodEntries.removeAll(lookForCalls);
            }

            if (currentDeclarationInfo.getDeclarationSignature()
                    .equals("setServer(org.eclipse.jetty.server.Server)") &&
                    currentDeclarationInfo.getDeclarationStartLine() == 1379 &&
                    currentDeclarationInfo.getDeclarationEndLine() == 1383) {
                System.out.println("4 Continue Found setServer");
                System.out.println("look for calls:" + lookForCalls.toString());
                System.out.println("original calls: " + internalTargetCalls.toString());
            }

            List<TypeDeclaration<?>> types = cu.getTypes();
            if (types.size() == 1) {
                digFunctionCallEntriesHelper(cuPath.toString(), types.get(0), lookForCalls, depth,
                        new HashSet<>(currentClassSignatureContext));
            } else {
                digType(types, packageLikePath, cuPath, true, lookForCalls, depth,
                        new HashSet<>(currentClassSignatureContext));
            }
        }
        
        // ! important: 
        // * if we have unresolved method entries, we need to add them to the loading
        // * buffer
        for (MethodCallEntry unresolvedEntry : unresolvedMethodEntries) {
            if (!doneBuffer.hasMethodCall(unresolvedEntry)) {
                loadingBuffer.addMethodCall(unresolvedEntry);
            }
        }
    }

    private Set<String> getAllPossibleDeclaringTypesFromCU(CompilationUnit cu, String basePackagePath,
            Set<String> result) {
        if (result == null) {
            result = new HashSet<>();
        }
        List<TypeDeclaration<?>> types = cu.getTypes();
        for (TypeDeclaration<?> type : types) {
            result.add(basePackagePath);
            // Recursively process any nested types within the current type
            collectNestedTypes(type, basePackagePath, result);
        }
        return result;
    }

    private void collectNestedTypes(TypeDeclaration<?> type, String currentPath, Set<String> result) {
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof TypeDeclaration) {
                TypeDeclaration<?> nestedType = (TypeDeclaration<?>) member;
                String nestedTypeName = currentPath + "." + nestedType.getNameAsString();
                result.add(nestedTypeName);
                // Recurse into this nested type
                collectNestedTypes(nestedType, nestedTypeName, result);
            }
        }
    }

    private void digType(List<TypeDeclaration<?>> types, String basePackageLikePath, Path filePath,
            boolean isTopLevel, List<MethodCallEntry> lookForCalls, int depth,
            Set<MethodSignatureKey> currentClassSignatureContext) {
        for (TypeDeclaration<?> type : types) {
            // Construct the package-like path for this type
            String typePath = isTopLevel ? basePackageLikePath : basePackageLikePath + "." + type.getNameAsString();

            digFunctionCallEntriesHelper(filePath.toString(), type, lookForCalls, depth,
                    new HashSet<>(currentClassSignatureContext));

            // Recursively process member types for inner classes
            if (!type.getMembers().isEmpty()) {
                type.getMembers().stream()
                        .filter(member -> member instanceof TypeDeclaration)
                        .map(member -> (TypeDeclaration<?>) member)
                        .forEach(memberType -> digType(Collections.singletonList(memberType), typePath,
                                filePath,
                                false, lookForCalls, depth, currentClassSignatureContext));
            }
        }
    }

    /**
     * Helper function to dig into the internal method calls
     * it loops thru the method declarations to find match
     */
    private void digFunctionCallEntriesHelper(String fullPath, TypeDeclaration<?> currentType,
            List<MethodCallEntry> lookForCalls, int depth,
            Set<MethodSignatureKey> currentClassSignatureContext) {

        if (lookForCalls.size() == 0) {
            return;
        }

        // lookForCalls.stream().forEach(call -> {
        // if
        // (call.getMethodSignature().equals("setServer(org.eclipse.jetty.server.Server)")
        // &&
        // call.getDeclaringType().equals("org.eclipse.jetty.server.handler.ContextHandler"))
        // {
        // System.out.println("dig at ");
        // System.out.println(fullPath);
        // }
        // });

        if (depth == 55) {
            System.out.println(lookForCalls.toString());
            System.out.println("Paused, possible inifite recursion. Depth reached: " + depth);
            System.out.println("----------");
            return;
        }

        List<MethodDeclaration> methodDeclarations = currentType.findAll(MethodDeclaration.class);

        String currentPath = String.valueOf(fullPath);
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
                // skip anonymous function
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

                    // ! potentially correct
                    doneBuffer.addMethodCall(lookForCall);
                    loadingBuffer.removeMethodCall(lookForCall);

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

        });

        // * add the combined solver to the static parser in case we need to resolve
        StaticJavaParser
                .setConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedSolver)));
        this.symbolSolver = new JavaSymbolSolver(combinedSolver);
        System.out.println("Combined solver initialized.");

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
        if (targetPackage.toString().contains(".eclipse.jetty.server.handler.ContextHandler")) {
            System.out.println("3 Continue Found setServer");
            System.out.println("look for calls:" + internalTargetCalls.toString());
            System.out.println(targetPackage);
        }
        List<MethodCallEntry> internCallEntries = internalTargetCalls.stream()
                .filter(call -> {
                    if (call.getDeclaringType().contains(".Anonymous-")) {
                        doneBuffer.addMethodCall(call);
                        loadingBuffer.removeMethodCall(call);
                        return false;
                    }

                    // if
                    // (call.getDeclaringType().equals("org.eclipse.jetty.server.handler.ContextHandler"))
                    // {
                    // System.out.println("check " + call.getDeclaringType() + " at filter");
                    // System.out.println(call.getFullExpression());
                    // System.out.println(targetPackage);
                    // }

                    if (targetPackage.equals(call.getDeclaringType())) {

                        // ! potentially correct
                        // doneBuffer.addMethodCall(call);
                        // loadingBuffer.removeMethodCall(call);
                        return true;
                    } else {
                        // only add to loading buffer if it's not already in done buffer
                        if (!doneBuffer.hasMethodCall(call)) {
                            loadingBuffer.addMethodCall(call);
                        }
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (extendedAnalysis) {
            // ! we only add extra calls to resolve if it's extended analysis, because first
            // time analysis only gather loading buffer, not spend it
            Set<MethodCallEntry> otherCallsToResolve = loadingBuffer.getMethodCalls(dependency);
            internCallEntries.addAll(otherCallsToResolve);
        }

        return internCallEntries;
    }

}
