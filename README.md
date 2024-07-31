
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

```bash
python3 scanCovSBOMAnalysis.py  <integrated_sbom.json > <vulnerabilities.json > <output.json >
```

This command will initiate the scanning process on your SBOM.

## Contributing

Contributions to CovSBOM are welcome! If you would like to contribute, please fork the repository and submit a pull request with your changes. For major changes, please open an issue first to discuss what you would like to change.

## License

CovSBOM is released under the [MIT License](LICENSE). See the LICENSE file for more details.

