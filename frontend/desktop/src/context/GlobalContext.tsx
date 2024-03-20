import React, { createContext, useContext, useState } from "react";

interface GlobalContextType {
  path: string;
  setPath: (path: string) => void;
  stage: StageProps;
  setStage: (stage: StageProps) => void;
}

export enum StageProps {
  SELECT = "SELECT",
  ANALYZE = "ANALYZE",
  RESULTS = "RESULTS",
  ERROR = "ERROR",
}

const GlobalContext = createContext<GlobalContextType | undefined>(undefined);

export const useGlobalContext = () => {
  const context = useContext(GlobalContext);
  if (!context) {
    throw new Error("useGlobalContext must be used within a GlobalProvider");
  }
  return context;
};

export const GlobalProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [path, setPath] = useState("");
  const [stage, setStage] = useState(StageProps.SELECT);
  return (
    <GlobalContext.Provider value={{ path, setPath, stage, setStage }}>
      {children}
    </GlobalContext.Provider>
  );
};
