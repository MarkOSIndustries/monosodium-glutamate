// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const grpc = require('grpc')

const dirSelect = document.querySelector('#dir-select')
const dirName = document.querySelector('#dir-name')
const dirListing = document.querySelector('#dir-listing')
const fileName = document.querySelector('#file-name')
const protoListing = document.querySelector('#proto-listing')
const requestListing = document.querySelector('#request-listing')
const responseListing = document.querySelector('#response-listing')
const serverHost = document.querySelector('#server-host')
const serverPort = document.querySelector('#server-port')

dirSelect.addEventListener('click', event => {
  ipc.send('open-directory-dialog')
})

function changeDirectory(path) {
  document.title = `GRPC GUI - ${path}`
  dirListing.innerHTML = '<ul class="list-group">'+
    '<li class="dir-listing-entry list" style="cursor:pointer;padding-left:10px" data-filename="..">..\\</li><hr style="margin:0px"/>'+
    fsLib.readdirSync(path).map(file => `<li class="dir-listing-entry" style="cursor:pointer;padding-left:10px" data-filename="${file}">${file}</li>`).join('<hr style="margin-top:2px;margin-bottom:2px"/>')+
  '</ul>'

  const fileButtons = document.querySelectorAll('#dir-listing .dir-listing-entry')
  fileButtons.forEach(fileButton => {
    fileButton.addEventListener('click', event => {
      const filename = fileButton.attributes["data-filename"].value
      const filepath = pathLib.join(path, filename)
      if(fsLib.statSync(filepath).isDirectory()) {
        return changeDirectory(filepath)
      }

      fileName.innerHTML = filename
      protoListing.innerHTML = ''
      let protoModel = ''
      try {
        protoModel = grpc.load({root:path,file:filename})
      } catch (e) {
        // TODO: Make this a better error message
        alert(e.toString())
      }

      const services = describeServices(protoModel)
      protoListing.innerHTML = Object.keys(services).map(serviceKey => {
        const service = services[serviceKey]
        const serviceId = serviceKey.replace(/\./gi, '-')
        return `<div id="${serviceId}" class="service"><h4>${serviceKey}</h4>`+
                Object.keys(service).map(methodKey => {
                  const method = service[methodKey]
                  const methodId = methodKey.replace(/\./gi, '-')
                  return `<div id="${methodId}" class="method"><h5><code>${method.method}(<var>${method.requestType}</var>) => <var>${method.responseType}</var></code></h5>`+
                          `<div class="form-group">`+
                            `<div class="request-json" style="width:100%;height:45em"></div>`+
                            `<span class="input-group-btn">`+
                              `<button class="request-invoke btn btn-primary" type="button">Invoke</button>`+
                            `</span>`+
                          `</div>`+
                        `</div>`
                }).join('<hr/>')+
              `</div>`
      }).join('')

      Object.keys(services).map(serviceKey => {
        const service = services[serviceKey]
        const serviceId = serviceKey.replace(/\./gi, '-')
        Object.keys(service).map(methodKey => {
          const method = service[methodKey]
          const methodId = methodKey.replace(/\./gi, '-')

          require('monaco-loader')().then(monaco => {
            const editor = monaco.editor.create(document.querySelector(`#${serviceId} #${methodId} .request-json`), {
              language: 'json',
              theme: 'vs-light',
              automaticLayout: true,
              value: JSON.stringify(method.requestSample, undefined, '  ')
            })

            document.querySelector(`#${serviceId} #${methodId} .request-invoke`).addEventListener('click', event => {
              const request = {
                host: serverHost.value,
                port: serverPort.value,
                service: serviceKey,
                method: methodKey,
                body: JSON.parse(editor.getValue())
              }
              requestListing.innerHTML = '<pre>'+JSON.stringify(request, undefined, '  ')+'</pre>'
              console.log('Request', request)
              method
                .invokeRpc(request.host, request.port, request.body)
                .then(response => {
                  responseListing.innerHTML = `<div class="alert alert-success" role="alert"><pre>${JSON.stringify(response, undefined, '  ')}</pre></div>`
                  console.log('Response', response)
                })
                .catch(error => {
                  responseListing.innerHTML = `<div class="alert alert-danger" role="alert">${error.toString()}<hr/><pre>${JSON.stringify(error, undefined, '  ')}</pre></div>`
                  console.error('Error', error.toString(), JSON.stringify(error))
                })
            })
          })
        })
      })
    })
  })
}

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  changeDirectory(path)
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
        invokeRpc: (host, port, request) => { // here's a function which will call the service! yay
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
