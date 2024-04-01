package com.ast_generator;

import java.util.ArrayList;
import java.util.List;

public class DependencyNode {
    private Dependency dependency;
    private List<DependencyNode> children = new ArrayList<>();

    public DependencyNode(Dependency dependency) {
        this.dependency = dependency;
    }

    public DependencyNode(Dependency dependency, List<DependencyNode> children) {
        this.dependency = dependency;
        this.children = children;
    }

    // Constructor
    public DependencyNode(String groupId, String artifactId, String version, String jarPath, String sourceJarPath) {
      this.dependency = new Dependency(groupId, artifactId, version, jarPath, sourceJarPath);
    }

    // Getters and setters
    public void addChild(DependencyNode child) {
        children.add(child);
    }

    public Dependency getDependency() {
        return dependency;
    }

    public List<DependencyNode> getChildren() {
        return children;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    public void setChildren(List<DependencyNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return buildString("", true);
    }

    // ! credits: chatgpt
    private String buildString(String prefix, boolean isTail) {
        StringBuilder builder = new StringBuilder();
        
        builder.append(prefix)
               .append(isTail ? "└── " : "├── ")
               .append(dependency.getGroupId())
               .append(":")
               .append(dependency.getArtifactId())
               .append(":")
               .append(dependency.getVersion());

        int i = 0;
        for (DependencyNode child : children) {
            if (i < children.size() - 1) {
                builder.append("\n")
                       .append(child.buildString(prefix + (isTail ? "    " : "│   "), false));
            } else {
                builder.append("\n")
                       .append(child.buildString(prefix + (isTail ? "    " : "│   "), true));
            }
            i++;
        }

        return builder.toString();
    }
}

