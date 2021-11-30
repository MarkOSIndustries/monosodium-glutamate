const rxmq = require('rxmq').default
const transport = require('../grpc.transport')(rxmq)
const os = require('os')
const stream = require('stream')

module.exports = {
  invokeStream,
}

function invokeStream(method, inputStreamDecoder, outputStreamEncoder, host, port, timeout, customHeaders) {
  const channelManager = new transport.ChannelManager()

  var requestsSent = 0
  var responsesReceived = 0

  const {requestsStream, responsesStream} = method.makeRpcStreams(channelManager.getChannel(host, port), {
    timeoutValue: timeout,
    customHeaders,
  })

  stream.pipeline(
    inputStreamDecoder.getInputStream(),
    inputStreamDecoder.makeInputTransformer(),
    new stream.Transform({
      readableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(inputStreamDecoder.unmarshalSchemaObject(data))
          requestsSent = requestsSent + 1
        } catch(ex) {
          console.error(ex)
        }
        done()
      }
    }),
    requestsStream,
    err => {
      if(err) {
        console.error(err)
      }
    })

  stream.pipeline(
    responsesStream,
    new stream.Transform({
      writableObjectMode: true,
      
      transform(schemaObject, encoding, done) {
        responsesReceived = responsesReceived + 1
        this.push(outputStreamEncoder.marshalSchemaObject(schemaObject))
        done()
      },

      flush(done) {
        done()
        process.exit()
      }
    }),
    outputStreamEncoder.makeOutputTransformer(),
    outputStreamEncoder.getOutputStream(),
    err => {
      if(err) {
        console.error(err)
        process.exit()
      }
    })

  if(process.stdin.isTTY) {
    process.on('SIGINT', function() {
      process.exit()
    })
  }

  process.on('exit', function () {
    process.stderr.write(`Sent ${requestsSent} requests and received ${responsesReceived} responses${os.EOL}`)
  })
}
