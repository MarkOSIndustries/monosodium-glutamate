const grpc = require('grpc')
const ipc = require('electron').ipcMain

ipc.on('open-proto-file', function (event, path) {
  const protoObj = grpc.load(path)
  event.sender.send('proto-file-keys', Object.keys(protoObj))
})
