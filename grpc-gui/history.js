
module.exports = function(channels) {
  const cmd = {}

  const dom = {
    requestListing: document.querySelector('#request-listing'),
  }

  channels.invocation.subject('sent').subscribe(request => {
    console.log('Request', request)
    dom.requestListing.innerHTML = `<pre><code>${JSON.stringify(request, undefined, '  ')}</code><pre>`
  })

  return cmd
}
