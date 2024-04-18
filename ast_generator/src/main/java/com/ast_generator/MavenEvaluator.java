package com.ast_generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class MavenEvaluator {
    private static String runMavenCommand(String projectPath, String mavenProperty) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("mvn", "help:evaluate", "-Dexpression=" + mavenProperty, "-q", "-DforceStdout");
        builder.directory(Paths.get(projectPath).toFile());
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            process.waitFor();
            return output;
        }
    }

    public static String[] evaluateMavenPath(String projectPath) throws IOException, InterruptedException {
        String groupId = runMavenCommand(projectPath, "project.groupId");
        String artifactId = runMavenCommand(projectPath, "project.artifactId");
        String version = runMavenCommand(projectPath, "project.version");
        return new String[] {groupId, artifactId, version};
    }  

    public static DependencyNode evaluateCurrentProject(String projectPath) throws IOException, InterruptedException {
        String[] mavenPath = evaluateMavenPath(projectPath);
        return new DependencyNode(mavenPath[0], mavenPath[1], mavenPath[2], "", "");
    }
}