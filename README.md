
# CovSBOM

<img src="logo.png" width="200">

**CovSBOM** is a robust static analysis tool designed to enhance Software Bill of Materials (SBOM) by integrating detailed static analysis to reduce false positive rates in vulnerability scans. This tool leverages and extends existing SBOM capabilities to provide deeper insights into code dependencies and security vulnerabilities.

## Prerequisites

Before you begin using CovSBOM, ensure that your environment is set up with the necessary Java libraries. The tool assumes that all dependencies and plugins are properly installed. If you have not yet set up the environment, follow the steps below to install the required components:

```bash
mvn install -DskipTests
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

## SBOM Integration

To integrate the analysis into your SBOM, you can use the following command:

```bash
python3 cdx.py < sbom.json > < analysis_file_path >
```

This command will insert the analysis into your SBOM file.

## Analysis Scan
After generating the integrated file, you can run the scanning tool to scan your SBOM using the following command:

### Vulnerability Input Example
you will need your own **vulnerabilities.json** file, that looks similar to:
```json
[
    {
        "Function": "newFolder()",
        "SBOM_Format": "CycloneDX",
        "CVE-ID": "CVE-2023-1234",
        "Sbom_scanning_tool": "OWASP Dependency-Check",
        "class": "TemporaryFolder",
        "dependencyGroupId": "junit",
        "dependencyArtifactId": "junit"
    },
    {
        "SBOM_Format": "CycloneDX",
        "CVE-ID": "CVE-2023-5678",
        "Sbom_scanning_tool": "OWASP Dependency-Check",
        "class": "TemporaryFolder",
        "Function": "None",
        "dependencyGroupId": "org.eclipse.jetty.websocket",
        "dependencyArtifactId": "websocket-server"
    }
]
```

### Explanation of Fields

- **Function**: Specifies the function within the class that is potentially vulnerable. For example, `"newFolder()"` indicates that the `newFolder` function within the `TemporaryFolder` class is under scrutiny. `"None"` means no specific function is being targeted.
- **SBOM_Format**: The format of the Software Bill of Materials (SBOM). `"CycloneDX"` is one of the popular formats.
- **CVE-ID**: The Common Vulnerabilities and Exposures (CVE) identifier for the vulnerability. This is a unique identifier for the vulnerability, such as `"CVE-2023-1234"`.
- **Sbom_scanning_tool**: The tool used to scan and generate the SBOM, for instance, `"OWASP Dependency-Check"`.
- **class**: The class in the dependency that is being evaluated for vulnerabilities, such as `"TemporaryFolder"`.
- **dependencyGroupId**: The group ID of the dependency, e.g., `"junit"`.
- **dependencyArtifactId**: The artifact ID of the dependency, e.g., `"junit"`.
- 

```bash
python3 scanCovSBOMAnalysis.py  <integrated_sbom.json > <vulnerabilities.json > <output.json >
```

This command will initiate the scanning process on your SBOM.

## Contributing

Contributions to CovSBOM are welcome! If you would like to contribute, please fork the repository and submit a pull request with your changes. For major changes, please open an issue first to discuss what you would like to change.

## License

CovSBOM is released under the [MIT License](LICENSE). See the LICENSE file for more details.

