#!/bin/bash

# The first argument is the project root path
PROJECT_ROOT_PATH=$1

# The second argument is the desired output file path for the aggregated dependency tree
OUTPUT_FILE=$2

# The remaining arguments are the module root paths
MODULE_PATHS=("${@:3}")

# Ensure the output file is empty before starting
> "$OUTPUT_FILE"

echo "Aggregating Maven dependency trees..."

# Loop through each module path
for MODULE_PATH in "${MODULE_PATHS[@]}"; do
    echo "Processing module at $MODULE_PATH..."

    # Construct the path to the module's pom.xml file
    MODULE_POM_PATH="$PROJECT_ROOT_PATH/$MODULE_PATH/pom.xml"
    
    if [[ -f "$MODULE_POM_PATH" ]]; then
        # Use Maven's -DoutputFile option to write the dependency tree directly to the output file
        mvn -f "$MODULE_POM_PATH" dependency:tree -DoutputFile="$OUTPUT_FILE" -DappendOutput=true
        echo "Finished processing $MODULE_PATH"
    else
        echo "POM file not found for $MODULE_PATH"
    fi
done

echo "Aggregation complete. Results stored in $OUTPUT_FILE"
