package com.ast_generator;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses a Maven POM file to extract all the modules defined in it.
 */
public class MavenModuleParser {

    /**
     * Parses the given POM file to extract all the modules defined in it.
     * 
     * @param pomFilePath The path to the POM file.
     * @return A map of module names to their directory paths.
     */
    public static Map<String, String> parseAllPomForMavenModules(String pomFilePath) {
        Map<String, String> moduleMap = new HashMap<>();
        parsePomForModulesRecursive(pomFilePath, moduleMap);
        return moduleMap;
    }

    private static void parsePomForModulesRecursive(String pomFilePath, Map<String, String> moduleMap) {
        try {
            File pomFile = new File(pomFilePath);
            if (!pomFile.exists()) {
                System.out.println("POM file does not exist: " + pomFilePath);
                return;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            // Assuming Java version parsing logic is defined elsewhere
            // Update the javaVersion based on this POM file
            // javaVersion = findJavaVersionInPom(doc);

            NodeList modulesList = doc.getElementsByTagName("modules");
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
                            // Resolve the path to the module's directory
                            String moduleDirPath = Paths.get(pomFile.getParent()).resolve(moduleName).toString();
                        
                            // Construct the path to the module's pom.xml to check for its existence
                            String modulePomPath = Paths.get(moduleDirPath, "pom.xml").toString();
                        
                            if (new File(modulePomPath).exists()) {
                                // Store the directory path of the module instead of the pom.xml path
                                moduleMap.put(moduleName, moduleDirPath);
                                // Recursively parse the found module for more modules using the pom.xml path
                                parsePomForModulesRecursive(modulePomPath, moduleMap);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("An error occurred while parsing POM: " + pomFilePath);
            e.printStackTrace();
        }
    }
}
