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
