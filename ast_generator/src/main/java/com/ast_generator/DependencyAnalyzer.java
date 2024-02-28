package com.ast_generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;

public class DependencyAnalyzer {
    private Map<String, Dependency> dependencyMap;
    private MethodCallReporter methodCallReporter;


    // * Map of jarPath to a set of types that are found in the jar
    // * Key: jarPath, Value: Set of types
    private Map<String, Set<String>> typeToJarLookup = new HashMap<String, Set<String>>();

    public DependencyAnalyzer() {
    }

    public DependencyAnalyzer(Map<String, Dependency> dependencyMap,  MethodCallReporter methodCallReporter) {
        this.dependencyMap = dependencyMap;
        this.methodCallReporter = methodCallReporter;
    }

    public void analyze() {

        // * find all required jar and save the results in typeToJarLookup
        findRequiredJars();

        // * analyze jars
       
        for (String jarPath : typeToJarLookup.keySet()) {
            
            // * we get all the parth we need to analyze for this jar 
            Set<String> types = typeToJarLookup.get(jarPath);
            SourceJarAnalyzer sourceJarAnalyzer = new SourceJarAnalyzer(jarPath, types, methodCallReporter, "decompressed");
            try {
                sourceJarAnalyzer.analyze();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void findRequiredJars() {
        Map<String, List<MethodCallEntry>> reportMap = methodCallReporter.getReportMap();
        for (String javaFilePath : reportMap.keySet()) {
            List<MethodCallEntry> currentMethodCallEntries = reportMap.get(javaFilePath);
            // findJarPathForType(declaringType);
            for (MethodCallEntry methodCallEntry : currentMethodCallEntries) {
                String declaringType = methodCallEntry.getDeclaringType();
                findJarPathForType(declaringType);
            }
        }
    }


    /*
     * Create a map of jarPath to a set of types that are found in the jar
     * Key: jarPath, Value: Set of types
     */
    private void findJarPathForType(String declaringType) {
        // Example logic to extract groupId or a part of the package name
        String groupIdPart = declaringType.substring(0, declaringType.lastIndexOf('.'));
        
        // Search in dependencyMap for a matching dependency
        for (Dependency dependency : dependencyMap.values()) {
            if (groupIdPart.startsWith(dependency.getGroupId())) {
                if (typeToJarLookup.get(dependency.getSourceJarPath()) == null) {
                    typeToJarLookup.put(dependency.getSourceJarPath(), new HashSet<String>());
                }

                if (typeToJarLookup.get(dependency.getSourceJarPath()).contains(declaringType)) {
                    continue;
                }

                System.out.println("Found dependency for " + declaringType + " : " + dependency.getSourceJarPath());
                typeToJarLookup.get(dependency.getSourceJarPath()).add(declaringType);
            }
        }
    }

    
}
