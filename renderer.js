// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const grpc = require('grpc')

const dirSelect = document.querySelector('#dir-select')
const dirName = document.querySelector('#dir-name')
const dirListing = document.querySelector('#dir-listing')
const fileName = document.querySelector('#file-name')
const protoListing = document.querySelector('#proto-listing')

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
      let protoModel = ''
      try {
        protoModel = grpc.load({root:path,file:fileButton.innerHTML})
      } catch (e) {
        // TODO: Make this a better error message
        alert(e.toString())
      }

      const services = describeServices(protoModel)
      protoListing.innerHTML = Object.keys(services).map(serviceKey => {
        const service = services[serviceKey]
        const serviceId = serviceKey.replace(/\./gi, '-')
        return `<div id="${serviceId}" class="service"><h3>${serviceKey}</h3>`+
                `${Object.keys(service).map(methodKey => {
                  const method = service[methodKey]
                  const methodId = methodKey.replace(/\./gi, '-')
                  return `<div id="${methodId}" class="method"><h4>${method.method}</h4>`+
                          `<div>${method.requestType}<textarea class="request-json">${JSON.stringify(method.requestSample, undefined, '  ')}</textarea></div>`+
                          `<button class="request-invoke">Invoke</button>`+
                        `</div>`
                }).join('')}`+
              `</div>`
      }).join('')

      Object.keys(services).map(serviceKey => {
        const service = services[serviceKey]
        const serviceId = serviceKey.replace(/\./gi, '-')
        Object.keys(service).map(methodKey => {
          const method = service[methodKey]
          const methodId = methodKey.replace(/\./gi, '-')
          document.querySelector(`#${serviceId} #${methodId} .request-invoke`).addEventListener('click', event => {
            const request = JSON.parse(document.querySelector(`#${serviceId} #${methodId} .request-json`).value)
            method
              .invokeRpc('127.0.0.1',12372,request)
              .then(response => {
                console.log(response)
              })
              .catch(error => {
                console.error(error.toString(), JSON.stringify(error))
              })
          })
        })
      })
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
