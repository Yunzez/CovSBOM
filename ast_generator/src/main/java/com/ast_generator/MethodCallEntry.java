package com.ast_generator;
import java.util.ArrayList;
public class MethodCallEntry {
    private String declaringType;
    private String methodName;
    private ArrayList<String> lineNumber;
    private String fullExpression;
    private String methodSignature;
    private MethodDeclarationInfo declarationInfo; // New field
    // Constructor and getters/setters
    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression, String methodSignature) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = new ArrayList<>(lineNumber); 
        this.lineNumber.add(Integer.toString(lineNumber));
        this.fullExpression = fullExpression;
        this.methodSignature = methodSignature;
        this.declarationInfo = null; // Initialize declarationInfo to null
    }

    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression,  String methodSignature, MethodDeclarationInfo declarationInfo) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = new ArrayList<>(); 
        this.lineNumber.add(Integer.toString(lineNumber)); // Convert lineNumber to String before adding to the list
        this.fullExpression = fullExpression;
        this.methodSignature = methodSignature;
        this.declarationInfo = declarationInfo; // Initialize declarationInfo to null
    }

    // Add a method to set the declarationInfo
    public void setDeclarationInfo(MethodDeclarationInfo declarationInfo) {
        this.declarationInfo = declarationInfo;
    }

    public void addLineNumber(int newLineNumber) {
        lineNumber.add(Integer.toString(newLineNumber));
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public MethodDeclarationInfo getDeclarationInfo() {
        return declarationInfo;
    }

    // Getters
    public String getDeclaringType() { return declaringType; }
    public String getMethodName() { return methodName; }
    public ArrayList<String> getLineNumber() { return lineNumber; }
    public String getFullExpression() { return fullExpression; }

    public String toString() {
        return declaringType + "." + methodName;
    }
}

