const {app, BrowserWindow} = require('electron')
const path = require('path')
const url = require('url')

// Keep a global reference to the window objects, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
var openWindows = []
var lastFocussedWindow = null

function getLastFocussedWindow() {
  return lastFocussedWindow;
}

function createWindow () {
  // Create the browser window.
  var thisWindow = new BrowserWindow({width: 800, height: 600, webPreferences: { nodeIntegration: true } })

  // and load the index.html of the app.
  thisWindow.loadURL(url.format({
    pathname: path.join(__dirname, 'index.html'),
    protocol: 'file:',
    slashes: true
  }))

  // Open the DevTools.
  // thisWindow.webContents.openDevTools()

  thisWindow.on('focus', function () {
    lastFocussedWindow = thisWindow
  })
  lastFocussedWindow = thisWindow

  // Emitted when the window is closed.
  thisWindow.on('closed', function () {
    openWindows = openWindows.filter(x => x !== thisWindow)
  })
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow)

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  // On OS X it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', function () {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (openWindows.length === 0) {
    createWindow()
  } else if(BrowserWindow.getFocusedWindow() === null) {
    if(lastFocussedWindow !== null) {
      lastFocussedWindow.focus()
    } else {
      openWindows[0].focus()
    }
  }
})

module.exports = {
  createWindow,
  getLastFocussedWindow,
}
