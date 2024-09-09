# CovSBOM

<img src="logo.png" width="200">

**CovSBOM** is a robust static analysis tool designed to enhance Software Bill of Materials (SBOM) by integrating detailed static analysis to reduce false positive rates in vulnerability scans. This tool leverages and extends existing SBOM capabilities to provide deeper insights into code dependencies and security vulnerabilities. You can view this quick [tutorial](https://drive.google.com/file/d/1RQ8fvevT_mb7EY4UntxKEB81qBLRJGxr/view?usp=drive_link) to see how CovSBOM works or follow the documentation below.


## File Structure: 

### **1. ast_generator**
This folder contains the core functionality of CovSBOM, which is responsible for multiple key processes:
- **AST Generation**: Generates abstract syntax trees (ASTs) from the analyzed code.
- **Decompiling Third-Party Libraries**: Handles the decompilation process for third-party libraries.
- **Source Analysis**: Performs static analysis on the source code to detect vulnerabilities.
- **CovSBOM Output**: Produces the main CovSBOM output, which integrates into the SBOM.

### **2. CovSBOM_output/analysis**
This folder stores the output files generated for each project. For each project, you will find three key files:

- **method_calls.json**: 
  - This file contains a record of all unique method calls to third-party libraries before decompilation. It helps track which methods in the target program invoke external libraries.

- **final_report_file_based.json**: 
  - This file provides detailed call stack information to third-party libraries on a per-file basis. It may contain duplicates when different files in the target program call the same method.

- **final_report_package_base.json**: 
  - This file eliminates duplicates by organizing the call stack data based on third-party library packages. It only provides information for packages that the target program reaches, offering a streamlined view of method calls to external libraries.

### **3. SBOM_Integration**
this folder contains the required scripts to insert analysis into SBOM for both CDX and SPDX format.

### **4. Analysis_scan**
this folder contains the required scripts to scan for vulnerabilities after SBOM integration.

---

## Example Project:
We use [Spark - a tiny web framework for Java 8](https://github.com/perwendel/spark) for example project, if you run CovSBOM and enter no source file, it would default to this example application for review purposes. 

```bash
// install required packages first
cd Application/spark-master
mvn clean install -DskipTests
mvn dependency:sources

// back to project main folder
cd ../..

// now run the example:

java -jar CovSBOM.jar
-------Initializing-------
Please enter the path to the Java source file:  // leave this blank 
rootDirectoryPath: /Code/CovSBOM
Inferred path to pom.xml: Application/spark-master/pom.xml
```

## Prerequisites

Before you begin using CovSBOM, ensure that your environment is set up with the necessary Java libraries. The tool ##assumes that all dependencies and plugins are properly installed##. If you have not yet set up the environment, follow the steps below to ##install the required components##:

```bash
mvn clean install -DskipTests
mvn dependency:sources
```

## Installation

To install CovSBOM, clone this repository to your local machine using the following command:

```bash
git clone https://github.com/Yunzez/CovSBOM.git
```

Navigate into the cloned repository:

```bash
cd CovSBOM
```

## Usage

To run CovSBOM and perform the analysis, use the following command under the project directory:

```bash
java -jar CovSBOM.jar
```
For a step-by-step guide, watch the [Tutorial Video](https://github.com/Yunzez/CovSBOM/blob/main/Tutorial/CovSBOM_Tutorial.mp4) to learn more.

Here's a refined version of your console output explanation:

---

## Console Output Explanation

At the end of CovSBOM's execution, there are a few key outputs that are important to understand:

- **`<Package name> : []`**  
  This output shows the method call loading buffer for a specific package. An empty buffer (`[]`) indicates that all methods within the package were successfully resolved. If the buffer is not empty, it will display unresolved type names within that package, meaning that CovSBOM could not resolve or find certain files in that package.

- **`no jar dependencies`**  
  This output lists the dependencies that do not have corresponding `.jar` files, which means they could not be decompiled and analyzed. If everything runs correctly, this output should show only the target file as having no `.jar` dependency.

## SBOM Integration

To integrate the analysis into your SBOM, you can use the following command:

```bash
python3 cdx.py < sbom.json > < analysis_file_path >
```

This command will insert the analysis into your SBOM file.

## Analysis Scan

After generating the integrated file, you can run the scanning tool to scan your SBOM:

```bash
cd Analysis_scan
```

### Vulnerability Input Example

you will need your own **vulnerabilities.json** file, that looks similar to:

```json
[
  {
    "SBOM_Format": "CycloneDX",
    "CVE-ID": "CVE-2023-1234",
    "Sbom_scanning_tool": "OWASP Dependency-Check",
    "class": "TemporaryFolder",
    "function": "newFolder()",
    "dependencyGroupId": "junit",
    "dependencyArtifactId": "junit"
  },
  {
    "SBOM_Format": "CycloneDX",
    "CVE-ID": "CVE-2023-5678",
    "Sbom_scanning_tool": "OWASP Dependency-Check",
    "class": "TemporaryFolder",
    "function": "None",
    "dependencyGroupId": "org.eclipse.jetty.websocket",
    "dependencyArtifactId": "websocket-server"
  }
]
```

### Explanation of Fields

- **function**: Specifies the function within the class that is potentially vulnerable. For example, `"newFolder()"` indicates that the `newFolder` function within the `TemporaryFolder` class is under scrutiny. `"None"` means no specific function is being targeted.
- **SBOM_Format**: The format of the Software Bill of Materials (SBOM). `"CycloneDX"` is one of the popular formats.
- **CVE-ID**: The Common Vulnerabilities and Exposures (CVE) identifier for the vulnerability. This is a unique identifier for the vulnerability, such as `"CVE-2023-1234"`.
- **Sbom_scanning_tool**: The tool used to scan and generate the SBOM, for instance, `"OWASP Dependency-Check"`.
- **class**: The class in the dependency that is being evaluated for vulnerabilities, such as `"TemporaryFolder"`.
- **dependencyGroupId**: The group ID of the dependency, e.g., `"junit"`.
- **dependencyArtifactId**: The artifact ID of the dependency, e.g., `"junit"`.

```bash
python3 scanCovSBOMAnalysis.py  <integrated_sbom.json > <vulnerabilities.json > <output.json >
```

This command will initiate the scanning process on your SBOM.

### Output File Example

```json
[
    {
        "Function": "newFolder()",
        "SBOM_Format": "CycloneDX",
        "CVE-ID": "your-cve-id",
        "Sbom_scanning_tool": "OWASP Dependency-Check",
        "class": "TemporaryFolder",
        "dependencyGroupId": "junit",
        "dependencyArtifactId": "junit",
        "vulnerable": true,
        "evidence": [
            {
                "declaringType": "org.junit.rules.TemporaryFolder",
                "fullExpression": "temporaryFolder.newFolder()"
            }
        ]
    },
    {
        "SBOM_Format": "CycloneDX",
        "CVE-ID": "your-cve-id",
        "Sbom_scanning_tool": "OWASP Dependency-Check",
        "class": "TemporaryFolder",
        "Function": "None",
        "dependencyGroupId": "org.eclipse.jetty.websocket",
        "dependencyArtifactId": "websocket-server",
        "vulnerable": false,
        "evidence": []
    }
]
```

### Explanation of Fields

- **Function**: The function within the class that was checked for vulnerabilities. For example, `"newFolder()"` indicates that the `newFolder` function within the `TemporaryFolder` class was checked.
- **SBOM_Format**: The format of the Software Bill of Materials (SBOM). `"CycloneDX"` is one of the popular formats.
- **CVE-ID**: The Common Vulnerabilities and Exposures (CVE) identifier for the vulnerability. This is a unique identifier for the vulnerability, such as `"your-cve-id"`.
- **Sbom_scanning_tool**: The tool used to scan and generate the SBOM, in this case, `"OWASP Dependency-Check"`.
- **class**: The class in the dependency that was evaluated for vulnerabilities, such as `"TemporaryFolder"`.
- **dependencyGroupId**: The group ID of the dependency, e.g., `"junit"`.
- **dependencyArtifactId**: The artifact ID of the dependency, e.g., `"junit"`.
- **vulnerable**: A boolean value indicating whether the dependency is vulnerable (`true`) or not (`false`).
- **evidence**: A list of evidence supporting the determination of vulnerability. Each piece of evidence includes:
  - **declaringType**: The class type where the vulnerability was found, such as `"org.junit.rules.TemporaryFolder"`.
  - **fullExpression**: The full expression in the code where the vulnerable function is called, such as `"temporaryFolder.newFolder()"`.

### Example Outputs

1. **Vulnerable Dependency**:
    - **Example**: 
        ```json
        {
            "Function": "newFolder()",
            "SBOM_Format": "CycloneDX",
            "CVE-ID": "your-cve-id",
            "Sbom_scanning_tool": "OWASP Dependency-Check",
            "class": "TemporaryFolder",
            "dependencyGroupId": "junit",
            "dependencyArtifactId": "junit",
            "vulnerable": true,
            "evidence": [
                {
                    "declaringType": "org.junit.rules.TemporaryFolder",
                    "fullExpression": "temporaryFolder.newFolder()"
                }
            ]
        }
        ```
    - **Explanation**: The `TemporaryFolder` class from the `junit` dependency is found to be vulnerable when calling the `newFolder()` function. The evidence provided includes the declaring type and the full expression of the vulnerability in the code.

2. **Non-Vulnerable Dependency**:
    - **Example**: 
        ```json
        {
            "SBOM_Format": "CycloneDX",
            "CVE-ID": "your-cve-id",
            "Sbom_scanning_tool": "OWASP Dependency-Check",
            "class": "TemporaryFolder",
            "Function": "None",
            "dependencyGroupId": "org.eclipse.jetty.websocket",
            "dependencyArtifactId": "websocket-server",
            "vulnerable": false,
            "evidence": []
        }
        ```
    - **Explanation**: The `TemporaryFolder` class from the `websocket-server` dependency (under `org.eclipse.jetty.websocket` group) is not found to be vulnerable. No specific function was checked, and no evidence was found indicating any vulnerability.
    

## Contributing

Contributions to CovSBOM are welcome! If you would like to contribute, please fork the repository and submit a pull request with your changes. For major changes, please open an issue first to discuss what you would like to change.

## License

CovSBOM is released under the [GNU General Public License v3.0](LICENSE). See the LICENSE file for more details.
