import React, { useState, ChangeEvent, useEffect } from "react";
import logo from "../logo.svg";

interface DirectoryInputAttributes
  extends React.InputHTMLAttributes<HTMLInputElement> {
  directory?: string;
  webkitdirectory?: string;
}

import { StageProps, useGlobalContext } from "../context/GlobalContext";

const SelectFile: React.FC = () => {
  const { path, setPath, stage, setStage } = useGlobalContext();

  const handleSelectDirectory = async () => {
    const path = await window.electron.invoke("open-directory-dialog");
    setPath(path);
    console.log(path);
  };

  const startAnalysis = async () => {
    const result = await window.electron.invoke("run-java-program", path);
  };

  const [uploadProject, setUploadProject] = useState(true);

  return (
    <div>
      <section>
        <div className="min-h-screen min-w-screen bg-gray-100 flex flex-col justify-center items-center">
          <div className="items-center  p-5" style={{ width: "50vw" }}>
            <div
              className="items-cente flex justify-center"
              style={{ width: "100%" }}
            >
              <img src={logo} alt="logo" className="h-36 " />
            </div>
            {/* <div className="flex justify-between">
              <button
                onClick={() => setUploadProject(true)}
                className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              >
                Analyze Project
              </button>
              <button
                onClick={() => setUploadProject(false)}
                className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              >
                Visualize Analysis
              </button>
            </div> */}
          </div>
          <div className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-10">
            <h1 className="block text-gray-700 text-xl font-bold mb-3">
              Welcome to CovSBOM
            </h1>

            <p className="text-gray-700 text-base mb-1 text-center">
              CovSBOM is a tool designed to help you analyze your{" "}
              <b>Java Maven</b> project's software dependencies and security.
            </p>
            <p className="text-gray-700 text-base mb-4 text-center">
              Get started by selecting your project root directory.
            </p>

            {uploadProject && (
              <div>
                <label
                  className="block text-gray-700 text-sm font-bold mb-2"
                  htmlFor="project-root"
                >
                  Project Root
                </label>
                <div className="flex items-center">
                  <input
                    className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                    id="project-root"
                    type="text"
                    value={path}
                    readOnly
                  />
                  <button
                    onClick={handleSelectDirectory}
                    className="ml-4 bg-indigo-500 hover:bg-indigo-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
                  >
                    Select
                  </button>
                </div>
                <p className="mt-2 text-sm text-gray-600">
                  {path
                    ? `Selected directory: ${path}`
                    : "No directory selected."}
                </p>
                <button
                  className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mt-4"
                  onClick={() => {
                    console.log("Analyze");
                    setStage(StageProps.ANALYZE);
                    startAnalysis();
                  }}
                >
                  Analyze
                </button>

                <button
                  className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mt-4"
                  onClick={() => {
                    console.log("Analyze");
                    setStage(StageProps.RESULTS);
                    startAnalysis();
                  }}
                >
                  go to analysis
                </button>
              </div>
            )}

          </div>
        </div>
      </section>
    </div>
  );
};

export default SelectFile;
