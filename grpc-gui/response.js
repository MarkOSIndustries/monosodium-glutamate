module.exports = function(channels) {
  const cmd = {
  }

  const dom = {
    statusArea: document.querySelector('#status-area'),
    responseTiming: document.querySelector('#response-timing'),
    responseListing: document.querySelector('#response-listing'),
    responseOutcome: document.querySelector('#response-outcome'),
  }

  const state = {
    inFlight: false,
    responses: [],
    status: '',
  }

  channels.invocation.subject('started').subscribe(requestPayload => {
    dom.responseListing.innerHTML = ''
    dom.responseOutcome.innerHTML = 'Running'
    dom.responseTiming.innerHTML = '...'

    state.inFlight = true
    state.responses = []
    state.status = 'success'

    const t0 = performance.now()
    const durationRenderInterval = setInterval(() => {
      const t1 = performance.now()
      const duration = t1-t0
      dom.responseTiming.innerHTML = `${duration.toFixed(3)} milliseconds`
      if(!state.inFlight) {
        clearInterval(durationRenderInterval)
        console.log('Response completed in', duration)
      }
    }, 25)
    const responseRenderInterval = setInterval(() => {
      if(!state.inFlight) {
        clearInterval(responseRenderInterval)
      }

      if(state.responses.length) {
        console.log('Rendering response batch of ', state.responses.length)
        var fragment = document.createDocumentFragment()
        while(state.responses.length) {
          const response = state.responses.shift()
          const responseElement = document.createElement('pre')
          responseElement.innerText = JSON.stringify(response, undefined, '  ')
          fragment.appendChild(responseElement)
          fragment.appendChild(document.createElement('hr'))
        }
        dom.responseListing.appendChild(fragment)
      }
    }, 500)
  })
  channels.invocation.subject('received').subscribe(response => {
    state.responses.push(response)
  })
  channels.invocation.subject('error').subscribe(error => {
    state.status = 'failure'
    state.error = error
  })
  channels.invocation.subject('finished').subscribe(() => {
    state.inFlight = false
    dom.responseOutcome.innerHTML = state.status
    dom.responseListing.className = dom.statusArea.setAttribute('data-state', state.status)
    if(state.error) {
      dom.responseListing.innerHTML = `<pre>${JSON.stringify(state.error, undefined, '  ')}</pre>`
    }
  })

  return cmd
}
