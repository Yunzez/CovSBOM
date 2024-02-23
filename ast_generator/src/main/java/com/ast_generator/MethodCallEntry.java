package com.ast_generator;

public class MethodCallEntry {
    private String declaringType;
    private String methodName;
    private int lineNumber; // Add line number field
    // Constructor and getters/setters
    public MethodCallEntry(String declaringType, String methodName, int lineNumber) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = lineNumber; 
    }

    // Getters
    public String getDeclaringType() { return declaringType; }
    public String getMethodName() { return methodName; }
    public int getLineNumber() { return lineNumber; }
}
