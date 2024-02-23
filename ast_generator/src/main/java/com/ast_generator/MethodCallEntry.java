package com.ast_generator;

public class MethodCallEntry {
    private String declaringType;
    private String methodName;

    // Constructor and getters/setters
    public MethodCallEntry(String declaringType, String methodName) {
        this.declaringType = declaringType;
        this.methodName = methodName;
    }

    // Getters
    public String getDeclaringType() { return declaringType; }
    public String getMethodName() { return methodName; }
}
