package com.ast_generator;

import java.util.Objects;


/*
 * this is a class to represent a key for a unique method usage with declaring type and signature
 */
public class MethodSignatureKey {
    private final String declaringType;
    private final String methodSignature;
    // private final String fullExpression;

    public MethodSignatureKey(String declaringType, String methodSignature) {
        this.declaringType = declaringType;
        this.methodSignature = methodSignature;
        // this.fullExpression = null;
    }

    // public MethodSignatureKey(String declaringType, String methodSignature, String fullExpression) {
    //     this.declaringType = declaringType;
    //     this.methodSignature = methodSignature;
    //     this.fullExpression = fullExpression;
    // }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignatureKey that = (MethodSignatureKey) o;

        return Objects.equals(declaringType, that.declaringType) &&
                Objects.equals(methodSignature, that.methodSignature);
                // && Objects.equals(fullExpression, that.fullExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringType, methodSignature);
    }

    public String getDeclaringType() {
        return declaringType;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String toString() {
        return declaringType + ":" + methodSignature;
    }
}

