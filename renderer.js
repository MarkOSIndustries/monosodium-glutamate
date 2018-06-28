// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protos = require('./protos.js')
const transport = require('./transport.js')

const dirSelect = document.querySelector('#dir-select')
const dirName = document.querySelector('#dir-name')
const dirListing = document.querySelector('#dir-listing')
const fileNameDOM = document.querySelector('#file-name')
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
    Object.keys(messagesIndex.services).map(fqServiceName => `<li class="dir-listing-entry" style="cursor:pointer;padding-left:10px" data-fq-service-name="${fqServiceName}"><code>${fqServiceName}</code></li>`).join('<hr style="margin-top:2px;margin-bottom:2px"/>')+
  '</ul>'

  const protoButtons = document.querySelectorAll('#dir-listing .dir-listing-entry')
  protoButtons.forEach(protoButton => {
    protoButton.addEventListener('click', event => {
      const fqServiceName = protoButton.attributes["data-fq-service-name"].value


      const service = messagesIndex.services[fqServiceName]
      // const client = messagesIndex.clients[fqServiceName]
      const serviceId = fqServiceName.replace(/\./gi, '-')
      const serviceDescription = protos.describeServiceMethods(service)
      fileNameDOM.innerHTML = service.filename
      protoListing.innerHTML =
             `<div id="${serviceId}" class="service"><h4><code>${fqServiceName}</code></h4>`+
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
            const t0 = performance.now();
            const http2Channel = new transport.Http2Channel(request.host, request.port)
            const responses = []
            const responseStream = method
              .invokeWith(http2Channel, request.body)
            responseStream.on('data', response => {
              console.log('Response', response)
              responses.push(response)
              responseListing.innerHTML = `<div class="alert alert-success" role="alert">Responses<hr/><pre>${JSON.stringify(responses, undefined, '  ')}</pre></div>`
            })
            responseStream.on('end', () => {
              const t1 = performance.now()
              const duration = t1-t0
              console.log('Response completed in', duration)
              responseTiming.innerHTML = `<p>Call duration ${duration.toFixed(3)} milliseconds</p>`
            })
            responseStream.on('error', error => {
              const t1 = performance.now()
              const duration = t1-t0
              responseTiming.innerHTML = `<p>Call duration ${duration.toFixed(3)} milliseconds</p>`
              responseListing.innerHTML = `<div class="alert alert-danger" role="alert">Error<hr/><pre>${JSON.stringify(error, undefined, '  ')}</pre></div>`
              console.error('Error', error)
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
