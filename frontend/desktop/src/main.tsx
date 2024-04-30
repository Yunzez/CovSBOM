import React, { useState, ChangeEvent, useEffect } from "react";
import ipcRenderer from "electron";
import SelectFile from "./pages/SelectFile";
import {
  GlobalProvider,
  StageProps,
  useGlobalContext,
} from "./context/GlobalContext";
import Analyze from "./pages/Analyze";
import Results from "./pages/Results";
import Error from "./pages/Error";
interface DirectoryInputAttributes
  extends React.InputHTMLAttributes<HTMLInputElement> {
  directory?: string;
  webkitdirectory?: string;
}

function App() {


  return (
    <GlobalProvider>
      <Swicther />
    </GlobalProvider>
  );
}

const Swicther = () => {
  const { stage, setStage } = useGlobalContext();
  return (
    <>
      {stage == StageProps.SELECT && <SelectFile />}
      {stage == StageProps.ANALYZE && <Analyze />}
      {stage == StageProps.RESULTS && <Results />}
      {stage == StageProps.ERROR && <Error />}
    </>
  );
};

export default App;
