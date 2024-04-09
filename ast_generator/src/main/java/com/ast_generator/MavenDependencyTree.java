package com.ast_generator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyTree {

    /**
     * this method will run the maven dependency:tree command and parse the output
     * to get the dependencies and dependencies tree
     * 
     * @param projectDir    - the directory of the project to run the maven
     *                      dependency:tree command
     * @param packageInfo   - the root package info for filtering dependencies
     * @param dependencyMap - the map to update with the dependencies
     * @return
     */
    public static DependencyNode runMavenDependencyTree(String projectDir, Dependency packageInfo,
            Map<String, DependencyNode> dependencyMap) {
        System.out.println(System.getProperty("user.dir"));
        System.out.println("Running maven dependency:tree for " + projectDir);
        List<String> mavenOutput = new ArrayList<>();
        try {
            // String outputPath = Paths.get(projectDir, "mvn_dependency_tree.txt").toString();
            // ProcessBuilder processBuilder = new ProcessBuilder();
            // processBuilder.command("mvn", "-f", projectDir, "dependency:tree",
            //                        "-DoutputFile=" + outputPath, "-DappendOutput=true");
            // processBuilder.directory(new java.io.File(projectDir));
            // Process process = processBuilder.start();

            // int exitCode = process.waitFor();
            // System.out.println("\nExited with error code : " + exitCode);

            // // Read the output from the file
            // Path outputFilePath = Paths.get(outputPath);
            // mavenOutput = Files.readAllLines(outputFilePath);

            // Optionally, delete the file after reading
            // Files.delete(outputFilePath);

            Path projectPath = Paths.get(projectDir).toAbsolutePath();
            // Define the output file path
            Path outputPath = projectPath.resolve("mvn_dependency_tree.txt");
            
            // Execute the Maven command
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("mvn", "-f", projectPath.toString(), "dependency:tree", "-DoutputFile=" + outputPath.toString(), "-DappendOutput=true");
            processBuilder.directory(new java.io.File(projectDir));
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
    
            // Read the output from the file
            mavenOutput = Files.readAllLines(outputPath);
    
            // delete the file after reading
            Files.delete(outputPath);
    
            DependencyNode rootNode = updateDependencyMapWithTreeOutput(mavenOutput, dependencyMap, packageInfo);
            return rootNode;
    

        } catch (Exception e) {
            e.printStackTrace();
        }

        DependencyNode rootNode = updateDependencyMapWithTreeOutput(mavenOutput, dependencyMap, packageInfo);
        return rootNode;
    }


    /**
     * 
     * @param mavenTree     - the output of the maven dependency:tree command
     * @param dependencyMap - the map to update with the dependencies
     * @param packageInfo   - the root package info for filtering dependencies
     */
    public static DependencyNode updateDependencyMapWithTreeOutput(List<String> mavenTree,
            Map<String, DependencyNode> dependencyMap,
            Dependency packageInfo) {

        DependencyNode rootNode = buildDependencyTree(mavenTree);
        List<DependencyNode> mainDependencies = rootNode.getChildren();
        for (DependencyNode dependencyNode : mainDependencies) {
            // System.out.println("dependency: " + dependencyNode.toString());

            String key = dependencyNode.getGroupId() + ":" + dependencyNode.getArtifactId();

            dependencyMap.put(key, dependencyNode);
        }

        return rootNode;
    }

    /**
     * Build a dependency tree from the output of the maven dependency:tree command
     * 
     * @param mavenTreeLines - the output of the maven dependency:tree command
     * @return
     */
    public static DependencyNode buildDependencyTree(List<String> mavenTreeLines) {
        System.out.println("mavenTreeLines: " + mavenTreeLines.toString());
        List<DependencyNode> nodes = new ArrayList<>();
        Stack<DependencyNode> nodeStack = new Stack<>();
        int start = 0;
        DependencyNode root = null;
        for (int i = 0; i< mavenTreeLines.size(); i++) {
            String line = mavenTreeLines.get(i);
            if (line.contains("+-")) {
                System.out.println("start: " + i + " line: " + line);
                start = i;
                break;
            }
        }

        int count = 0;
        for (String line : mavenTreeLines) {
            count ++;
            System.out.println("count: " + count + "start:" + start + " line: " + line);
            
            if (count == start) {
                Dependency dependency = getDependencyFromLine(line);
                DependencyNode node = new DependencyNode(dependency);
                root = node;
                nodeStack.push(node);
            }

            if (!line.contains(":jar:"))
                continue;
            
            int depth = getDepth(line); // Implement this method to determine the depth based on leading spaces or
                                        // dashes
            Dependency dependency = getDependencyFromLine(line);

            DependencyNode node = new DependencyNode(dependency);

            // Add node to the list of all nodes
            nodes.add(node);

            // Adjust stack based on the current node's depth
            System.out.println("nodeStack.size(): " + nodeStack.size() + " depth: " + depth);
            while (nodeStack.size() > depth) {
                nodeStack.pop();
            }

            if (nodeStack.isEmpty()) {
                root = node;
            } else {
                // Add the current node as a child of the node at the top of the stack
                nodeStack.peek().addChild(node);
            }

            nodeStack.push(node);
        }

        System.out.println("root: ");
        System.out.println( root.toConsoleString());
        return root; // Return the root of the tree
    }

    private static int getDepth(String line) {
        int leadingSpaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '|' || c == '+' || c == '-' || c == '\\' || c == '/') {
                leadingSpaces++;
            } else {
                break; // Stop counting spaces once you encounter a non-space character
            }
        }
        // Assuming 3 spaces represent one level of indentation
        int depth = leadingSpaces / 3;
        return depth;
    }
    

    private static Dependency getDependencyFromLine(String line) {
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

        return dependency;
    }

}
