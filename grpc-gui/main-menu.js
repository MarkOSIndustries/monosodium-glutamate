const {app, Menu, dialog} = require('electron')

const menuTemplate = [
  {
    label: 'File',
    submenu: [
      {
        label: 'New workspace',
        accelerator: 'CmdOrCtrl+N',
        click () {
          mainWindow.webContents.send('new-workspace')
        }
      },
      {
        label: 'Add path(s) to workspace',
        accelerator: 'CmdOrCtrl+O',
        click () {
          dialog.showOpenDialog({
            properties: ['openDirectory']
          }, function (paths) {
            if (paths) mainWindow.webContents.send('added-workspace-paths', paths)
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
          mainWindow.webContents.send('invoke-service-method')
        }
      },
      {
        label: 'Cancel method',
        accelerator: 'CmdOrCtrl+Alt+Backspace',
        click() {
          mainWindow.webContents.send('cancel-service-method')
        }
      },
      {type: 'separator'},
      {
        label: 'Regenerate request body',
        accelerator: 'CmdOrCtrl+Alt+G',
        click() {
          mainWindow.webContents.send('generate-request-body')
        }
      },
      {
        label: 'Load last sent request body',
        accelerator: 'CmdOrCtrl+Alt+L',
        click() {
          mainWindow.webContents.send('load-last-request-body')
        }
      },
      {type: 'separator'},
      {
        label: 'Next method',
        accelerator: 'CmdOrCtrl+Tab',
        click() {
          mainWindow.webContents.send('next-service-method')
        }
      },
      {
        label: 'Previous method',
        accelerator: 'CmdOrCtrl+Shift+Tab',
        click() {
          mainWindow.webContents.send('previous-service-method')
        }
      },
      {
        label: 'Find method',
        accelerator: 'CmdOrCtrl+Shift+F',
        click() {
          mainWindow.webContents.send('find-service-method')
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
