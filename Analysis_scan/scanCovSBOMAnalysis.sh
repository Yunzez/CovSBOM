#!/bin/bash

# Check if the correct number of arguments was passed
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <sbom.json> <vulnerabilities.json> <output.json>"
    exit 1
fi

SBOM_FILE="$1"
VULN_FILE="$2"
OUTPUT_FILE="$3"

# Read the vulnerabilities file and process each entry
jq -c '.[]' "$VULN_FILE" | while read -r vuln; do
    type=$(echo "$vuln" | jq -r '.type')
    dependencyGroupId=$(echo "$vuln" | jq -r '.dependencyGroupId')
    dependencyArtifactId=$(echo "$vuln" | jq -r '.dependencyArtifactId')

    # Check if the type exists in the SBOM analysis data
    exists=$(jq --arg type "$type" --arg groupId "$dependencyGroupId" --arg artifactId "$dependencyArtifactId" '
    .dependencies[] | select(.groupId == $groupId and .artifactId == $artifactId) | .analysis | any(.name == $type)
    ' "$SBOM_FILE")

    # Determine the false positive status based on the existence check
    falsePositive="true"
    if [ "$exists" == "true" ]; then
        falsePositive="false"
    fi

    # Add the falsePositive flag to the vulnerability object
    modified_vuln=$(echo "$vuln" | jq --arg fp "$falsePositive" '. + {"falsePositive": $fp}')

    # Append the modified vulnerability to the output file
    echo "$modified_vuln" >> temp_output.json
done

# Format the output into a proper JSON array
jq -s '.' temp_output.json > "$OUTPUT_FILE"
rm temp_output.json

echo "Analysis completed. Results are saved in $OUTPUT_FILE"
