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


# Initialize variables
# project_folder=""
# output_file=""

# # Parse command line options
# while getopts "p:o:" opt; do
#   case ${opt} in
#     p ) project_folder=$OPTARG ;;
#     o ) output_file=$OPTARG ;;
#     \? ) echo "Usage: cmd [-p] project_folder_path [-o] output_file_path"
#          exit 1 ;;
#   esac
# done

# # Check if project folder path has been set
# if [ -z "$project_folder" ]; then
#     echo "Project folder path must be specified with -p"
#     exit 1
# fi

# # Check if output file path has been set
# if [ -z "$output_file" ]; then
#     echo "Output file path must be specified with -o"
#     exit 1
# fi

# # Check and create the output file if it does not exist
# if [ ! -f "$output_file" ]; then
#     touch "$output_file"
# fi

# # Find all 'pom.xml' files within the project folder
# find "$project_folder" -name 'pom.xml' | while read pom_path; do
#     dir_path=$(dirname "$pom_path")
#     echo "Processing in directory: $dir_path"
#     cd "$dir_path"
#     # Execute Maven commands and append output to file
#     echo "Running 'mvn source':"
#     mvn dependency:sources
#     mvn -f "$dir_path" dependency:tree -DoutputFile="$output_file" -DappendOutput=true
# done

# echo "All Maven command outputs have been written to $output_file"