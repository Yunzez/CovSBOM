package com.ast_generator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ast_generator.Utils.DependencyCollector;

public class DependencyAnalyzer {
    private Map<String, DependencyNode> dependencyMap;
    private MethodCallReporter methodCallReporter;
    private Set<String> unresolvedTypes = new HashSet<String>();
    private List<String> jarDecompressedPaths = new ArrayList<String>();
    Map<DependencyNode, HashSet<MethodSignatureKey>> loadingBuffer = new HashMap<DependencyNode, HashSet<MethodSignatureKey>>();
    Map<DependencyNode, HashSet<MethodSignatureKey>> doneBuffer = new HashMap<DependencyNode, HashSet<MethodSignatureKey>>();

    // * Map of jarPath to a set of types that are found in the jar
    // * Key: jarPath, Value: Set of types
    private Map<DependencyNode, Set<String>> typeToJarLookup = new HashMap<DependencyNode, Set<String>>();

    public DependencyAnalyzer() {
    }

    public DependencyAnalyzer(Map<String, DependencyNode> dependencyMap, MethodCallReporter methodCallReporter) {
        this.dependencyMap = dependencyMap;
        this.methodCallReporter = methodCallReporter;
    }

    public void analyze() {

        // this steps will decompressed all the jars iin the dependencyMap
        // we will also update the dependency's attribute sourceDecompressedPath

        List<DependencyNode> dependencies = new ArrayList<DependencyNode>();
        for (DependencyNode dependency : dependencyMap.values()) {
            dependencies.addAll(DependencyCollector.collectAllDependencies(dependency));
        }

        System.out.println("all dependencies: " + dependencies.size());

        // decompress all jars
        this.jarDecompressedPaths = Utils.decompressAllJars(dependencies, "decompressed");
        System.out.println("done decompressing jars");
        DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver = new DeclaringTypeToDependencyResolver(
                dependencies, jarDecompressedPaths);
        // System.out.println("jarDecompressedPaths: " +
        // jarDecompressedPaths.toString());
        // * find all required jar and save the results in typeToJarLookup
        findRequiredJars(declaringTypeToDependencyResolver);

        System.out.println("done findRequiredJars");
        // * initialize buffers
        MethodCallBuffer loadingBuffer = new MethodCallBuffer(dependencies, declaringTypeToDependencyResolver);
        MethodCallBuffer doneBuffer = new MethodCallBuffer(dependencies, declaringTypeToDependencyResolver);

        System.out.println("total unique types: " + methodCallReporter.getUniqueTypes().size());
        System.out.println("unresolved types: " + unresolvedTypes.size());
        System.out.println(unresolvedTypes.toString());

        // * only analyze jars that are used in the program and gather loading buffer
        for (DependencyNode dependency : typeToJarLookup.keySet()) {
            // * we get all the parth we need to analyze for this jar
            Set<String> types = typeToJarLookup.get(dependency);
            SourceJarAnalyzer sourceJarAnalyzer = new SourceJarAnalyzer(dependency, dependencies, loadingBuffer,
                    doneBuffer,
                    types, methodCallReporter, "decompressed");
            sourceJarAnalyzer.setDeclaringTypeToDependencyResolver(declaringTypeToDependencyResolver);

            try {
                sourceJarAnalyzer.analyze();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("------- entering extended analysis -------");
        int faultCatchCount = 0;

        // skip this for now
        while (loadingBuffer.size() > 0) {

            System.out.println("***");
            System.out.println("processing loading buffer: " + faultCatchCount);
            System.out.println("***");
            faultCatchCount++;
            for (DependencyNode dependency : loadingBuffer.getKeys()) {

                // * we update the jarPath for type for reporter here
                // for (MethodCallEntry methodCallEntry :
                // loadingBuffer.getMethodCalls(dependency)) {
                // String declaringType = methodCallEntry.getDeclaringType();
                // declaringTypeToDependencyResolver.getDependencyForDeclaringType(declaringType);
                // }

                // * we get all the types we need to analyze for this jar
                Set<String> types = new HashSet<String>();

                loadingBuffer.getMethodCalls(dependency).forEach(methodSignatureKey -> {
                    types.add(methodSignatureKey.getDeclaringType());
                });

                if (types.size() == 0) {
                    continue;
                }

                SourceJarAnalyzer sourceJarAnalyzer = new SourceJarAnalyzer(dependency, dependencies, loadingBuffer,
                        doneBuffer,
                        types, methodCallReporter, "decompressed");
                sourceJarAnalyzer.setExtendedAnalysis(true); // enable subdependency analysing
                // sourceJarAnalyzer.setDeclaringTypeToDependencyResolver(declaringTypeToDependencyResolver);

                try {
                    sourceJarAnalyzer.analyze();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (faultCatchCount > 10) {
                System.out.println("pause operation: " + faultCatchCount);
                break;
            }
        }

        System.out.println(loadingBuffer.toString());
        methodCallReporter.setTypeToJarReference(typeToJarLookup);

        for (DependencyNode dependency : doneBuffer.getKeys()) {
            if (dependency.toShortString().contains("org.apache.httpcomponents:httpclient")) {
                System.out.println("doneBuffer for org.apache.httpcomponents:httpclient");
                doneBuffer.getMethodCalls(dependency).forEach(methodSignatureKey -> {
                    if (methodSignatureKey.getDeclaringType().contains("org.apache.http.util.TextUtils")) {
                        System.out.println("found org.apache.http.util.TextUtils type function marked as done");
                        System.out.println(methodSignatureKey.toString());
                    }

                });
            }
        }
    }

    /**
     * Find all required jars and save the results in typeToJarLookup
     */
    private void findRequiredJars(DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver) {

        // first layer unique types
        List<String> uniqueTypes = methodCallReporter.getUniqueTypes();

        for (String declaringType : uniqueTypes) {
            if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")
                    || declaringType.startsWith(methodCallReporter.getParentPackageName())) {
                continue;
            }
            DependencyNode matchedDependency = declaringTypeToDependencyResolver
                    .getDependencyForDeclaringType(declaringType);
            if (matchedDependency != null) {
                if (typeToJarLookup.get(matchedDependency) == null) {
                    typeToJarLookup.put(matchedDependency, new HashSet<String>());
                }
                typeToJarLookup.get(matchedDependency).add(declaringType);
                unresolvedTypes.remove(declaringType); // Mark as resolved
                // break; // Stop searching once matched
            } else {
                unresolvedTypes.add(declaringType);
            }
        }

    }

}
