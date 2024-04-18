package com.ast_generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MethodCallBuffer {
    private int size;
    private Map<DependencyNode, HashSet<MethodCallEntry>> buffer = new HashMap<DependencyNode, HashSet<MethodCallEntry>>();
    private DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver;

    /**
     * Constructor
     * 
     * @param dependencies                      the list of dependencies
     * @param declaringTypeToDependencyResolver the declaring type to dependency
     *                                          resolver, this help the call buffer
     *                                          to locate function calls
     */
    public MethodCallBuffer(List<DependencyNode> dependencies,
            DeclaringTypeToDependencyResolver declaringTypeToDependencyResolver) {
        this.declaringTypeToDependencyResolver = declaringTypeToDependencyResolver;
        for (DependencyNode dependency : dependencies) {
            buffer.put(dependency, new HashSet<MethodCallEntry>());
        }
        this.size = 0;
    }

    /**
     * Add a method call to the buffer, it will be added to the buffer of the
     * dependency that the method call is related to
     * 
     * @param methodSignatureKey the method signature key of the method call
     */
    public void addMethodCall(MethodCallEntry method) {
        String declaringType = method.getDeclaringType();
        DependencyNode matchDependencyNode = declaringTypeToDependencyResolver
                .getDependencyForDeclaringType(declaringType);

        if (matchDependencyNode == null) {
            // System.out.println("no match in " + " for " + method.toString() + " type: " + declaringType);
            return;
        }
        if (!buffer.get(matchDependencyNode).contains(method)) {
            buffer.get(matchDependencyNode).add(method);
            this.size++;
        }
    }

    /**
     * Get the buffer of a dependency if it exists
     * 
     * @param methodSignatureKey the method signature key of the method call
     */
    public void removeMethodCall(MethodCallEntry method) {
        String declaringType = method.getDeclaringType();
        DependencyNode matchDependencyNode = declaringTypeToDependencyResolver
                .getDependencyForDeclaringType(declaringType);
        if (buffer.get(matchDependencyNode).contains(method)) {
            buffer.get(matchDependencyNode).remove(method);
            size--;
        }

    }

    /**
     * Get the buffer of a dependency if it exists
     * 
     * @param methodSignatureKey the method signature key of the method call
     * @return boolean
     */
    public boolean hasMethodCall(MethodCallEntry method) {

        String declaringType = method.getDeclaringType();
        DependencyNode matchDependencyNode = declaringTypeToDependencyResolver
                .getDependencyForDeclaringType(declaringType);


        if (matchDependencyNode == null) {
            // System.out.println("no match in " + " for " + method.toString());
            return false;
        }
        if (buffer.get(matchDependencyNode) == null) {
            System.out.println("null buffer in " + matchDependencyNode.toShortString() + " for " + declaringType);
            return false;
        }
        if (buffer.get(matchDependencyNode).contains(method)) {
            return true;
        }
        return false;
    }

    /**
     * Get the buffer of a dependency if it exists
     * 
     * @param dependency         the dependency node
     * @param methodSignatureKey the method signature key of the method call
     * @return boolean
     */
    public boolean hasMethodCall(DependencyNode dependency, MethodCallEntry method) {

        return buffer.get(dependency).contains(method);

    }

    /**
     * if the buffer is empty
     * 
     * @return boolean
     */
    public boolean isEmpty() {
        for (DependencyNode dependency : buffer.keySet()) {
            if (buffer.get(dependency).size() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the method calls of a specific dependency
     * 
     * @param dependency the dependency node
     * @return
     */
    public Set<MethodCallEntry> getMethodCalls(DependencyNode dependency) {
        return buffer.get(dependency);
    }

    /**
     * Clear the method calls of a specific dependency
     * 
     * @param dependency the dependency node
     */
    public void clearMethodCalls(DependencyNode dependency) {
        buffer.get(dependency).clear();
    }

    /**
     * Get the size of the buffer
     * 
     * @return int
     */
    public int size() {
        return this.size;
    }

    /**
     * Get the size of the buffer of a specific dependency
     * 
     * @param dependency the dependency node
     * @return int
     */
    public int size(DependencyNode dependency) {
        return buffer.get(dependency).size();
    }

    public Map<DependencyNode, HashSet<MethodCallEntry>> getBuffer() {
        return buffer;
    }

    public void setBuffer(Map<DependencyNode, HashSet<MethodCallEntry>> buffer) {
        this.buffer = buffer;
    }

    public void clear() {
        for (DependencyNode dependency : buffer.keySet()) {
            buffer.get(dependency).clear();
        }
        this.size = 0;
    }

    public void clear(DependencyNode dependency) {
        buffer.get(dependency).clear();
        this.size -= buffer.get(dependency).size();
    }

    public Set<DependencyNode> getKeys() {
        return buffer.keySet();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DependencyNode dependency : buffer.keySet()) {
            builder.append(dependency.getArtifactId() + " : " + buffer.get(dependency).toString() + "\n");
        }
        return builder.toString();
    }

}
