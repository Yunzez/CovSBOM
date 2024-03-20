import React, { useState, ChangeEvent } from "react";
import logo from "./logo.svg";
import "./App.css";


interface DirectoryInputAttributes extends React.InputHTMLAttributes<HTMLInputElement> {
  directory?: string;
  webkitdirectory?: string;
}

function App() {
  const [selectedPath, setSelectedPath] = useState('');

  const handleDirectoryChange = (event: ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length) {
      // Assuming the user selects a directory, the path of the first file is used to extract the directory path
      const path = event.target.files[0].webkitRelativePath.split('/')[0];
      setSelectedPath(path);
    }
  };
  return (
    <div className="App">
      =
      <section>
        <div className="min-h-screen bg-gray-100 flex flex-col justify-center items-center">
          <div className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">
            <h1 className="block text-gray-700 text-xl font-bold mb-2">
              Welcome to CovSBOM
            </h1>
            <p className="text-gray-700 text-base mb-4">
              CovSBOM is a tool designed to help you analyze your project's
              software dependencies and security. Get started by selecting your
              project root directory.
            </p>
            <div>
              <label
                className="block text-gray-700 text-sm font-bold mb-2"
                htmlFor="project-root"
              >
                Project Root
              </label>
              <input
                className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                id="project-root"
                type="file"
                {...{ directory: '', webkitdirectory: '' } as DirectoryInputAttributes}
                onChange={handleDirectoryChange}
              />
              <p className="mt-2 text-sm text-gray-600">
                {selectedPath
                  ? `Selected directory: ${selectedPath}`
                  : "No directory selected."}
              </p>
              <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mt-4">
                Analyze
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default App;
