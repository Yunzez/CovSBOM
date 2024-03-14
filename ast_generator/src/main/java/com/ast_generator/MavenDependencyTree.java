package com.ast_generator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyTree {

    public static List<String> runMavenDependencyTree(String projectDir) {
        System.out.println(System.getProperty("user.dir"));
        System.out.println("Running maven dependency:tree " + projectDir);
        List<String> dependencies = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("mvn", "-f", System.getProperty("user.dir") + "/" + projectDir, "dependency:tree");
            processBuilder.directory(new java.io.File(projectDir));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                dependencies.add(line);
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependencies;
    }

    public static void updateDependencyMapWithTreeOutput(List<String> mavenTree, Map<String, Dependency> dependencyMap) {
        for (String line : mavenTree) {
            System.out.println("line: " + line);
    
            if (line.contains(":jar:")) {
                String[] separatedLine = line.split(":");
                String artifactId = separatedLine[1].trim();
                String groupId = separatedLine[0].trim();
                String version = separatedLine[3].trim();
    
                groupId = groupId.split(" ")[groupId.split(" ").length - 1];
    
                String key = groupId + ":" + artifactId;
    
                // Correcting the mavenPathBase construction
                String mavenPathBase = System.getProperty("user.home") + "/.m2/repository/"
                        + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                        + "/" + artifactId + "-" + version;
    
                Dependency dependency = new Dependency(groupId, artifactId, version, mavenPathBase + ".jar", mavenPathBase + "-sources.jar");
                dependencyMap.put(key, dependency);
    
                System.out.println("parsed dependency: " + dependency.toString());
            }
        }
    }
    
}
