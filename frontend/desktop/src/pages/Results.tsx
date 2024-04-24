import React, { useEffect } from "react";
import { StageProps } from "../context/GlobalContext";
import { useGlobalContext } from "../context/GlobalContext";
import data from "../../../../CovSBOM_output/analysis/spark-master/final_report_package_based.json";

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

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <h1 className="text-2xl font-bold text-gray-700 mb-4">Dependency Analysis Results</h1>
      <p className="mb-6 text-gray-600">Total Dependencies Analyzed: {dataSize}</p>
  
      <button
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
        onClick={() => {
          setStage(StageProps.SELECT);
        }}
      >
        Select Other Projects
      </button>
  
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-8">
        {dependenciesArray.map((dependency, index) => (
          <div key={index} className="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-200 ease-in-out">
            <h2 className="text-md font-semibold text-gray-800 mb-1">{dependency.dependencyInfo.split(",")[0]}</h2>
            <h2 className="text-md font-semibold text-gray-800 mb-1">{dependency.dependencyInfo.split(",")[1]}</h2>
            <div className="text-gray-600 flex flex-wrap">
              {dependency.methodCalls.map((call, callIndex) => (
                <div key={callIndex} className="text-sm shadow p-1 rounded-md bg-gray-200 shadow m-1">
                  Method: {call.methodName}, Line Number: {call.lineNumber.join(", ")}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
  
};

export default Results;
