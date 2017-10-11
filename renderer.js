// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const ProtoBuf = require("protobufjs")
const grpc = require('grpc')

const dirSelect = document.querySelector('#dir-select')
const dirName = document.querySelector('#dir-name')
const dirListing = document.querySelector('#dir-listing')
const fileName = document.querySelector('#file-name')
const protoListing = document.querySelector('#proto-listing')

const scope = {};

dirSelect.addEventListener('click', event => {
  ipc.send('open-directory-dialog')
})

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  dirName.innerHTML = `${path}`
  dirListing.innerHTML = fsLib.readdirSync(path).map(file => `<div class="dir-listing-entry">${file}</div>`).join('')

  const fileButtons = document.querySelectorAll('#dir-listing .dir-listing-entry')
  fileButtons.forEach(fileButton => {
    fileButton.addEventListener('click', event => {
      fileName.innerHTML = fileButton.innerHTML
      const protoPath = pathLib.join(path, fileButton.innerHTML)
      const protoDefinition = fsLib.readFileSync(protoPath).toString()
      fileListing.innerHTML = `<pre>${protoDefinition}</pre>`
      // const protoBuilder = ProtoBuf.loadProtoFile({root:path,file:fileButton.innerHTML})
      // console.log(protoBuilder)
      // console.log(protoBuilder.build())
      let protoModel = ''
      try {
        protoModel = grpc.load({root:path,file:fileButton.innerHTML})
      } catch (e) {
        // TODO: Make this a better error message
        alert(e.toString())
      }

      protoListing.innerHTML = `<pre>${JSON.stringify(describeServices(protoModel), undefined, '  ')}</pre>`

      scope.protoModel = protoModel;
      scope.described = describeServices(protoModel)
      console.log(scope)
    })
  })
})

function describeServices(protoModel, namespace=[]) {
  return Object.assign(...Object.keys(protoModel).map(key => {
    const child = protoModel[key]
    const childNamespace = namespace.concat(key)
    if(typeof(child) === "function" && child.name === "ServiceClient") {
      return { [childNamespace.join('.')]: describeServiceMethods(child) }
    }
    if(typeof(child) === "object" && !child.hasOwnProperty('$options')) {
      return describeServices(child, childNamespace)
    }
    return {}
  }))
}

function describeServiceMethods(protoService) {
  return Object.assign(...Object.keys(protoService.service).map(serviceMethodKey => {
    const serviceMethod = protoService.service[serviceMethodKey]
    return {
      [serviceMethodKey]: {
        method: serviceMethod.originalName,
        requestType: serviceMethod.requestType.name,
        requestSample: makeFullySpecifiedJsonSample(serviceMethod.requestType),
        responseType: serviceMethod.responseType.name,
        responseSample: makeFullySpecifiedJsonSample(serviceMethod.responseType),
        makeCallAsync: (host, port, request) => { // here's a function which will call the service! yay
          const service = new protoService(`${host}:${port}`, grpc.credentials.createInsecure())
          return new Promise((resolve,reject) => {
            service[serviceMethodKey](request, (err, response) => {
              if(err) {
                reject(err)
              } else {
                resolve(response)
              }
            })
          })
        }
      }
    }
  }))
}

function makeFullySpecifiedJsonSample(messageType) {
  return Object.assign(...messageType._fields.map(field => {
    const wrap = field.repeated ?
      (val => { return { [field.name]: [ val ] } }) :
      (val => { return { [field.name]: val } })
    switch(field.type.name) {
      case "bool":
        return wrap(false)
      case "string":
        return wrap(`${field.name} ${Math.ceil(1000*Math.random())}`)
      case "bytes":
        return wrap(btoa(`${field.name} ${Math.ceil(1000*Math.random())}`))
      case "enum":
        return wrap(Object.keys(field.resolvedType.object).join('|'))

      case "group":
      case "message":
        return wrap(makeFullySpecifiedJsonSample(field.resolvedType))

      // everything else is a number :)
      default:
        return wrap(Math.ceil(1000*Math.random()))
    }
  }))
}

ipc.on('proto-file-keys', (event, keys) => {
  protoListing.innerHTML = keys.join('<br />')
})
