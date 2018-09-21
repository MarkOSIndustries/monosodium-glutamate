module.exports = function(channels) {
  const cmd = {
    loadSample: () => {
      if(!state.method) return false
      channels.request.subject('payload.replaced').next(JSON.stringify(state.method.requestSample, undefined, '  '))
      return true
    },
    loadLastSent: () => {
      if(!state.method) return false
      const lastSent = localStorage.getItem(`${state.method.serviceName}-${state.method.methodName}-request`)
      if(!lastSent) return false
      channels.request.subject('payload.replaced').next(lastSent)
      return true
    },
  }

  const state = {
    method: null,
  }

  const requestEditorPromise = require('monaco-loader')().then(monaco =>
    monaco.editor.create(document.querySelector(`.request-json`), {
      value: '{}',
      language: 'json',
      theme: 'vs-light',
      automaticLayout: true,
      scrollBeyondLastLine: false,
      minimap: {
        enabled: false
      }
    }))

  channels.request.subject('payload.replaced').subscribe(payload => {
    requestEditorPromise.then(requestEditor => {
      requestEditor.setValue(payload)
      channels.request.subject('payload.changed').next(payload)
    })
  })

  requestEditorPromise.then(requestEditor => requestEditor.onDidChangeModelContent((e) => {
    channels.request.subject('payload.changed').next(requestEditor.getValue())
  }))

  channels.services.subject('method.selection.finished').subscribe(() => {
    requestEditorPromise.then(requestEditor => requestEditor.focus())
  })

  channels.services.subject('method.selection.changed').subscribe(method => {
    state.method = method
    if(!cmd.loadLastSent()) cmd.loadSample()
  })

  channels.invocation.subject('started').subscribe(requestPayload => {
    localStorage.setItem(`${state.method.serviceName}-${state.method.methodName}-request`, requestPayload)
  })

  return cmd
}
