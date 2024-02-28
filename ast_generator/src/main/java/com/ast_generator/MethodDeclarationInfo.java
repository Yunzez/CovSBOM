package com.ast_generator;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclarationInfo {


    private int declarationStartLine;
    private int declarationEndLine;
    private String methodName;
    private List<MethodCallEntry> innerMethodCalls;

    public MethodDeclarationInfo(int declarationStartLine, int declarationEndLine, String methodName) {
        this.declarationStartLine = declarationStartLine;
        this.declarationEndLine = declarationEndLine;
        this.innerMethodCalls = new ArrayList<>();
        this.methodName = methodName;
    }

    // Method to add an inner method call
    public void addInnerMethodCall(MethodCallEntry methodCall) {
        innerMethodCalls.add(methodCall);
    }

    // Getters
    public int getDeclarationStartLine() { return declarationStartLine; }
    public int getDeclarationEndLine() { return declarationEndLine; }
    public List<MethodCallEntry> getInnerMethodCalls() { return innerMethodCalls; }
    public void toStrings() {
        System.out.println("MethodDeclarationInfo: " + methodName + " start: " + declarationStartLine + " end: " + declarationEndLine);
        for (MethodCallEntry methodCall : innerMethodCalls) {
            System.out.println("MethodCallEntry: " + methodCall.getMethodName() + " line: " + methodCall.getLineNumber());
        }
    }
}
