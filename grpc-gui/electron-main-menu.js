const {app, BrowserWindow, Menu, dialog} = require('electron')
const { createWindow, getLastFocussedWindow } = require('./electron-window-management.js')

function sendToFocussedWindow() {
  const focussedWindow = BrowserWindow.getFocusedWindow() || getLastFocussedWindow()
  if(focussedWindow) {
    focussedWindow.webContents.send(...arguments)
  }
}

const menuTemplate = [
  {
    label: 'Main',
    submenu: [
      {
        label: 'New window',
        accelerator: 'CmdOrCtrl+N',
        click () {
          createWindow()
        }
      },
      {
        label: 'Reset workspace',
        accelerator: 'CmdOrCtrl+R',
        click () {
          sendToFocussedWindow('reset-workspace')
        }
      },
      {
        label: 'Add path(s) to workspace',
        accelerator: 'CmdOrCtrl+O',
        click () {
          dialog.showOpenDialog({
            properties: ['openDirectory']
          }, function (paths) {
            if (paths) sendToFocussedWindow('added-workspace-paths', paths)
          })
        }
      },
      {type: 'separator'},
      {role: 'quit'}
    ]
  },
  {
    label: 'Service',
    submenu: [
      {
        label: 'Invoke method',
        accelerator: 'CmdOrCtrl+Alt+Enter',
        click() {
          sendToFocussedWindow('invoke-service-method')
        }
      },
      {
        label: 'Cancel method',
        accelerator: 'CmdOrCtrl+Alt+Backspace',
        click() {
          sendToFocussedWindow('cancel-service-method')
        }
      },
      {type: 'separator'},
      {
        label: 'Regenerate request body',
        accelerator: 'CmdOrCtrl+Alt+G',
        click() {
          sendToFocussedWindow('generate-request-body')
        }
      },
      {
        label: 'Load last sent request body',
        accelerator: 'CmdOrCtrl+Alt+L',
        click() {
          sendToFocussedWindow('load-last-request-body')
        }
      },
      {type: 'separator'},
      {
        label: 'Next method',
        accelerator: 'CmdOrCtrl+Tab',
        click() {
          sendToFocussedWindow('next-service-method')
        }
      },
      {
        label: 'Previous method',
        accelerator: 'CmdOrCtrl+Shift+Tab',
        click() {
          sendToFocussedWindow('previous-service-method')
        }
      },
      {
        label: 'Find method',
        accelerator: 'CmdOrCtrl+Shift+F',
        click() {
          sendToFocussedWindow('find-service-method')
        }
      }
    ]
  },
  {
    label: 'View',
    submenu: [
      {role: 'reload'},
      {role: 'forcereload'},
      {role: 'toggledevtools'},
      {type: 'separator'},
      {role: 'resetzoom'},
      {role: 'zoomin'},
      {role: 'zoomout'},
      {type: 'separator'},
      {role: 'togglefullscreen'}
    ]
  },
  {
    role: 'window',
    submenu: [
      {role: 'minimize'},
      {role: 'close'}
    ]
  }
]

app.on('ready', ()=> {
const menu = Menu.buildFromTemplate(menuTemplate)
Menu.setApplicationMenu(menu)
})
