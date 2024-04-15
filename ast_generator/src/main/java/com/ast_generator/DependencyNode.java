package com.ast_generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DependencyNode extends Dependency{
    private String groupId;
    private String artifactId;
    private String version;
    private String jarPath;
    private String sourceJarPath;
    private Set<DependencyNode> children = new HashSet<>();

    // * isValid is used to mark if the dependency has source jar
    private Boolean isValid = true;

    public DependencyNode(String groupId, String artifactId, String version, String jarPath, String sourceJarPath) {
        super(groupId, artifactId, version, jarPath, sourceJarPath);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarPath = jarPath;
        this.sourceJarPath = sourceJarPath;
    }

    public DependencyNode(Dependency dependency) {
        super(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getJarPath(), dependency.getSourceJarPath());
        this.artifactId = dependency.getArtifactId();
        this.groupId = dependency.getGroupId();
        this.version = dependency.getVersion();
        this.jarPath = dependency.getJarPath();
        this.sourceJarPath = dependency.getSourceJarPath();
    }

    public void addChild(DependencyNode child) {
        children.add(child);
    }

    public Set<DependencyNode> getChildren() {
        return children;
    }

    public List<DependencyNode> toList() {
        List<DependencyNode> list = new ArrayList<>();
        list.add(this);
        for (DependencyNode child : children) {
            list.addAll(child.toList());
        }
        return list;
    }

    public void setChildren(Set<DependencyNode> children) {
        this.children = children;
    }

    public void setIsValid (Boolean valid) {
        this.isValid = valid;
    }

    public Boolean getIsValid() {
        return isValid;
    }


    public String toConsoleString() {
        return buildString("", true);
    }

    // ! credits: chatgpt
    private String buildString(String prefix, boolean isTail) {
        StringBuilder builder = new StringBuilder();
        
        builder.append(prefix)
               .append(isTail ? "└── " : "├── ")
               .append(groupId)
               .append(":")
               .append(artifactId)
               .append(":")
               .append(version);

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

