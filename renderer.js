// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protos = require('./protos.js')
const grpc = require('@grpc/grpc-js')

const dirSelect = document.querySelector('#dir-select')
const dirName = document.querySelector('#dir-name')
const dirListing = document.querySelector('#dir-listing')
const protoNameDOM = document.querySelector('#file-name')
const protoListing = document.querySelector('#proto-listing')
const requestListing = document.querySelector('#request-listing')
const responseTiming = document.querySelector('#response-timing')
const responseListing = document.querySelector('#response-listing')
const serverHost = document.querySelector('#server-host')
const serverPort = document.querySelector('#server-port')

function changeDirectory(path) {
  document.title = `GRPC GUI - ${path}`
  const messages = protos.loadDirectory(path)

  const messagesIndex = protos.makeFlatIndex(messages)

// TODO: split into service and message listings
  dirListing.innerHTML = '<ul class="list-group">'+
    Object.keys(messagesIndex.services).map(fqServiceName => `<li class="dir-listing-entry" style="cursor:pointer;padding-left:10px" data-fq-service-name="${fqServiceName}">${fqServiceName}</li>`).join('<hr style="margin-top:2px;margin-bottom:2px"/>')+
  '</ul>'

  const protoButtons = document.querySelectorAll('#dir-listing .dir-listing-entry')
  protoButtons.forEach(protoButton => {
    protoButton.addEventListener('click', event => {
      const fqServiceName = protoButton.attributes["data-fq-service-name"].value

      protoNameDOM.innerHTML = fqServiceName
      protoListing.innerHTML = ''

      const service = messagesIndex.services[fqServiceName]
      const client = messagesIndex.clients[fqServiceName]
      const serviceId = fqServiceName.replace(/\./gi, '-')
      const serviceDescription = protos.describeServiceMethods(service, client)

// TODO: up to here, above code is fairly solid IMO
      protoListing.innerHTML =
             `<div id="${serviceId}" class="service"><h4>${fqServiceName}</h4>`+
                Object.keys(serviceDescription).map(methodKey => {
                  const method = serviceDescription[methodKey]
                  const methodId = methodKey.replace(/\./gi, '-')
                  return `<div id="${methodId}" class="method"><h5><code>${method.method}(<var>${method.requestType}</var>) => <var>${method.responseOf}</var> <var>${method.responseType}</var></code></h5>`+
                          `<div class="form-group">`+
                            `<div class="request-json" style="height:30em"></div>`+
                            `<span class="input-group-btn">`+
                              `<button class="request-invoke btn btn-primary" type="button">Invoke</button>`+
                            `</span>`+
                          `</div>`+
                        `</div>`
                }).join('<hr/>')+
              `</div>`

      Object.keys(serviceDescription).map(methodKey => {
        const method = serviceDescription[methodKey]
        const methodId = methodKey.replace(/\./gi, '-')

        require('monaco-loader')().then(monaco => {
          const editor = monaco.editor.create(document.querySelector(`#${serviceId} #${methodId} .request-json`), {
            value: JSON.stringify(method.requestSample, undefined, '  '),
            language: 'json',
            theme: 'vs-light',
            automaticLayout: true,
            scrollBeyondLastLine: false,
            minimap: {
          		enabled: false
          	}
          })

          document.querySelector(`#${serviceId} #${methodId} .request-invoke`).addEventListener('click', event => {
            const request = {
              host: serverHost.value,
              port: serverPort.value,
              service: fqServiceName,
              method: methodKey,
              body: JSON.parse(editor.getValue())
            }
            requestListing.innerHTML = '<pre>'+JSON.stringify(request, undefined, '  ')+'</pre>'
            console.log('Request', request)
            responseListing.innerHTML = '';
            responseTiming.innerHTML = '<p>Running</p>'
            var t0 = performance.now();
            method
              .invokeRpc(request.host, request.port, request.body)
              .then(response => {
                var t1 = performance.now()
                responseTiming.innerHTML = `<p>gRPC call - completed in ${(t1-t0).toFixed(3)} milliseconds</p>`
                responseListing.innerHTML = `<div class="alert alert-success" role="alert"><pre>${JSON.stringify(response, undefined, '  ')}</pre></div>`
                console.log('Response', response)
              })
              .catch(error => {
                var t1 = performance.now()
                responseTiming.innerHTML = `<p>gRPC call - errored in ${(t1-t0).toFixed(3)} milliseconds</p>`
                responseListing.innerHTML = `<div class="alert alert-danger" role="alert">${error.toString()}<hr/><pre>${JSON.stringify(error, undefined, '  ')}</pre></div>`
                console.error('Error', error.toString(), JSON.stringify(error))
              })
          })
        })
      })
    })
  })
}

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  console.log('Selected dir', paths)
  changeDirectory(path)
})
