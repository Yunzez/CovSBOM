import React, { useEffect, useState } from "react";
import { StageProps } from "../context/GlobalContext";
import { useGlobalContext } from "../context/GlobalContext";
import data from "../../../../CovSBOM_output/analysis/spark-master/final_report_package_based.json";
import TreeGraph from "./TreeGraph";
import { MethodCallEntry } from "./TreeGraph";
interface Dependency {
  sourceFilePath: string;
  declarationStartLine: number;
  declarationEndLine: number;
  methodName: string;
  declarationSignature: string;
}

interface MethodCall {
  declaringType: string;
  methodName: string;
  lineNumber: string[];
  fullExpression: string;
  declarationInfo: Dependency; // New field
  currentLayer: number;
  methodSignature: string;
}

const Results = () => {
  const { path, setPath, stage, setStage } = useGlobalContext();
  const dependenciesArray = Object.entries(data).map(([key, value]) => ({
    dependencyInfo: key,
    methodCalls: value as unknown as MethodCall[],
  }));
  const dataSize = dependenciesArray.length;
  const [selectedDependency, setSelectedDependency] =
    useState<MethodCallEntry[]>();
  const [selectedDependencyName, setSelectedDependencyName] =
    useState<string>();
  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <h1 className="text-2xl font-bold text-gray-700 mb-4">
        Dependency Analysis Results
      </h1>
      <p className="mb-6 text-gray-600">
        Total Dependencies Analyzed: {dataSize}
      </p>

      <button
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
        onClick={() => {
          setStage(StageProps.SELECT);
        }}
      >
        Select Other Projects
      </button>
      <div className="flex justify-start overflow-scroll">
        {Object.entries(dependenciesArray).map(
          ([dependencyName, methodCalls], index) => {
            // Parse the dependency info to get the group ID and artifact ID
            const infoParts = methodCalls.dependencyInfo.split(", ");
            const groupId = infoParts
              .find((part) => part.startsWith("groupId="))
              ?.split("=")[1];
            const artifactId = infoParts
              .find((part) => part.startsWith("artifactId="))
              ?.split("=")[1];

            return (
              <section className="p-3 text-center" style={{ minWidth: "25vw", minHeight: "10vw" }}>
                <div
                style={{ minHeight: "10vw" }}
                  key={index}
                  className="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-200 ease-in-out hover:bg-gray-100 cursor-pointer h-100"
                  onClick={() => {
                    setSelectedDependency(
                      methodCalls.methodCalls as MethodCallEntry[]
                    );
                    setSelectedDependencyName(methodCalls.dependencyInfo);
                  }}
                >
                  <h2 className="text-md font-semibold text-gray-800 mb-1">
                    {groupId} {/* Display Group ID */}
                  </h2>
                  <h2 className="text-md font-semibold text-gray-800 mb-1">
                    {artifactId} {/* Display Artifact ID */}
                  </h2>
                </div>
              </section>
            );
          }
        )}
      </div>

      {selectedDependency && (
        <TreeGraph
          key={selectedDependencyName}
          data={{
            dependencyInfo: selectedDependencyName,
            methodCalls: selectedDependency,
          }}
        />
      )}

      {/* <TreeGraph data={dependenciesArray[0]} /> */}
    </div>
  );
};

export default Results;
