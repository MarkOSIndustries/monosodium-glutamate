// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
const ipc = require('electron').ipcRenderer
const fsLib = require('fs')
const pathLib = require('path')
const protobuf = require('../protobuf')(require('protobufjs'))
const transport = require('../grpc.transport')
const { SchemaConverter } = require('../protobuf.convert')

const dom = {
  methodSearch: document.querySelector('#method-search'),
  methodListing: document.querySelector('#method-listing'),
  methodDetails: document.querySelector('#method-details'),
  requestListing: document.querySelector('#request-listing'),
  responseTiming: document.querySelector('#response-timing'),
  responseListing: document.querySelector('#response-listing'),
  responseOutcome: document.querySelector('#response-outcome'),
  serverHost: document.querySelector('#server-host'),
  serverPort: document.querySelector('#server-port'),
  serverStatus: document.querySelector('#server-status'),
  serverDetails: document.querySelector('#server-area'),
}

const selected = {
  service: null,
  serviceName: '',
  method: null,
  methodName: '',
  methodCount: 0,
}

const globals = {
  requestEditor: {},
  channelManager: new transport.ChannelManager(newChannel => {
    newChannel.on('connecting', channel => {
      dom.serverDetails.setAttribute('data-state', 'connecting')
      dom.serverStatus.innerHTML = `Connecting to ${channel.address}`
    })
    newChannel.on('connect', channel => {
      dom.serverDetails.setAttribute('data-state', 'connected')
      dom.serverStatus.innerHTML = `Connected to ${channel.address}`
      localStorage.setItem('last-connected-host', channel.host)
      localStorage.setItem('last-connected-port', channel.port)
    })
    newChannel.on('disconnect', channel => {
      dom.serverDetails.setAttribute('data-state', 'disconnected')
      dom.serverStatus.innerHTML = 'Disconnected'
    })
  }),
  loadSampleRequest: () => {
    alert('Please select a service method first');
  },
  loadLastSentRequest: () => {
    alert('Please select a service method first');
  }
}

dom.serverHost.value = localStorage.getItem('last-connected-host')
dom.serverPort.value = localStorage.getItem('last-connected-port')

function searchUpdated() {
  document.querySelectorAll('#method-listing .method-listing-entry').forEach(methodButton => {
    const fqServiceName = methodButton.attributes["data-fq-service-name"].value
    const methodName = methodButton.attributes["data-method-name"].value
    const searchRegex = new RegExp(dom.methodSearch.value, "i")
    if(dom.methodSearch.value == '' || -1 < fqServiceName.search(searchRegex) || -1 < methodName.search(searchRegex)) {
      methodButton.classList.remove('hidden')
    } else {
      methodButton.classList.add('hidden')
    }
  })
}

dom.methodSearch.addEventListener('keydown', event => {
  var key = event.which || event.keyCode;
  switch(key) {
    case 13: // Enter
    case 27: // Esc
      dom.methodSearch.value = ''
      globals.requestEditor.focus()
      return event.preventDefault()
    case 38: // Up
      previousServiceMethod()
      return event.preventDefault()
    case 40: // Down
      nextServiceMethod()
      return event.preventDefault()
    default:
      setTimeout(() => searchUpdated(), 0)
  }
})
dom.methodSearch.addEventListener('blur', event => {
  searchUpdated()
})

require('monaco-loader')().then(monaco => {
  globals.requestEditor = monaco.editor.create(document.querySelector(`.request-json`), {
    value: '{}',
    language: 'json',
    theme: 'vs-light',
    automaticLayout: true,
    scrollBeyondLastLine: false,
    minimap: {
      enabled: false
    }
  })

  if(localStorage.getItem('last-selected-directory')) {
    changeDirectory(localStorage.getItem('last-selected-directory'));
  }
})

function changeDirectory(path) {
  document.title = `GRPC GUI - ${path}`
  const messages = protobuf.loadDirectory(path)

  const messagesIndex = protobuf.makeFlatIndex(messages)

  selected.methodCount = 0
// TODO: split into service and message listings
  dom.methodListing.innerHTML = '<ul class="entry-list">'+
    Object.keys(messagesIndex.services).map(fqServiceName => {
      const service = messagesIndex.services[fqServiceName]
      const serviceId = fqServiceName.replace(/\./gi, '-')
      const serviceDescription = protobuf.describeServiceMethods(service)

      return Object.keys(serviceDescription).map((methodName) => {
          const method = serviceDescription[methodName]
          const methodId = methodName.replace(/\./gi, '-')
          selected.methodCount = selected.methodCount+1
          return `<li id="${methodId}" class="method-listing-entry clickable-entry tab" data-fq-service-name="${fqServiceName}" data-method-name="${methodName}">`+
            `<div class="layout layout-row" style="justify-content:space-between;padding-right:0.2em">`+
              `<h4><code>${method.method}</code></h4>`+
              `<h6><code>${fqServiceName}</code></h6>`+
            `</div>`+
            `<h5><code><var>${method.requestTypeName}</var> ⇒ <var>${method.responseOf}</var> <var>${method.responseTypeName}</var></code></h5>`+
          `</li>`
        }).join('')
    }).join('')+
  '</ul>'

  const methodButtons = document.querySelectorAll('#method-listing .method-listing-entry')
  methodButtons.forEach(methodButton => {
    const fqServiceName = methodButton.attributes["data-fq-service-name"].value

    const service = messagesIndex.services[fqServiceName]
    const serviceId = fqServiceName.replace(/\./gi, '-')
    const serviceDescription = protobuf.describeServiceMethods(service)

    const methodName = methodButton.attributes["data-method-name"].value
    const method = serviceDescription[methodName]
    const methodId = methodName.replace(/\./gi, '-')

    methodButton.addEventListener('click', methodButtonClickEvent => {
      const lastSelectedMethodButton = document.querySelector('#method-listing .method-listing-entry.selected')
      if(lastSelectedMethodButton) {
        lastSelectedMethodButton.classList.remove('selected')
      }
      methodButton.classList.add('selected')

      selected.service = service
      selected.serviceName = fqServiceName
      selected.method = method
      selected.methodName = methodName

      globals.requestEditor.setValue(localStorage.getItem(`${selected.serviceName}-${selected.methodName}-request`) || JSON.stringify(selected.method.requestSample, undefined, '  '))
      globals.loadLastSentRequest = () => globals.requestEditor.setValue(localStorage.getItem(`${selected.serviceName}-${selected.methodName}-request`))
      globals.loadSampleRequest = () => globals.requestEditor.setValue(JSON.stringify(selected.method.requestSample, undefined, '  '))
    })
  })
  nextServiceMethod()
  globals.requestEditor.focus()
}

function invokeServiceMethod() {
  if(!selected.method) {
    return
  }
  const requestConverter = new SchemaConverter(selected.method.requestType)
  const responseConverter = new SchemaConverter(selected.method.responseType)

  localStorage.setItem(`${selected.serviceName}-${selected.methodName}-request`, globals.requestEditor.getValue())
  const request = {
    host: dom.serverHost.value,
    port: dom.serverPort.value,
    service: selected.serviceName,
    method: selected.methodName,
    body: JSON.parse(globals.requestEditor.getValue())
  }
  dom.requestListing.innerHTML = '<pre>'+JSON.stringify(request, undefined, '  ')+'</pre>'
  console.log('Request', request)
  dom.responseListing.innerHTML = ''
  dom.responseOutcome.innerHTML = 'Running'
  dom.responseTiming.innerHTML = '...'
  dom.responseListing.className = 'running'
  const t0 = performance.now()
  const channel = globals.channelManager.getChannel(request.host, request.port)
  const responseStream = selected.method.invokeWith(channel, requestConverter.json_object_to_schema_object(request.body))
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
    responses.push(responseConverter.schema_object_to_json_object(response))
    responseCount++
    dom.responseOutcome.innerHTML = 'Success'
    dom.responseListing.className = 'success'
  })
  responseStream.on('end', () => {
    const t1 = performance.now()
    const duration = t1-t0
    console.log('Response completed in', duration)
    console.log('Response count', responseCount)
    dom.responseTiming.innerHTML = `${duration.toFixed(3)} milliseconds`
    responseDone = true
  })
  responseStream.on('error', error => {
    dom.responseOutcome.innerHTML = 'Error'
    dom.responseListing.innerHTML = `<pre>${JSON.stringify(error, undefined, '  ')}</pre>`
    dom.responseListing.className = 'failure'
    console.error('Error', error)
  })
}

function previousServiceMethod() {
  let currentMethodButton = document.querySelector('#method-listing .method-listing-entry.selected')
  do {
    if(currentMethodButton) {
      currentMethodButton = currentMethodButton.previousElementSibling
    }
    if(!currentMethodButton) {
      currentMethodButton = document.querySelector('#method-listing .method-listing-entry:last-child')
    }
  } while(currentMethodButton && !currentMethodButton.classList.contains('selected') && (currentMethodButton.classList.contains('hidden') || !currentMethodButton.classList.contains('method-listing-entry')))

  if(currentMethodButton) {
    currentMethodButton.click()
  }
}

function nextServiceMethod() {
  let currentMethodButton = document.querySelector('#method-listing .method-listing-entry.selected')
  do {
    if(currentMethodButton) {
      currentMethodButton = currentMethodButton.nextElementSibling
    }
    if(!currentMethodButton) {
      currentMethodButton = document.querySelector('#method-listing .method-listing-entry:first-child')
    }
  } while(currentMethodButton && !currentMethodButton.classList.contains('selected') && (currentMethodButton.classList.contains('hidden') || !currentMethodButton.classList.contains('method-listing-entry')))

  if(currentMethodButton) {
    currentMethodButton.click()
  }
}

ipc.on('selected-directory', (event, paths) => {
  const [path] = paths
  changeDirectory(path)
  localStorage.setItem('last-selected-directory', path)
})
ipc.on('invoke-service-method', invokeServiceMethod)
ipc.on('next-service-method', () => {
  nextServiceMethod()
  globals.requestEditor.focus()
})
ipc.on('previous-service-method', () => {
  previousServiceMethod()
  globals.requestEditor.focus()
})
ipc.on('find-service-method', () => dom.methodSearch.focus())
ipc.on('generate-request-body', () => globals.loadSampleRequest())
ipc.on('load-last-request-body', () => globals.loadLastSentRequest())
