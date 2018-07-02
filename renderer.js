// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protos = require('./protos.js')
const transport = require('./transport.js')

const dom = {
  serviceListing: document.querySelector('#service-listing'),
  fileName: document.querySelector('#file-name'),
  protoListing: document.querySelector('#proto-listing'),
  requestListing: document.querySelector('#request-listing'),
  responseTiming: document.querySelector('#response-timing'),
  responseListing: document.querySelector('#response-listing'),
  serverHost: document.querySelector('#server-host'),
  serverPort: document.querySelector('#server-port'),
  serverStatus: document.querySelector('#server-status'),
  serverDetails: document.querySelector('#server-details'),
}

const channelManager = new transport.ChannelManager(newChannel => {
  newChannel.on('connecting', () => {
    dom.serverDetails.setAttribute('data-state', 'connecting')
    dom.serverStatus.innerHTML = 'Connecting'
  })
  newChannel.on('connect', () => {
    dom.serverDetails.setAttribute('data-state', 'connected')
    dom.serverStatus.innerHTML = 'Connected'
  })
  newChannel.on('disconnect', () => {
    dom.serverDetails.setAttribute('data-state', 'disconnected')
    dom.serverStatus.innerHTML = 'Disconnected'
  })
})

function changeDirectory(path) {
  document.title = `GRPC GUI - ${path}`
  const messages = protos.loadDirectory(path)

  const messagesIndex = protos.makeFlatIndex(messages)

// TODO: split into service and message listings
  dom.serviceListing.innerHTML = '<ul class="list-group">'+
    Object.keys(messagesIndex.services).map(fqServiceName => {
      const service = messagesIndex.services[fqServiceName]
      return `<li class="service-listing-entry" style="cursor:pointer;padding-left:10px" data-fq-service-name="${fqServiceName}"><code>${fqServiceName}</code></li>`
    }).join('<hr style="margin-top:2px;margin-bottom:2px"/>')+
  '</ul>'

  const protoButtons = document.querySelectorAll('#service-listing .service-listing-entry')
  protoButtons.forEach(protoButton => {
    protoButton.addEventListener('click', event => {
      const fqServiceName = protoButton.attributes["data-fq-service-name"].value

      const service = messagesIndex.services[fqServiceName]
      const serviceId = fqServiceName.replace(/\./gi, '-')
      const serviceDescription = protos.describeServiceMethods(service)
      dom.fileName.innerHTML = service.filename
      dom.protoListing.innerHTML =
        // `<div id="${serviceId}">` +
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
              host: dom.serverHost.value,
              port: dom.serverPort.value,
              service: fqServiceName,
              method: methodKey,
              body: JSON.parse(editor.getValue())
            }
            dom.requestListing.innerHTML = '<pre>'+JSON.stringify(request, undefined, '  ')+'</pre>'
            console.log('Request', request)
            dom.responseListing.innerHTML = '';
            dom.responseTiming.innerHTML = '<p>Running</p>'
            const t0 = performance.now();
            const channel = channelManager.getChannel(request.host, request.port)
            const responseStream = method
              .invokeWith(channel, request.body)
            const responses = []
            let responseDone = false
            let responseCount = 0
            const responseRenderInterval = setInterval(() => {
              if(!responses.length) {
                return
              }

              var fragment = document.createDocumentFragment()
              while(responses.length) {
                const response = responses.shift()
                const responseElement = document.createElement('pre')
                responseElement.innerText = JSON.stringify(response, undefined, '  ')
                fragment.appendChild(responseElement)
                fragment.appendChild(document.createElement('hr'))
              }
              dom.responseListing.appendChild(fragment)
              if(responseDone) {
                clearInterval(responseRenderInterval)
              }
            }, 300)
            responseStream.on('data', response => {
              responses.push(response)
              responseCount++
            })
            responseStream.on('end', () => {
              const t1 = performance.now()
              const duration = t1-t0
              console.log('Response completed in', duration)
              console.log('Response count', responseCount)
              dom.responseTiming.innerHTML = `<p>Call duration ${duration.toFixed(3)} milliseconds</p>`
              responseDone = true
            })
            responseStream.on('error', error => {
              const t1 = performance.now()
              const duration = t1-t0
              dom.responseTiming.innerHTML = `<p>Call duration ${duration.toFixed(3)} milliseconds</p>`
              dom.responseListing.innerHTML = `<div class="alert alert-danger" role="alert">Error<hr/><pre>${JSON.stringify(error, undefined, '  ')}</pre></div>`
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
