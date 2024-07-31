import json
import sys

def find_function_in_analysis(analysis, type_name):
    """
    Recursively searches for the type in analysis data and nested method calls.
    Returns True if the type or function call matches the given type_name, False otherwise.
    """
    if type_name in analysis.get('declaringType', '') or type_name in analysis.get('fullExpression', ''):
        return True
    # Recurse into inner method calls
    for call in analysis.get('innerMethodCalls', []):
        if find_function_in_analysis(call, type_name):
            return True
    return False

def process_vulnerabilities(analysis_data, vulnerabilities):
    """
    Process each vulnerability against the analysis data to determine false positives.
    """
    results = []
    for vuln in vulnerabilities:
        type_ = vuln['class']
        group_id = vuln['dependencyGroupId']
        artifact_id = vuln['dependencyArtifactId']

        print(f"Processing vulnerability: {type_} in {group_id}:{artifact_id}")
        # Initialize as true; set to false if found in analysis
        false_positive = True
        evidence = []

        # Search for matches in analysis data
        for key, components in analysis_data.items():
            if f"groupId={group_id}, artifactId={artifact_id}" in key:
                print('found key')
                for component in components:
                    if find_function_in_analysis(component, type_):
                        false_positive = False
                        evidence.append({
                            "declaringType": component['declaringType'],
                            "fullExpression": component['fullExpression']
                        })
                        break
                if not false_positive:
                    break

        # Append result with evidence and false positive status
        vuln_result = vuln.copy()
        vuln_result['vulnerable'] = not false_positive
        vuln_result['evidence'] = evidence
        results.append(vuln_result)
    
    return results

def main(sbom_file_path, vulnerabilities_file_path, output_file_path):
    # Load the SBOM and vulnerabilities data
    with open(sbom_file_path, 'r') as file:
        sbom_data = json.load(file)
    with open(vulnerabilities_file_path, 'r') as file:
        vulnerabilities = json.load(file)

    print(vulnerabilities)
    # Determine the SBOM format and extract analysis section
    format = 'CDX' if 'externalReferences' in sbom_data else 'SPDX'
    analysis_data = {}

    if format == 'CDX':
        analysis_data = next(
            (item['component'] for item in sbom_data.get('externalReferences', [])
             if item.get('comment') == 'CovSBOM_Analysis results'),
            {}
        )
    elif format == 'SPDX':
        analysis_data = next(
            (item['component'] for item in sbom_data.get('externalRefs', [])
             if item.get('comment') == 'CovSBOM_Analysis results'),
            {}
        )

    # Process vulnerabilities and determine false positives
    results = process_vulnerabilities(analysis_data, vulnerabilities)

    # Save the results to the output file
    with open(output_file_path, 'w') as file:
        json.dump(results, file, indent=4)

    print(f"Analysis completed. Results are saved in {output_file_path}")

if __name__ == '__main__':
    if len(sys.argv) != 4:
        print("Usage: python3 script.py <sbom.json> <vulnerabilities.json> <output.json>")
        sys.exit(1)
    _, sbom_path, vulnerabilities_path, output_path = sys.argv
    main(sbom_path, vulnerabilities_path, output_path)

# Usage:
# python3 scanCovSBOMAnalysis.py ../SBOM_Integration/bom_cdx.json ./vul.json ./vul_output.json
