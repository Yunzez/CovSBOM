package com.ast_generator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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

    /**
     * 
     * @param mavenTree     - the output of the maven dependency:tree command
     * @param dependencyMap - the map to update with the dependencies
     * @param packageInfo   - the root package info for filtering dependencies
     */
    public static void updateDependencyMapWithTreeOutput(List<String> mavenTree, Map<String, Dependency> dependencyMap,
            Dependency packageInfo) {

        buildDependencyTree(mavenTree);
        for (String line : mavenTree) {
            System.out.println("line: " + line);

            if (line.contains(":jar:")) {
                String[] separatedLine = line.split(":");
                String artifactId = separatedLine[1].trim();
                String groupId = separatedLine[0].trim();
                String version = separatedLine[3].trim();

                groupId = groupId.split(" ")[groupId.split(" ").length - 1];

                if (groupId.startsWith(packageInfo.getGroupId())) {
                    continue;
                }

                String key = groupId + ":" + artifactId;

                // Correcting the mavenPathBase construction
                String mavenPathBase = System.getProperty("user.home") + "/.m2/repository/"
                        + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                        + "/" + artifactId + "-" + version;

                Dependency dependency = new Dependency(groupId, artifactId, version, mavenPathBase + ".jar",
                        mavenPathBase + "-sources.jar");
                dependencyMap.put(key, dependency);

                // System.out.println("parsed dependency: " + dependency.toString());
            }
        }
    }

    public static DependencyNode buildDependencyTree(List<String> mavenTreeLines) {
        List<DependencyNode> nodes = new ArrayList<>();
        Stack<DependencyNode> nodeStack = new Stack<>();
        DependencyNode root = null;

        for (String line : mavenTreeLines) {
            if (!line.contains(":jar:"))
                continue;

            int depth = getDepth(line); // Implement this method to determine the depth based on leading spaces or
                                        // dashes
            System.out.println("line: " + line + " depth: " + depth);

            String[] separatedLine = line.split(":");
            String artifactId = separatedLine[1].trim();
            String groupId = separatedLine[0].trim();
            String version = separatedLine[3].trim();

            groupId = groupId.split(" ")[groupId.split(" ").length - 1];

            String mavenPathBase = System.getProperty("user.home") + "/.m2/repository/"
                    + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                    + "/" + artifactId + "-" + version;

            Dependency dependency = new Dependency(groupId, artifactId, version, mavenPathBase + ".jar",
                    mavenPathBase + "-sources.jar");

            DependencyNode node = new DependencyNode(dependency);
            
            // Add node to the list of all nodes
            nodes.add(node);

            // Adjust stack based on the current node's depth
            System.out.println("nodeStack.size(): " + nodeStack.size() + " depth: " + depth);
            while (nodeStack.size() > depth) {
                nodeStack.pop();
            }

            if (nodeStack.isEmpty()) {
                // Root node
                root = node;
            } else {
                // Add the current node as a child of the node at the top of the stack
                nodeStack.peek().addChild(node);
            }

            nodeStack.push(node);
        }
       
        System.out.println("root: "+ root.toString());
        return root; // Return the root of the tree
    }

    private static int getDepth(String line) {
        int depth = 0;

        for (char c : line.toCharArray()) {
            if (c == '|' || c == '+' || c == '\\') {
                depth++;
            }
        }

        return depth; // Adjust this based on the indentation pattern of your Maven output
    }

}
