import React, { useEffect, useRef, useState } from "react";
import { StageProps } from "../context/GlobalContext";
import { useGlobalContext } from "../context/GlobalContext";

import "../styles/typeWritter.css";
import "../styles/slideLoader.css";
const Analyze = () => {
  const { path, setPath, stage, setStage } = useGlobalContext();
  const [loading, setLoading] = useState(true);
  const [logs, setLogs] = useState([]);
  const logsEndRef = useRef(null);
  const scrollToBottom = () => {
    logsEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };
  useEffect(() => {
    window.electron.on("java-log", (log) => {
      console.log(log); // or update the state to display the log in the UI
      setLogs((currentLogs) => [...currentLogs, log]);
      scrollToBottom();
      if (log[0].includes("End of analysis")) {
        setStage(StageProps.RESULTS);
        waitFor(2000).then(() => {
          setLoading(false);
        });
      }
    });
  }, []);

  const waitFor = (ms: number) => new Promise((r) => setTimeout(r, ms));

  return (
    <div className="min-h-screen min-w-screen bg-gray-100 flex flex-col justify-center items-center">
      {loading ? (
        <>
          <div
            className=" top-0 left-0 m-4 p-4 bg-white shadow rounded-lg overflow-scroll shadow-lg"
            style={{ maxHeight: "70%", height:"65vh" }}
          >
            <h2 className="font-bold text-lg">Logs</h2>
            <div className="text-sm overflow-scroll">
              {logs.map((log, index) => (
                <div key={index}>{log}</div>
              ))}
              {/* Empty div at the end of your logs */}
              <div ref={logsEndRef} />
            </div>
          </div>
          {/* <button
            className="p-2 bg-gray-300 shadow rounded-lg m-3"
            onClick={() => setLoading(false)}
          >
            done
          </button> */}

          <div className="p-6 bg-gray-300 shadow rounded-lg m-3">
            {/* typewriter animation */}
            <div className="typewriter">
              <div className="slide">
                <i></i>
              </div>
              <div className="paper"></div>
              <div className="keyboard"></div>
            </div>
          </div>

          {/* slide animation */}
          <div className="p-5 bg-gray-300 shadow rounded-lg m-3 relative  ">
            <div className="slideLoader"></div>
          </div>
          <h1 className="text-xl font-bold text-center mt-5">
            Analyzing your project...
          </h1>
        </>
      ) : (
        <div>
          <button
            className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mt-4"
            onClick={() => {
              setStage(StageProps.RESULTS);
            }}
          >
            Show results
          </button>
        </div>
      )}
    </div>
  );
};

export default Analyze;
