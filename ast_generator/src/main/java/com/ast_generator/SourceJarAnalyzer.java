package com.ast_generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class SourceJarAnalyzer {
    private Path jarPath;
    private Collection<String> targetPackages;
    private Path decompressDir;

    // Unified constructor
    public SourceJarAnalyzer(String jarPath, Collection<String> targetPackages, String decompressDir) {
        this.jarPath = Paths.get(jarPath);
        this.targetPackages = targetPackages; // Accepts any Collection<String>
        this.decompressDir = Paths.get(decompressDir);
    }

    public void analyze() throws IOException {
        // Decompress the JAR file
        decompressJar();

        // Process the decompressed directory
        processDecompressedDirectory();
    }

    private void decompressJar() throws IOException {
        if (!Files.exists(decompressDir)) {
            Files.createDirectories(decompressDir);
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarPath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = decompressDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    // Extract file
                    extractFile(zipIn, filePath);
                } else {
                    // Make directory
                    Files.createDirectories(filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent()); // Ensure directory exists
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private void processDecompressedDirectory() throws IOException {
        Files.walk(decompressDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(this::processJavaFile);
    }

    private void processJavaFile(Path javaFilePath) {
        // Implement logic to check if the Java file is within the target packages
        // and process it accordingly.
        System.out.println("Processing Java file: " + javaFilePath);
    }
}
