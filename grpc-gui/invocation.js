const transport = require('../grpc.transport')
const { SchemaConverter } = require('../protobuf.convert')

module.exports = function(channels) {
  const cmd = {
    invoke: () => {
      if(!state.method || state.inFlight) return
      channels.invocation.subject('started').next(state.requestPayload)
    },
    cancel: () => {
      if(!state.inFlight) return
      channels.invocation.subject('cancelled').next()
    }
  }

  const dom = {
    serverHost: document.querySelector('#server-host'),
    serverPort: document.querySelector('#server-port'),
    serverArea: document.querySelector('#server-area'),
    serverStatus: document.querySelector('#server-status'),
    invokeButton: document.querySelector('#invoke-method'),
    cancelButton: document.querySelector('#cancel-method'),
  }

  const state = {
    requestPayload: '{}',
    method: null,
    inFlight: false,
    responseStream: null,
  }

  dom.serverHost.value = localStorage.getItem('last-connected-host')
  dom.serverPort.value = localStorage.getItem('last-connected-port')
  dom.invokeButton.addEventListener('click', cmd.invoke)
  dom.cancelButton.addEventListener('click', cmd.cancel)

  const channelManager = new transport.ChannelManager(channel => {
    ['connected', 'connecting', 'disconnected'].forEach(channelState => {
      channel.on(channelState, () => channels.invocation.subject('channel.state.changed').next({ channelState, channel }))
    })
  })

  channels.invocation.subject('channel.state.changed').subscribe(({channelState, channel}) => {
    dom.serverArea.setAttribute('data-state', channelState)
    dom.serverStatus.innerHTML = {
      connected:`Connected to ${channel.address}`,
      connecting:`Connecting to ${channel.address}`,
      disconnected:'Disconnected'
    }[channelState]
    localStorage.setItem('last-connected-host', channel.host)
    localStorage.setItem('last-connected-port', channel.port)
    if(state.inFlight) {
      channels.invocation.subject('terminated').next()
    }
  })

  channels.services.subject('method.selection.changed').subscribe(method => {
    state.method = method
  })
  channels.request.subject('payload.changed').subscribe(requestPayload => {
    state.requestPayload = requestPayload
  })

  channels.invocation.subject('started').subscribe(requestPayload => {
    state.inFlight = true

    dom.invokeButton.setAttribute('disabled', true)
    dom.cancelButton.removeAttribute('disabled')

    const request = {
      host: dom.serverHost.value,
      port: dom.serverPort.value,
      service: state.method.serviceName,
      method: state.method.methodName,
      body: JSON.parse(requestPayload),
      sent: Date.now(),
    }

    const requestConverter = new SchemaConverter(state.method.requestType)
    const responseConverter = new SchemaConverter(state.method.responseType)

    channels.invocation.subject('sent').next(request)
    state.responseStream = state.method.invokeWith(channelManager.getChannel(request.host, request.port), requestConverter.json_object_to_schema_object(request.body))
    state.responseStream.on('data', response => {
      channels.invocation.subject('received').next(responseConverter.schema_object_to_json_object(response))
    })
    state.responseStream.on('end', () => { channels.invocation.subject('finished').next() })
    state.responseStream.on('error', error => { channels.invocation.subject('error').next(error) })
  })

  channels.invocation.subject('cancelled').subscribe(() => {
    state.responseStream.end()
  })

  channels.invocation.subject('finished').subscribe(() => {
    state.inFlight = false
    dom.invokeButton.removeAttribute('disabled')
    dom.cancelButton.setAttribute('disabled', true)
  })

  return cmd
}
