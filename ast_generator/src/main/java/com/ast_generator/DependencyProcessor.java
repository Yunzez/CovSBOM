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
                // System.out.println("\nCurrent Element :" + dependencyNode.getNodeName());
                if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element dependencyElement = (Element) dependencyNode;

                    String groupId = dependencyElement.getElementsByTagName("groupId").item(0).getTextContent();
                    String artifactId = dependencyElement.getElementsByTagName("artifactId").item(0).getTextContent();
                    String version = dependencyElement.getElementsByTagName("version").item(0).getTextContent();

                    if (version.startsWith("${")) {
                        version = versionMap.get(version.substring(2, version.length() - 1));
                    }
                    
                    // System.out.println("\nCurrent groupId :" + groupId);
                    // System.out.println("\nCurrent artifactId :" + artifactId);
                    // System.out.println("\nCurrent version :" + version);

                    String mavenPathBase = System.getProperty("user.home") + "/.m2/repository/"
                            + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                            + "/" + artifactId + "-" + version;


                    // System.out.println("\nCurrent mavenPath :" + mavenPath);
                    

                    Dependency dependency = new Dependency(groupId, artifactId, version, mavenPathBase + ".jar", mavenPathBase + "-sources.jar");
                    dependencyMap.put(artifactId, dependency);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependencyMap;
    }

}
