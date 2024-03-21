package com.ast_generator;

import java.util.Objects;


/*
 * this is a class to represent a key for a unique method usage with declaring type and signature
 */
public class MethodSignatureKey {
    private final String declaringType;
    private final String methodSignature;

    public MethodSignatureKey(String declaringType, String methodSignature) {
        this.declaringType = declaringType;
        this.methodSignature = methodSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignatureKey that = (MethodSignatureKey) o;
        return Objects.equals(declaringType, that.declaringType) &&
                Objects.equals(methodSignature, that.methodSignature);
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

