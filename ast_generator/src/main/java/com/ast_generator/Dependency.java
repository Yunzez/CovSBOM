package com.ast_generator;

public class Dependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String jarPath;
    private String sourceJarPath;
    private String sourceDecompressedPath;

    // Constructor, getters, and setters
    public Dependency(String groupId, String artifactId, String version, String jarPath, String sourceJarPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarPath = jarPath;
        this.sourceJarPath = sourceJarPath;
        this.sourceDecompressedPath = null;
    }

    public String getBasePackageName() {
        // Convert groupId and artifactId to a base package name
        // Typically, groupId and artifactId are used in Maven to denote the base package structure.
        return groupId + "." + artifactId;
    }

    public void setSourceDecompressedPath(String sourceDecompressedPath) {
        this.sourceDecompressedPath = sourceDecompressedPath;
    }

    public String getSourceDecompressedPath() {
        return sourceDecompressedPath;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getSourceJarPath() {
        return sourceJarPath;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String toString() {
        return "Dependency [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", jarPath="
                + jarPath + "]";
    }

}
