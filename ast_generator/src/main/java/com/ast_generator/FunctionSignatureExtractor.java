package com.ast_generator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FunctionSignatureExtractor {

    private Set<String> methodSignatures;
    private Map<String, Dependency> dependencyMap;
    private Set<String> importSet;
    private Set<String> thirdPartyImports;

    public FunctionSignatureExtractor() {
        this.methodSignatures = new HashSet<>();
    }

    public FunctionSignatureExtractor(Map<String, Dependency> dependencyMap) {
        this.methodSignatures = new HashSet<>();
        this.dependencyMap = dependencyMap;
        this.importSet = new HashSet<>();
        this.thirdPartyImports = new HashSet<>();
    }

    /**
     * Extracts third-party import statements from a given CompilationUnit (AST).
     * 
     * @param compilationUnit The AST of a Java file.
     */
    public void extractThirdPartyImports(CompilationUnit compilationUnit) {
        compilationUnit.findAll(ImportDeclaration.class).forEach(importDecl -> {
            String importName = importDecl.getNameAsString();
            // Check if this is a third-party import
            System.out.println("importName: " + importName);
            if (isThirdPartyImport(importName)) {
                thirdPartyImports.add(importName);
            }
        });

        System.out.println("thirdPartyImports: " + thirdPartyImports.toString());
    }


    /**
     * Checks if the import is from a third-party library.
     * 
     * @param importName The import statement to check.
     * @return true if it's a third-party import, false otherwise.
     */
    private boolean isThirdPartyImport(String importName) {
        // Check against the dependency map
        return dependencyMap.values().stream()
            .anyMatch(dep -> importName.startsWith(dep.getGroupId()) ||
                            importName.startsWith(dep.getArtifactId()) ||
                            importName.contains(dep.getArtifactId())); // Checking class-level import
    }

    /**
     * Extracts method signatures from a given CompilationUnit (AST).
     * 
     * @param compilationUnit The AST of a Java file.
     */
    public void extractMethodSignatures(CompilationUnit compilationUnit) {
        if (dependencyMap != null) {
            compilationUnit.findAll(ImportDeclaration.class).forEach(importDecl -> {
                String importName = importDecl.getNameAsString();
                importSet.add(importName);
            });
        }

        compilationUnit.accept(new MethodVisitor(), null);
    }

    /**
     * Returns the set of extracted method signatures.
     * 
     * @return A set of method signatures.
     */
    public Set<String> getMethodSignatures() {
        return methodSignatures;
    }


    public Set<String> getThirdPartyImports() {
        return thirdPartyImports;
    }

    /**
     * Visitor class to visit each method call within the AST.
     */
    private class MethodVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);
            String methodName = methodCall.getNameAsString();
            String arguments = methodCall.getArguments().toString();

            // Determine the fully qualified name of the method call
            // This logic may vary depending on your project's structure and needs

            // Check if the method call matches any third-party dependency
            if (dependencyMap != null) {
                String qualifiedName = getQualifiedName(methodCall);
                System.out.println("qualifiedName: " + qualifiedName + "  arguments: " + arguments + "  methodName: " + methodCall.getNameAsString());
                if (isThirdPartyCall(qualifiedName)) {
                    String signature = qualifiedName + arguments;
                    methodSignatures.add(signature); // Add only third-party signatures
                }
            } else {
                String signature = methodName + arguments;
                methodSignatures.add(signature); // Add all signatures
            }

        }

        private String getQualifiedName(MethodCallExpr methodCall) {
            String methodName = methodCall.getNameAsString();
            String className = methodCall.getScope()
                    .map(scope -> scope.toString())
                    .orElse(null);

            if (className != null) {
                // Check if className is part of the importSet
                String finalClassName = className;
                String matchedImport = importSet.stream()
                        .filter(imp -> imp.endsWith(finalClassName))
                        .findFirst()
                        .orElse(null);

                if (matchedImport != null) {
                    return matchedImport + "." + methodName;
                }
            }
            return className != null ? className + "." + methodName : methodName;
        }

        private boolean isThirdPartyCall(String qualifiedName) {
            if (qualifiedName == null) {
                return false;
            }

            // Extract the base package from the qualified name
            String basePackage = qualifiedName.contains(".")
                    ? qualifiedName.substring(0, qualifiedName.lastIndexOf("."))
                    : qualifiedName;

            // Check against the dependency map
            return dependencyMap.values().stream()
                    .anyMatch(dep -> basePackage.startsWith(dep.getGroupId()) ||
                            basePackage.startsWith(dep.getArtifactId()));
        }
    }
}
