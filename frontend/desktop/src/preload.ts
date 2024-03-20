// See the Electron documentation for details on how to use preload scripts:
// https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts
import { contextBridge, ipcRenderer } from 'electron';

interface ElectronAPI {
    invoke: (channel: string, ...args: any[]) => Promise<any>;
  }
  
  declare global {
    interface Window {
      electron: ElectronAPI;
    }
  }


contextBridge.exposeInMainWorld('electron', {
  invoke: (channel: string, ...args: any[]): Promise<any> => ipcRenderer.invoke(channel, ...args),
});
