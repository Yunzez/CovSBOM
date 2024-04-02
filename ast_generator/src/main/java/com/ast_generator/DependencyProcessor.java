package com.ast_generator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ! # DependencyProcessor
 * * The `DependencyProcessor` class is part of the `com.ast_generator`
 * package. It is responsible for processing dependencies in a Maven project,
 * generating Abstract Syntax Trees (ASTs) for each dependency, and serializing
 * these ASTs into JSON format.
 */

public class DependencyProcessor {
    private static Dependency packageInfo;
    private static String javaVersion;

    public DependencyProcessor() {
    }

    /*
     * This method parses the `pom.xml` file for a Maven project and returns a
     * `Map` of `Dependency` objects. Each `Dependency` object represents a
     * dependency in the Maven project.
     */
    public static Map<String, Dependency> parsePomForDependencies(String pomFilePath) {
        Map<String, Dependency> dependencyMap = new HashMap<>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(pomFilePath));
            doc.getDocumentElement().normalize();

            NodeList dependenciesList = doc.getElementsByTagName("dependency");

            NodeList propertiesList = doc.getElementsByTagName("properties");
            Map<String, String> versionMap = new HashMap<>();
            if (propertiesList != null && propertiesList.getLength() > 0) {
                for (int i = 0; i < propertiesList.getLength(); i++) {
                    Node propertiesNode = propertiesList.item(i);
                    if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element propertiesElement = (Element) propertiesNode;
                        NodeList propertyNodes = propertiesElement.getChildNodes();
                        for (int j = 0; j < propertyNodes.getLength(); j++) {
                            Node propertyNode = propertyNodes.item(j);
                            if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element propertyElement = (Element) propertyNode;
                                String propertyName = propertyElement.getTagName();
                                String propertyValue = propertyElement.getTextContent();
                                versionMap.put(propertyName, propertyValue);
                            }
                        }
                    }
                }
            }

            System.out.println("Number of dependencies: " + dependenciesList.getLength());
            System.out.println("----------------------------");
            for (int i = 0; i < dependenciesList.getLength(); i++) {
                Node dependencyNode = dependenciesList.item(i);
                System.out.println("\nCurrent Element :" + dependencyNode.getNodeName());
                if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element dependencyElement = (Element) dependencyNode;
                    // System.out.println("dependencyElement: " + dependencyElement.toString());
                    String groupId = dependencyElement.getElementsByTagName("groupId").item(0).getTextContent();
                    String artifactId = dependencyElement.getElementsByTagName("artifactId").item(0).getTextContent();
                    String version = dependencyElement.getElementsByTagName("version").item(0).getTextContent();

                    if (version.startsWith("${")) {
                        version = versionMap.get(version.substring(2, version.length() - 1));
                    }

                    String mavenPathBase = System.getProperty("user.home") + "/.m2/repository/"
                            + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                            + "/" + artifactId + "-" + version;

                    // System.out.println("\nCurrent mavenPath :" + mavenPath);

                    Dependency dependency = new Dependency(groupId, artifactId, version, mavenPathBase + ".jar",
                            mavenPathBase + "-sources.jar");
                    dependencyMap.put(artifactId, dependency);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependencyMap;
    }

    public static Map<String, String> parsePomForModules(String pomFilePath) {
        packageInfo = parseProjectInfo(pomFilePath);
        Map<String, String> moduleMap = new HashMap<>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(pomFilePath));

            doc.getDocumentElement().normalize();

            javaVersion = findJavaVersionInProperties(doc);

            // If not found in <properties>, attempt to find in maven-compiler-plugin
            // configuration
            if (javaVersion == null) {
                javaVersion = findJavaVersionInCompilerPlugin(doc);
            }

            NodeList modulesList = doc.getElementsByTagName("modules");

            if (modulesList.getLength() > 0) {
                for (int i = 0; i < modulesList.getLength(); i++) {
                    Node modulesNode = modulesList.item(i);
                    if (modulesNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element modulesElement = (Element) modulesNode;
                        NodeList moduleNodes = modulesElement.getElementsByTagName("module");
                        for (int j = 0; j < moduleNodes.getLength(); j++) {
                            Node moduleNode = moduleNodes.item(j);
                            if (moduleNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element moduleElement = (Element) moduleNode;
                                String moduleName = moduleElement.getTextContent();
                                // Assuming modules are located directly under the project root
                                String modulePomPath = Paths.get(pomFilePath).getParent().resolve(moduleName)
                                        .resolve("pom.xml").toString();

                                // Check if the module's pom.xml file exists
                                if (new File(modulePomPath).exists()) {
                                    moduleMap.put(moduleName, modulePomPath);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return moduleMap;
    }

    /** 
     * This method parses the `pom.xml` file for a Maven project and returns a `Dependency` object representing the project.
     */
    public static Dependency parseProjectInfo(String pomFilePath) {
        Dependency projectInfo = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(pomFilePath));
            doc.getDocumentElement().normalize();

            // Assuming there's only one project defined per pom.xml
            Node projectNode = doc.getElementsByTagName("project").item(0);
            if (projectNode.getNodeType() == Node.ELEMENT_NODE) {
                Element projectElement = (Element) projectNode;

                // Get groupId
                NodeList groupIdNodes = projectElement.getElementsByTagName("groupId");
                String groupId = (groupIdNodes.getLength() > 0) ? groupIdNodes.item(0).getTextContent() : "";

                // Get artifactId
                NodeList artifactIdNodes = projectElement.getElementsByTagName("artifactId");
                String artifactId = (artifactIdNodes.getLength() > 0) ? artifactIdNodes.item(0).getTextContent() : "";

                // Get version
                NodeList versionNodes = projectElement.getElementsByTagName("version");
                String version = (versionNodes.getLength() > 0) ? versionNodes.item(0).getTextContent() : "";

                // Get packaging
                NodeList packagingNodes = projectElement.getElementsByTagName("packaging");
                String packaging = (packagingNodes.getLength() > 0) ? packagingNodes.item(0).getTextContent() : "jar"; // Default
                                                                                                                       // to
                                                                                                                       // jar
                                                                                                                       // if
                                                                                                                       // not
                                                                                                                       // specified

                projectInfo = new Dependency(groupId, artifactId, version, "", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return projectInfo;
    }

    private static String findJavaVersionInProperties(Document doc) {
        NodeList propertiesList = doc.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Node propertiesNode = propertiesList.item(0); // Assuming one <properties> section
            if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
                Element properties = (Element) propertiesNode;
                String source = getTextContentByTagName(properties, "maven.compiler.source");
                String target = getTextContentByTagName(properties, "maven.compiler.target");
                // Prioritize source; fall back to target if necessary
                return (source != null) ? source : target;
            }
        }
        return null;
    }

    private static String findJavaVersionInCompilerPlugin(Document doc) {
        NodeList pluginList = doc.getElementsByTagName("plugin");
        for (int i = 0; i < pluginList.getLength(); i++) {
            Node pluginNode = pluginList.item(i);
            if (pluginNode.getNodeType() == Node.ELEMENT_NODE) {
                Element plugin = (Element) pluginNode;
                String artifactId = getTextContentByTagName(plugin, "artifactId");
                if ("maven-compiler-plugin".equals(artifactId)) {
                    NodeList configurations = plugin.getElementsByTagName("configuration");
                    if (configurations.getLength() > 0) {
                        Element configuration = (Element) configurations.item(0);
                        if (configuration != null) { // Added check for null
                            String source = getTextContentByTagName(configuration, "source");
                            if (source != null) {
                                return source;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String getTextContentByTagName(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    public static Dependency getPackageInfo() {
        return packageInfo;
    }

    public static String getJavaVersion() {
        return javaVersion;
    }

}
