import json
import sys

def insert_analysis_to_sbom(sbom_path, analysis_path):
    # Load the SBOM
    with open(sbom_path, 'r') as file:
        sbom = json.load(file)
    
    # Load the analysis data
    with open(analysis_path, 'r') as file:
        analysis_data = json.load(file)
    
    # Check if 'externalReferences' exists in the SBOM
    if 'externalReferences' not in sbom:
        sbom['externalReferences'] = []
    
    # Add the analysis data to 'externalReferences'
    sbom['externalReferences'].append({
        "type": "other",
        "comment": "CovSBOM_Analysis results",
        "component": analysis_data
    })
    
    # Save the modified SBOM
    with open(sbom_path, 'w') as file:
        json.dump(sbom, file, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 script.py <path_to_sbom.json> <path_to_analysis.json>")
        sys.exit(1)
    
    sbom_path = sys.argv[1]
    analysis_path = sys.argv[2]
    insert_analysis_to_sbom(sbom_path, analysis_path)
    print("Analysis inserted successfully into SBOM.")
