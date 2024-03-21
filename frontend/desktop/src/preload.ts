// See the Electron documentation for details on how to use preload scripts:
// https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts
import { contextBridge, ipcRenderer } from "electron";

interface ElectronAPI {
  invoke: (channel: string, ...args: any[]) => Promise<any>;
  on: (channel: string, func: (...args: any[]) => void) => void;
}

declare global {
  interface Window {
    electron: ElectronAPI;
  }
}

contextBridge.exposeInMainWorld("electron", {
  invoke: (channel: string, ...args: any[]): Promise<any> =>
    ipcRenderer.invoke(channel, ...args),
  on: (channel: string, func: (...args: any[]) => void) => {
    ipcRenderer.on(channel, (event, ...args) => func(args));
  },
});
