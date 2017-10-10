// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const ProtoBuf = require("protobufjs")
const grpc = require('grpc')

const dirSelect = document.querySelector('#dir-select')
const dirDisplay = document.querySelector('#dir-display')
const dirListing = document.querySelector('#dir-listing')
const fileListing = document.querySelector('#file-listing')
const protoListing = document.querySelector('#proto-listing') // todo: remove, temp

dirSelect.addEventListener('click', event => {
  ipc.send('open-directory-dialog')
})

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  dirDisplay.innerHTML = `${path}`
  dirListing.innerHTML = '<ul class="proto-file-listing">'+
    fsLib.readdirSync(path).map(file => `<li><button>${file}</button></li>`) +
  '</ul>'

  const fileButtons = document.querySelectorAll('.proto-file-listing li button')
  fileButtons.forEach(fileButton => {
    fileButton.addEventListener('click', event => {
      const protoPath = pathLib.join(path, fileButton.innerHTML)
      const protoDefinition = fsLib.readFileSync(protoPath).toString()
      fileListing.innerHTML = `<pre>${protoDefinition}</pre>`
      // const protoBuilder = ProtoBuf.loadProtoFile({root:path,file:fileButton.innerHTML})
      // console.log(protoBuilder)
      // console.log(protoBuilder.build())
      try {
        console.log(grpc.load({root:path,file:fileButton.innerHTML}))
      } catch (e) {
        // TODO: Make this a better error message
        alert(e.toString())
      }

    })
  })
})

ipc.on('proto-file-keys', (event, keys) => {
  protoListing.innerHTML = keys.join('<br />')
})
