import React, { useEffect, useMemo, useState } from "react";
import { AnimatedTree, Data } from "react-tree-graph";
import { Tree } from "react-d3-tree";
import "../styles/customTree.css";
import 'react-tooltip/dist/react-tooltip.css'
import "react-tree-graph/dist/style.css";
// import ReactTooltip from 'react-tooltip';
import { Tooltip } from 'react-tooltip';
interface DeclarationInfo {
  sourceFilePath: string;
  declarationStartLine: number;
  declarationEndLine: number;
  methodName: string;
  declarationSignature: string;
  innerMethodCalls: MethodCallEntry[]; // Array of MethodCallEntry, supporting nested method calls
}

export interface MethodCallEntry {
  declaringType: string;
  methodSignature: string;
  methodName: string;
  lineNumber: string[]; // Line numbers as array of strings
  fullExpression: string;
  currentLayer: number;
  declarationInfo: DeclarationInfo; // Nested declaration information
}

interface TreeProps {
  data: {
    dependencyInfo: string;
    methodCalls: MethodCallEntry[];
  };
}

const TreeGraph: React.FC<TreeProps> = ({ data }) => {
  const { dependencyInfo, methodCalls } = data;
  const [loading, setLoading] = useState(true);
  const [tooltipContent, setTooltipContent] = useState('');
  const [tooltipId, setTooltipId] = useState('');
  console.log(dependencyInfo, methodCalls);

  const infoParts = dependencyInfo.split(", ");
  const groupId = infoParts
    .find((part) => part.startsWith("groupId="))
    ?.split("=")[1];
  const artifactId = infoParts
    .find((part) => part.startsWith("artifactId="))
    ?.split("=")[1];

    // const handleNodeMouseOver = (node) => {
    //     setTooltipContent(node.attributes.fullName); // Set full method name as tooltip content
    //     setTooltipId('tree-tooltip'); // Use a consistent ID for your single tooltip
    //     ReactTooltip.show(document.getElementById(node.attributes.tooltipId)); // Show tooltip manually
    //   };
    
    //   const handleNodeMouseOut = () => {
    //     ReactTooltip.hide();
    //   };

  const transformToTreeData = (
    methodCalls: MethodCallEntry[],
    level = 0,
    counts = { entries: 0, layers: level }
  ) => {
    return methodCalls.map((call, index) => {
      counts.entries++;
      const truncatedName = call.methodName.length > 20 ? `${call.methodName.substring(0, 17)}...` : call.methodName; // Truncate names longer than 30 characters
      const node = {
        name: `${truncatedName}`,
        children: [] as any[],
        tooltipId: `tooltip-${index}-${level}`,
        attributes: {
            fullName: call.methodName,
          },
      };

      if (
        call.declarationInfo &&
        call.declarationInfo.innerMethodCalls.length > 0
      ) {
        node.children = transformToTreeData(
          call.declarationInfo.innerMethodCalls,
          level + 1,
          counts
        );
        if (level + 1 > counts.layers) counts.layers = level + 1; // Update max depth
      }

      return node;
    });
  };

  const { treeData, counts } = useMemo(() => {
    const counts = { entries: 0, layers: 0 };
    const nodes = transformToTreeData(methodCalls, 0, counts);
    setLoading(false);
    return {
      treeData: { name: "", children: nodes },
      counts,
    };
  }, [methodCalls, dependencyInfo]);

  return (
    <>
      {" "}
      {loading && <div>Loading...</div>}
      {!loading && (
        <div
          id="treeWrapper"
          style={{ width: "100%", height: "100%", overflow: "auto" }}
        >
          <text dx="5" dy="15">
            <small> groupId: {groupId} </small>
            <small> artifactId: {artifactId} </small>
          </text>
          <AnimatedTree
            data={treeData}
            height={counts.entries * 30} // Dynamic height based on entries
            width={counts.layers * 180} // Dynamic width based on depth
            margins={{ top: 20, bottom: 10, left: 20, right: 120 }}
            svgProps={{
              className: "customTree",
            }}
            textProps={
              {
                dx: "1em",
                dy: ".35em",
                fontSize: "0.65em",
                fontWeight: "",
                transform: "translate(-15, -15)",
                style: { fill: "black", stroke: "none" },
              } as React.SVGProps<SVGTextElement>
            }
            // pathFunc="step"
          />
          {...treeData.children.map((node) => (
                <Tooltip id={node.tooltipId} place="top">
                    {node.name}
                </Tooltip>
            ))}
        </div>
      )}
    </>
  );
};

export default TreeGraph;
