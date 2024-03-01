package com.ast_generator;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclarationInfo {

    private String sourceFilePath;
    private int declarationStartLine;
    private int declarationEndLine;
    private String methodName;
    private List<MethodCallEntry> innerMethodCalls;

    public MethodDeclarationInfo(String sourceFilePath, int declarationStartLine, int declarationEndLine, String methodName) {
        this.declarationStartLine = declarationStartLine;
        this.declarationEndLine = declarationEndLine;
        this.innerMethodCalls = new ArrayList<>();
        this.methodName = methodName;
        this.sourceFilePath  = sourceFilePath;
    }

    // Method to add an inner method call
    public void addInnerMethodCall(MethodCallEntry methodCall) {
        innerMethodCalls.add(methodCall);
    }

    // Getters
    public int getDeclarationStartLine() {
        return declarationStartLine;
    }

    public int getDeclarationEndLine() {
        return declarationEndLine;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public List<MethodCallEntry> getInnerMethodCalls() {
        return innerMethodCalls;
    }

    public String toString() {
        String ret = "MethodDeclarationInfo: " + methodName + " " + declarationStartLine + " " + declarationEndLine;
        return ret;
    }
}
