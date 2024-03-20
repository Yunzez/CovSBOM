import React from "react";
import { StageProps } from "../context/GlobalContext";
import { useGlobalContext } from "../context/GlobalContext";
const Results = () => {
  const { path, setPath, stage, setStage } = useGlobalContext();

  return (
    <button
    className="bg-slate-600 hover:bg-slate-500 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline mt-4"
      onClick={() => {
        setStage(StageProps.SELECT);
      }}
    >
      Select other projects
    </button>
  );
};

export default Results;
