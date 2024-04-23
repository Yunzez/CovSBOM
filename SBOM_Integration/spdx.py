import json
import sys

def insert_analysis_to_spdx_sbom(spdx_path, analysis_path):
    # Load the SPDX document
    with open(spdx_path, 'r') as file:
        spdx = json.load(file)
    
    # Load the analysis data
    with open(analysis_path, 'r') as file:
        analysis_data = json.load(file)
    
    # Check if 'externalRefs' exists in the SPDX document
    # Assuming we're adding this to the package level
    if 'packages' in spdx:
        for package in spdx['packages']:
            if 'externalRefs' not in package:
                package['externalRefs'] = []
            package['externalRefs'].append({
                "referenceType": "OTHER",
                "referenceLocator": "CovSBOM_Analysis results",
                "comment": json.dumps(analysis_data)  # Convert JSON analysis to a string if inserting directly
            })
    
    # Save the modified SPDX document
    with open(spdx_path, 'w') as file:
        json.dump(spdx, file, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py <path_to_spdx.json> <path_to_analysis.json>")
        sys.exit(1)
    
    spdx_path = sys.argv[1]
    analysis_path = sys.argv[2]
    insert_analysis_to_spdx_sbom(spdx_path, analysis_path)
    print("Analysis inserted successfully into SPDX.")
