package com.ast_generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyAnalyzer {
    private Map<String, Dependency> dependencyMap;
    private MethodCallReporter methodCallReporter;


    // * Map of jarPath to a set of types that are found in the jar
    // * Key: jarPath, Value: Set of types
    private Map<Dependency, Set<String>> typeToJarLookup = new HashMap<Dependency, Set<String>>();

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
        System.out.println(typeToJarLookup.toString());

        for (Dependency dependency : typeToJarLookup.keySet()) {
            String jarPath = dependency.getSourceJarPath();
            // * we get all the parth we need to analyze for this jar 
            Set<String> types = typeToJarLookup.get(dependency);
            SourceJarAnalyzer sourceJarAnalyzer = new SourceJarAnalyzer(dependency, types, methodCallReporter, "decompressed");
            
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
            if (groupIdPart.startsWith("java"))  {
                continue;
            }
            // System.out.println("type: " + declaringType + " groupIdPart: " + groupIdPart + " compare to " + dependency.getGroupId());
            if (groupIdPart.contains(dependency.getGroupId())) {
                if (typeToJarLookup.get(dependency) == null) {
                    typeToJarLookup.put(dependency, new HashSet<String>());
                }

                if (typeToJarLookup.get(dependency).contains(declaringType)) {
                    continue;
                }

                typeToJarLookup.get(dependency).add(declaringType);
            }
        }
    }

    
}
