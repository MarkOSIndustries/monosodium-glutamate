const rxmq = require('rxmq').default
const transport = require('../grpc.transport')(rxmq)
const os = require('os')
const stream = require('stream')

module.exports = {
  invoke,
  transformToRequestResponsePairs,
  transformToResponsesOnly,
}

function invoke(method, inputStreamDecoder, outputStreamEncoder, hosts, timeout, maxInflight, transformRequestResponse, customHeaders) {
  const channelManager = new transport.ChannelManager()

  var nextHostIndex = 0

  var requestsSent = 0
  var responsesReceived = 0
  var requestsCompleted = 0

  stream.pipeline(
    inputStreamDecoder.getInputStream(),
    inputStreamDecoder.makeInputTransformer(),
    new stream.Transform({
      readableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          this.push(inputStreamDecoder.unmarshalSchemaObject(data))
        } catch(ex) {
          console.error(ex)
        }
        done()
      }
    }),
    new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,

      transform(requestSchemaObject, encoding, done) {
        const {host,port} = hosts[nextHostIndex]
        nextHostIndex = (nextHostIndex + 1) % hosts.length
        const channel = channelManager.getChannel(host, port)

        requestsSent = requestsSent + 1
        const responseStream = method.invokeWith(channel, requestSchemaObject, {
          timeoutValue: timeout,
          customHeaders,
        })
        responseStream.on('data', responseSchemaObject => {
          responsesReceived = responsesReceived + 1
          this.push(transformRequestResponse(method, requestSchemaObject, responseSchemaObject))
        })
        responseStream.on('end', () => {
          requestsCompleted = requestsCompleted + 1
        })
        responseStream.on('error', error => { console.error(error) })

        if(requestsSent - requestsCompleted < maxInflight) {
          done()
        } else {
          const backpressureInterval = setInterval(() => {
            if(requestsSent - requestsCompleted < maxInflight) {
              done()
              clearInterval(backpressureInterval)
            }
          }, 1)
        }
      },

      flush(done) {
        setInterval(() => {
          if(requestsCompleted === requestsSent) {
            done()
            process.exit()
          }
        }, 10)
      }
    }),
    new stream.Transform({
      writableObjectMode: true,
      
      transform(schemaObject, encoding, done) {
        this.push(outputStreamEncoder.marshalSchemaObject(schemaObject))
        done()
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

function transformToRequestResponsePairs(protobufIndex) {
  const Any = protobufIndex.messages['google.protobuf.Any']
  const RequestResponsePair = protobufIndex.messages['msg.RequestResponsePair']

  return (method, requestSchemaObject, responseSchemaObject) => {
    return RequestResponsePair.create({
      request: Any.create({
        type_url: method.requestTypeName,
        value: method.requestType.encode(requestSchemaObject).finish(),
      }),
      response: Any.create({
        type_url: method.responseTypeName,
        value: method.responseType.encode(responseSchemaObject).finish(),
      }),
    })
  }
}

function transformToResponsesOnly(protobufIndex) {
  return (method, requestSchemaObject, responseSchemaObject) => {
    return responseSchemaObject
  }
}