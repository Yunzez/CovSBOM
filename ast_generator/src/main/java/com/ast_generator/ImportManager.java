package com.ast_generator;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;

public class ImportManager {
    private Set<String> thirdPartyImports;
    Map<String, DependencyNode> dependencyMap;
    Path sourceRootPath;
    FunctionSignatureExtractor extractor;

    public ImportManager() {
        this.thirdPartyImports = new HashSet<>();
    }

    public ImportManager(Map<String, DependencyNode> dependencyMap, Path sourceRootPath) {
        this.sourceRootPath = sourceRootPath;
        this.thirdPartyImports = new HashSet<>();
        this.dependencyMap = dependencyMap;
        this.extractor = new FunctionSignatureExtractor(dependencyMap != null ? dependencyMap : null);
    }

    public void addImports(Set<String> imports) {
        thirdPartyImports.addAll(imports);
    }

    public Set<String> getThirdPartyImports() {
        return thirdPartyImports;
    }

    public void clearImports() {
        thirdPartyImports.clear();
    }

    public void removeImport(String imp) {
        thirdPartyImports.remove(imp);
    }

    public void printImports() {
        for (String imp : thirdPartyImports) {
            System.out.println(imp);
        }
    }

    public void analyzeImports() {
        System.out.println("current root pathL " + sourceRootPath);
        if (dependencyMap == null) {
            System.out.println("Dependency map is null");
            return;
        }
        
        List<Path> analyzeingPaths = Utils.traverseFiles(sourceRootPath, ".json");
        for (Path path : analyzeingPaths) {
            JsonObject ast = Utils.readAst(path.toString());

        }
    }
    // Additional methods as needed, such as clearImports, removeImport, etc.
}
