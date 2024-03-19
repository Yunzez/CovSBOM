package com.ast_generator;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
@JsonPropertyOrder({ "declaringType", "methodSignature", "methodName", "lineNumber", "fullExpression", "currentLayer", "declarationInfo" })
public class MethodCallEntry {
    private String declaringType;
    private String methodName;
    private ArrayList<String> lineNumber;
    private String fullExpression;
    private MethodDeclarationInfo declarationInfo; // New field
    private int currentLayer;
    private String methodSignature;

    @JsonIgnore
    private MethodSignatureKey methodSignatureKey;
    // Constructor and getters/setters
    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression, String methodSignature) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = new ArrayList<>(lineNumber); 
        this.lineNumber.add(Integer.toString(lineNumber));
        this.fullExpression = fullExpression;
        this.methodSignature = methodSignature;
        this.declarationInfo = null; // Initialize declarationInfo to null
        this.currentLayer = 0;
        this.methodSignatureKey = new MethodSignatureKey(declaringType, methodSignature);
    }

    public MethodCallEntry(String declaringType, String methodName, int lineNumber, String fullExpression,  String methodSignature, MethodDeclarationInfo declarationInfo) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.lineNumber = new ArrayList<>(); 
        this.lineNumber.add(Integer.toString(lineNumber)); // Convert lineNumber to String before adding to the list
        this.fullExpression = fullExpression;
        this.methodSignature = methodSignature;
        this.declarationInfo = declarationInfo; // Initialize declarationInfo to null
        this.currentLayer = 0;
        this.methodSignatureKey = new MethodSignatureKey(declaringType, methodSignature);
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
    }

   

    // Add a method to set the declarationInfo
    public void setDeclarationInfo(MethodDeclarationInfo declarationInfo) {
        this.declarationInfo = declarationInfo;
    }

    public void addLineNumber(int newLineNumber) {
        lineNumber.add(Integer.toString(newLineNumber));
    }

    // Getters
    public String getDeclaringType() { return declaringType; }
    public String getMethodName() { return methodName; }
    public ArrayList<String> getLineNumber() { return lineNumber; }
    public String getFullExpression() { return fullExpression; }
    public int getCurrentLayer() {
        return currentLayer;
    }
    public String getMethodSignature() {
        return methodSignature;
    }

    public MethodDeclarationInfo getDeclarationInfo() {
        return declarationInfo;
    }

    public String toString() {
        return declaringType + "." + methodName + "-" + methodSignature;
    }

    public MethodSignatureKey getMethodSignatureKey() {
        return this.methodSignatureKey;
    }
}

