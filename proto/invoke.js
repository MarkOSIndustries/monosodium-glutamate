const transport = require('../grpc.transport')
const os = require('os')

module.exports = {
  invoke,
}

function invoke(method, inputStreamDecoder, outputStreamEncoder, host, port, timeout) {
  const channelManager = new transport.ChannelManager()

  var requestsSent = 0
  var responsesReceived = 0
  var requestsCompleted = 0
  inputStreamDecoder.readSchemaObjects(requestSchemaObject => {
    requestsSent = requestsSent + 1
    const responseStream = method.invokeWith(channelManager.getChannel(host, port), requestSchemaObject, {
      timeoutValue: timeout
    })
    responseStream.on('data', responseSchemaObject => {
      responsesReceived = responsesReceived + 1
      outputStreamEncoder.writeSchemaObject(responseSchemaObject)
    })
    responseStream.on('end', () => {
      requestsCompleted = requestsCompleted + 1
      if(requestsCompleted === requestsSent && !process.stdin.isTTY) exit()
    })
    responseStream.on('error', error => { console.error(error) })
  }, ex => console.error(ex))

  const exit = () => {
    process.stderr.write(`Sent ${requestsSent} requests and received ${responsesReceived} responses${os.EOL}`)
    process.exit()
  }

  process.on('SIGINT', function() {
    if(process.stdin.isTTY) {
      exit()
    } else {
      setInterval(() => {
        if(process.stdin.readableLength === 0) {
          exit()
        }
      }, 10)
    }
  })
}
