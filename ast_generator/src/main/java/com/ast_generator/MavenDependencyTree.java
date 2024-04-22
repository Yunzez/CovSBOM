package com.ast_generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import com.ast_generator.Utils.DependencyCollector;

public class MavenDependencyTree {
    public static Set<Dependency> noJarDependency = new HashSet<>();
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
            Map<String, DependencyNode> dependencyMap, Map<String, String> moduleList) {

        final boolean skipDependencyTree = false;

        System.out.println(System.getProperty("user.dir"));
        System.out.println("Running maven dependency:tree for " + projectDir);
        Set<String> resolvedClassPath = runAndReadMavenClasspath(projectDir);
        List<String> mavenOutput = new ArrayList<>();
        try {
            Path projectPath = Paths.get(projectDir).toAbsolutePath();
            // Define the output file path
            Path outputPath = projectPath.resolve("mvn_dependency_tree.txt");

            // Execute the Maven command
            System.out.println(moduleList.size());
            if (moduleList != null && moduleList.size() > 0) {
                if (skipDependencyTree) {
                    System.out.println("skip dependency tree");
                } else {
                    System.out.println("run mvn dependency:tree for multi-module project");
                    String basePath = System.getProperty("user.dir");
                    String scriptName = "mavenDependencyTreeRunner.sh";
                    // Assuming the script is now located in src/main/java/com/ast_generator/

                    // ! warning, this is temporary, we need to find a better way to locate the
                    // script
                    String scriptRelativePath = "ast_generator/src/main/java/com/ast_generator/" + scriptName;

                    String scriptPath = Paths.get(basePath, scriptRelativePath).toString();
                    String bashCommand = "bash " + scriptPath + " " + projectPath.toString() + " "
                            + outputPath.toString();
                    System.out.println("bashCommand: " + bashCommand);
                    for (String modulePath : moduleList.values()) {
                        // If modulePath is absolute, convert it to relative by subtracting projectDir
                        // part
                        Path absoluteModulePath = Paths.get(modulePath).toAbsolutePath();
                        Path relativeModulePath = Paths.get(projectDir).toAbsolutePath().relativize(absoluteModulePath);
                        bashCommand += " " + relativeModulePath;
                    }

                    // Execute the command
                    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", bashCommand);
                    processBuilder.directory(Paths.get(projectDir).toFile()); // Ensure we're in the correct directory
                    Process process = processBuilder.start();

                    System.out.println("the project is large, please wait for the process to finish");
                    // Output the process's output
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                    }

                    // Reading standard error
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.err.println(line); // Print standard error
                        }
                    }

                    int exitCode = process.waitFor();
                    System.out.println("Script exited with error code : " + exitCode);
                }
                mavenOutput = Files.readAllLines(outputPath);
            } else {
                System.out.println("run mvn dependency:tree for single module project");
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("mvn", "-f", projectPath.toString(), "dependency:tree",
                        "-DoutputFile=" + outputPath.toString(), "-DappendOutput=true");
                processBuilder.directory(new java.io.File(projectDir));
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                System.out.println("\nExited with error code : " + exitCode);

                // Read the output from the file
                mavenOutput = Files.readAllLines(outputPath);

                Files.delete(outputPath);

            }

            // delete the file after reading

            // DependencyNode rootNode = updateDependencyMapWithTreeOutput(mavenOutput,
            // dependencyMap, resolvedClassPath,
            // packageInfo,
            // moduleList != null && moduleList.size() > 0);
            // return rootNode;

        } catch (Exception e) {
            e.printStackTrace();
        }

        DependencyNode rootNode = updateDependencyMapWithTreeOutput(mavenOutput, dependencyMap, resolvedClassPath,
                packageInfo, moduleList != null && moduleList.size() > 0);
        System.out.println(resolvedClassPath);
        return rootNode;

    }


    public static Set<String> runAndReadMavenClasspath(String projectDir) {
        Set<String> classpaths = new HashSet<>();
        Path projectPath = Paths.get(projectDir).toAbsolutePath();
        String outputPath = "classPath.txt";

        try {
            // Execute Maven command to generate classpath files
            ProcessBuilder builder = new ProcessBuilder("mvn", "dependency:build-classpath", "-Dmdep.outputFile=" + outputPath);
            builder.directory(projectPath.toFile());  // Set the working directory
            builder.redirectErrorStream(true);  // Redirect errors to standard output
            Process process = builder.start();

            int exitCode = process.waitFor();
            System.out.println("Maven exited with code " + exitCode);

            // Read classPath.txt from all relevant directories
            Files.walk(projectPath)
                .filter(dir -> isMavenProject(dir))
                .forEach(path -> {
                    Path classpathFile = path.resolve(outputPath);
                    if (Files.exists(classpathFile)) {
                        try {
                            Files.readAllLines(classpathFile).forEach(line -> {
                                String[] elements = line.split(":");
                                for (String element : elements) {
                                    classpaths.add(element);
                                }
                            });
                        } catch (IOException e) {
                            System.err.println("Failed to read classpath file: " + classpathFile);
                            e.printStackTrace();
                        }
                    }
                });
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return classpaths;
    }

    private static boolean isMavenProject(Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve("pom.xml"));
    }



    /**
     * 
     * @param mavenTree            - the output of the maven dependency:tree command
     * @param dependencyMap        - the map to update with the dependencies
     * @param resolvedClassPath    - the output of maven dependency:build-classpath
     * @param packageInfo          - the root package info for filtering
     *                             dependencies
     * @param isMultiModuleProject - whether the project is a multi-module project
     * @return
     */
    public static DependencyNode updateDependencyMapWithTreeOutput(List<String> mavenTree,
            Map<String, DependencyNode> dependencyMap,
            Set<String> resolvedClassPath,
            Dependency packageInfo,
            Boolean isMultiModuleProject) {

        DependencyNode rootNode;
        if (isMultiModuleProject) {
            System.out.println("multi-module project, use simple parse");
            rootNode = simpleBuildTree(mavenTree, packageInfo, resolvedClassPath);
        } else {
            rootNode = buildDependencyTree(mavenTree, resolvedClassPath);
        }
        System.out.println();
        Set<DependencyNode> mainDependencies = rootNode.getChildren();

        for (DependencyNode dependencyNode : mainDependencies) {
            // System.out.println("dependency: " + dependencyNode.toString());

            String key = dependencyNode.getGroupId() + ":" + dependencyNode.getArtifactId();

            dependencyMap.put(key, dependencyNode);
        }

        return rootNode;
    }

    /**
     * @param packageInfo - the root package info for filtering dependencies
     */
    public static DependencyNode simpleBuildTree(List<String> mavenTreeLines, Dependency packageInfo,
            Set<String> resolvedClassPath) {
        Dependency rootDependency = packageInfo;
        DependencyNode rootNode = new DependencyNode(rootDependency);
        Set<Dependency> dependencies = new HashSet<>(); // remove duplicates
        Set<String> uniqueDependencyStrings = new HashSet<>();
        uniqueDependencyStrings.addAll(mavenTreeLines);
        for (String line : uniqueDependencyStrings) {
            // Assuming each dependency line can be identified and parsed
            if (line.contains(":jar:")) {
                Dependency dependency = getDependencyFromLine(line, resolvedClassPath);
                if (dependency == null) {
                    continue;
                }
                if (!dependency.getGroupId().equalsIgnoreCase(packageInfo.getGroupId())) {
                    dependencies.add(dependency);
                } else {
                    // we don't need to add the sub package to the dependencies
                    noJarDependency.remove(dependency);
                }
               
            }
        }

        for (Dependency dependency : dependencies) {
            DependencyNode node = new DependencyNode(dependency);
                rootNode.addChild(node);
        }

        System.out.println("root: ");
        System.out.println(rootNode.toConsoleString());
        System.out.println(packageInfo.toString());
        return rootNode;
    }

    /**
     * Build a dependency tree from the output of the maven dependency:tree command
     * 
     * @param mavenTreeLines - the output of the maven dependency:tree command
     * @return
     */
    public static DependencyNode buildDependencyTree(List<String> mavenTreeLines, Set<String> resolvedClassPath) {
        // System.out.println("mavenTreeLines: " + mavenTreeLines.toString());
        List<DependencyNode> nodes = new ArrayList<>();
        Stack<DependencyNode> nodeStack = new Stack<>();
        int start = 0;
        DependencyNode root = null;
        for (int i = 0; i < mavenTreeLines.size(); i++) {
            String line = mavenTreeLines.get(i);
            if (line.contains("+-")) {
                // System.out.println("start: " + i + " line: " + line);
                start = i;
                break;
            }
        }

        int count = 0;
        for (String line : mavenTreeLines) {
            count++;
            // System.out.println("count: " + count + "start:" + start + " line: " + line);

            if (count == start) {
                Dependency dependency = getDependencyFromLine(line, resolvedClassPath);
                if (dependency == null) {
                    continue;
                }
                DependencyNode node = new DependencyNode(dependency);
                root = node;
                nodeStack.push(node);
            }

            if (!line.contains(":jar:"))
                continue;

            int depth = getDepth(line); // Implement this method to determine the depth based on leading spaces or
                                        // dashes
            Dependency dependency = getDependencyFromLine(line, resolvedClassPath);

            if (dependency == null) {
                continue;
            }

            DependencyNode node = new DependencyNode(dependency);

            // Add node to the list of all nodes
            nodes.add(node);

            // Adjust stack based on the current node's depth
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
        System.out.println(root.toConsoleString());
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

    private static Dependency getDependencyFromLine(String line, Set<String> resolvedClassPath) {
        String[] separatedLine = line.split(":");
        String artifactId = separatedLine[1].trim();
        String groupId = separatedLine[0].trim();
        String version = separatedLine[3].trim();

        groupId = groupId.split(" ")[groupId.split(" ").length - 1];

        // we first initialize dependency with no jar path or sourcejar path
        Dependency dependency = new Dependency(groupId, artifactId, version, "",
                "");
        matchClasspathToDependency(dependency, resolvedClassPath);

        File jarFile = new File(dependency.getJarPath());
        File sourceJarFile = new File(dependency.getSourceJarPath());
        if (!jarFile.exists() || !sourceJarFile.exists()) {
            System.out.println("dependency jar not found: " + dependency.toString());
            noJarDependency.add(dependency);
        }
        // if (node.getGroupId().equalsIgnoreCase(packageInfo.getGroupId()))
        
        return dependency;
    }

    public static void matchClasspathToDependency(Dependency dependency, Set<String> classPaths) {
        for (String classPath : classPaths) {
            String[] parts = classPath.split("/");
            boolean groupIdFound = false, artifactIdFound = false, versionFound = false;

            // Check each part of the path to see if it contains the artifactId or version
            // directly
            for (String part : parts) {
                if (part.equals(dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar")) {
                    artifactIdFound = true;
                    versionFound = true;
                }

                // Check if the groupId is included anywhere in the path
                if (classPath.contains(dependency.getGroupId().replace(".", "/"))) {
                    groupIdFound = true;
                }
            }

            // Only consider it a match if both artifactId and version are found directly,
            // and groupId is included anywhere
            if (artifactIdFound && versionFound && groupIdFound) {
                System.out.println("Match found in path: " + classPath);
                dependency.setJarPath(classPath);
                dependency.setSourceJarPath(classPath.replace(".jar", "-sources.jar"));
                break; // Match found, no need to continue
            }
        }

        if (dependency.getJarPath() == null || dependency.getJarPath().isEmpty()) {
            System.out.println("No matching classpath found for dependency: " + dependency);
        }
    }

    public static String totNoJarDependencyString() {
        StringBuilder sb = new StringBuilder();
        for (Dependency dependency : noJarDependency) {
            sb.append(dependency.toString());
            sb.append("\n");
        }
        return sb + "";
    }

}
