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

    private Map<String, Set<String>> typeJarLookup = new HashMap<String, Set<String>>();
    public DependencyAnalyzer() {
    }

    public DependencyAnalyzer(Map<String, Dependency> dependencyMap,  MethodCallReporter methodCallReporter) {
        this.dependencyMap = dependencyMap;
        this.methodCallReporter = methodCallReporter;
    }

    public void analyze() {
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
                if (typeJarLookup.get(dependency.getJarPath()) == null) {
                    typeJarLookup.put(dependency.getJarPath(), new HashSet<String>());
                }

                if (typeJarLookup.get(dependency.getJarPath()).contains(declaringType)) {
                    continue;
                }

                System.out.println("Found dependency for " + declaringType + " : " + dependency.getJarPath());
                typeJarLookup.get(dependency.getJarPath()).add(declaringType);
            }
        }
    }

    
}
