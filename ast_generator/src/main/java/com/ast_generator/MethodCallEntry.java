package com.ast_generator;

public class MethodCallEntry {
    private String declaringType;
    private String methodName;
    private int lineNumber; // Add line number field
    private String fullExpression;
    private MethodDeclarationInfo declarationInfo; // New field
    // Constructor and getters/setters
    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = lineNumber; 
        this.fullExpression = fullExpression;
        this.declarationInfo = null; // Initialize declarationInfo to null
    }

    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression, MethodDeclarationInfo declarationInfo) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = lineNumber; 
        this.fullExpression = fullExpression;
        this.declarationInfo = declarationInfo; // Initialize declarationInfo to null
    }

    // Add a method to set the declarationInfo
    public void setDeclarationInfo(MethodDeclarationInfo declarationInfo) {
        this.declarationInfo = declarationInfo;
    }

    // Getters
    public String getDeclaringType() { return declaringType; }
    public String getMethodName() { return methodName; }
    public int getLineNumber() { return lineNumber; }
    public String getFullExpression() { return fullExpression; }
}
