const {app, Menu, dialog} = require('electron')

const menuTemplate = [
  {
    label: 'File',
    submenu: [
      {
        label: 'Open directory',
        accelerator: 'CmdOrCtrl+O',
        click () {
          dialog.showOpenDialog({
            properties: ['openDirectory']
          }, function (files) {
            if (files) mainWindow.webContents.send('selected-directory', files)
          })
        }
      },
      {type: 'separator'},
      {role: 'quit'}
    ]
  },
  {
    label: 'Query',
    submenu: [
      {
        label: 'Run query',
        accelerator: 'CmdOrCtrl+Alt+Enter',
        click() {
          mainWindow.webContents.send('run-query')
        }
      },
      {
        label: 'Select topic by schema name',
        accelerator: 'CmdOrCtrl+Alt+T',
        click() {
          mainWindow.webContents.send('select-matching-topic')
        }
      },
      {type: 'separator'},
      {
        label: 'Next schema',
        accelerator: 'CmdOrCtrl+Tab',
        click() {
          mainWindow.webContents.send('next-schema')
        }
      },
      {
        label: 'Previous schema',
        accelerator: 'CmdOrCtrl+Shift+Tab',
        click() {
          mainWindow.webContents.send('previous-schema')
        }
      },
      {
        label: 'Find schema',
        accelerator: 'CmdOrCtrl+Shift+F',
        click() {
          mainWindow.webContents.send('find-schema')
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
