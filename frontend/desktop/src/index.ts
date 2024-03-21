import { app, BrowserWindow, dialog, ipcMain } from "electron";
// This allows TypeScript to pick up the magic constants that's auto-generated by Forge's Webpack
// plugin that tells the Electron app where to look for the Webpack-bundled app code (depending on
// whether you're running in development or production).
declare const MAIN_WINDOW_WEBPACK_ENTRY: string;
declare const MAIN_WINDOW_PRELOAD_WEBPACK_ENTRY: string;
// import spawn from "child_process";
import { spawn } from "child_process";
// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require("electron-squirrel-startup")) {
  app.quit();
}

const createWindow = (): void => {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    height: 900,
    width: 1600,
    webPreferences: {
      preload: MAIN_WINDOW_PRELOAD_WEBPACK_ENTRY,
      contextIsolation: true,
    },
  });

  // and load the index.html of the app.
  mainWindow.loadURL(MAIN_WINDOW_WEBPACK_ENTRY);

  // Open the DevTools.
  mainWindow.webContents.openDevTools();

  // Handle directory selection request
  ipcMain.handle("open-directory-dialog", async (event) => {
    const result = await dialog.showOpenDialog({
      properties: ["openDirectory"],
    });
    if (result.filePaths.length > 0) {
      return result.filePaths[0];
    } else {
      return null;
    }
  });

  ipcMain.handle("run-java-program", async (event, rootPath) => {
    console.log("Running Java program");
    // Replace 'YourJavaProgram' and 'arg1', 'arg2' with your actual program and its arguments
    const javaProcess = spawn("java", [
      "-jar",
      "./ast_generator-1.0-SNAPSHOT.jar", // Adjusted path
      "--process-directory",
      rootPath,
  ]);
  

    javaProcess.stdout.on("data", (data) => {
      console.log(`stdout: ${data}`);
      event.sender.send("java-log", data.toString()); // Send log to renderer
    });

    javaProcess.stderr.on("data", (data) => {
      console.error(`stderr: ${data}`);
      event.sender.send("java-log", data.toString()); // Send error log to renderer
    });

    return new Promise((resolve) => {
      javaProcess.on("close", (code) => {
        console.log(`Java program exited with code ${code}`);
        resolve(code);
      });
    });
  });
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on("ready", createWindow);

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("activate", () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and import them here.
