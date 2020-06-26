const transport = require('../grpc.transport')
const os = require('os')
const stream = require('stream')

module.exports = {
  invoke,
}

function invoke(method, inputStreamDecoder, outputStreamEncoder, host, port, timeout) {
  const channelManager = new transport.ChannelManager()

  var requestsSent = 0
  var responsesReceived = 0
  var requestsCompleted = 0

  inputStreamDecoder
    .streamSchemaObjects(ex => console.error(ex))
    .pipe(new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(requestSchemaObject, encoding, done) {
        requestsSent = requestsSent + 1
        const responseStream = method.invokeWith(channelManager.getChannel(host, port), requestSchemaObject, {
          timeoutValue: timeout
        })
        responseStream.on('data', responseSchemaObject => {
          responsesReceived = responsesReceived + 1
          this.push(responseSchemaObject)
        })
        responseStream.on('end', () => {
          requestsCompleted = requestsCompleted + 1
        })
        responseStream.on('error', error => { console.error(error) })

        done()
      },

      flush(done) {
        setInterval(() => {
          if(requestsCompleted === requestsSent) {
            done()
            process.exit()
          }
        }, 10)
      }
    }))
    .pipe(outputStreamEncoder.streamSchemaObjects())

  if(process.stdin.isTTY) {
    process.on('SIGINT', function() {
      process.exit()
    })
  }

  process.on('exit', function () {
    process.stderr.write(`Sent ${requestsSent} requests and received ${responsesReceived} responses${os.EOL}`)
  })
}
