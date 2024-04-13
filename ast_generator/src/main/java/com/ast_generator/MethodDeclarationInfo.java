package com.ast_generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
// @JsonPropertyOrder({ "declaringType", "methodSignature", "methodName", "lineNumber", "fullExpression", "currentLayer", "declarationInfo" })
public class MethodDeclarationInfo {

    private String sourceFilePath;
    private int declarationStartLine;
    private int declarationEndLine;
    private String methodName;
    private String declarationSignature;
    private List<MethodCallEntry> innerMethodCalls;

    public MethodDeclarationInfo(String sourceFilePath, int declarationStartLine, int declarationEndLine, String methodName, String declarationSignature) {
        this.declarationStartLine = declarationStartLine;
        this.declarationEndLine = declarationEndLine;
        this.innerMethodCalls = new ArrayList<>();
        this.methodName = methodName;
        this.sourceFilePath  = sourceFilePath;
        this.declarationSignature = declarationSignature;
    }

    public String getDeclarationSignature() {
        return declarationSignature;
    }
    // Method to add an inner method call
    public void addInnerMethodCall(MethodCallEntry methodCall) {
        innerMethodCalls.add(methodCall);
    }
    
    public void addInnerMethodCalls(List<MethodCallEntry> methodCalls) {
        innerMethodCalls.addAll(methodCalls);
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

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public void setDeclarationStartLine(int declarationStartLine) {
        this.declarationStartLine = declarationStartLine;
    }

    public void setDeclarationEndLine(int declarationEndLine) {
        this.declarationEndLine = declarationEndLine;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<MethodCallEntry> getInnerMethodCalls() {
        return innerMethodCalls;
    }

    public String toString() {
        return sourceFilePath + ":" + declarationStartLine + "-" + declarationEndLine + " " + methodName;
    }

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"sourceFilePath\": \"").append(sourceFilePath).append("\",\n");
        sb.append("\"declarationStartLine\": ").append(declarationStartLine).append(",\n");
        sb.append("\"declarationEndLine\": ").append(declarationEndLine).append(",\n");
        sb.append("\"methodName\": \"").append(methodName).append("\",\n");
        sb.append("\"declarationSignature\": \"").append(declarationSignature).append("\",\n");
        sb.append("\"innerMethodCalls\": ").append(innerMethodCalls).append(",\n");
        return sb.toString();
    }

    @JsonIgnore
    public Set<String> getAllDeclaringTypes() {
        Set<String> declaringTypes = new HashSet<>();
        for (MethodCallEntry entry : innerMethodCalls) {
            declaringTypes.add(entry.getDeclaringType());
        }
        return declaringTypes;
    }
}
