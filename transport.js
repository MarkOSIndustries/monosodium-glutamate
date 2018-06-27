const http2 = require('http2')
const protobuf = require('protobufjs')
const stream = require('stream')
const encoding = require('./transport.encoding.js')
// Simple Impl of:
// https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

const MaxFrameSize = 16379

const TimeoutUnits = {
  Hour: 'H',
  Minute: 'M',
  Second: 'S',
  Millisecond: 'm',
  Microsecond: 'u',
  Nanosecond: 'n',
}
const DefaultRequestOptions = {
  timeoutValue: 5,
  timeoutUnit: TimeoutUnits.Minute,
  requestEncoding: encoding.GZIPEncoding,
  responseEncoding: encoding.GZIPEncoding,
}

const {
  HTTP2_HEADER_AUTHORITY,
  HTTP2_HEADER_CONTENT_TYPE,
  HTTP2_HEADER_METHOD,
  HTTP2_HEADER_PATH,
  HTTP2_HEADER_SCHEME,
  HTTP2_HEADER_TE,
  HTTP2_HEADER_USER_AGENT,
  HTTP2_HEADER_STATUS,
} = http2.constants;

const GRPC_HEADER_TIMEOUT = 'grpc-timeout';
const GRPC_HEADER_MESSAGE_TYPE = 'grpc-message-type';
const GRPC_HEADER_MESSAGE_ENCODING = 'grpc-encoding';
const GRPC_HEADER_ACCEPT_MESSAGE_ENCODING = 'grpc-accept-encoding';
const GRPC_HEADER_STATUS = "grpc-status";
const GRPC_HEADER_STATUS_NAME = "grpc-status-name"; // not part of spec, just useful
const GRPC_HEADER_MESSAGE = "grpc-message";

const GrpcStatusNames = [
  'OK',
  'CANCELLED',
  'UNKNOWN',
  'INVALID_ARGUMENT',
  'DEADLINE_EXCEEDED',
  'NOT_FOUND',
  'ALREADY_EXISTS',
  'PERMISSION_DENIED',
  'RESOURCE_EXHAUSTED',
  'FAILED_PRECONDITION',
  'ABORTED',
  'OUT_OF_RANGE',
  'UNIMPLEMENTED',
  'INTERNAL',
  'UNAVAILABLE',
  'DATA_LOSS',
  'UNAUTHENTICATED',
]

class Http2Channel {
  constructor(host, port, scheme) {
    const self = this
    this.host = host
    this.port = port
    this.scheme = scheme || 'http'
    this.address = `${this.scheme}://${this.host}:${this.port}`
    this.closed = false

    function initHttp2Client() {
      self.clientNeeded = new stream.Readable()
      self.clientClosed = new stream.Readable()
      self.connectClient = new Promise((resolve,reject) => {
        let connected = false
        self.clientNeeded.on('needed', () => {
          if(connected) return
          connected = true

          const client = http2.connect(self.address)
          client.on('ping', (pingBuffer) => client.ping(pingBuffer))
          client.on('goaway', () => console.log('Http2Channel server requested channel shutdown'))
          client.on('close', () => {
            self.clientClosed.emit('closed')
            console.log('Http2Channel closed. Will reconnect when needed')
          })
          // client.on('stream', () => console.log('Http2Channel stream initiated'))
          client.on('error', err => console.error('Http2Channel error', err))
          client.on('connect', () => console.log('Http2Channel connected', self.address))
          resolve(client)
        })
      })
    }

    initHttp2Client()
  }

  rpcImpl(service, options) {
    const self = this
    const requestOptions = Object.assign(DefaultRequestOptions, options || {})

    return (method, requestBuffer, callback) => {
      this.clientNeeded.emit('needed')
      this.connectClient.then(client => {
        let responseBuffer = Buffer.concat([])

        const stream = client.request(this.buildHeaders(service, method, requestOptions))

        function tryCallback() {
          responseBuffer = self.unpackMessages(requestOptions.responseEncoding, responseBuffer, message => callback(null, message))
        }

        function endStream(err) {
          tryCallback()
          callback(err || null, null) // tell protobufjs we're finished
          stream.destroy()
        }

        self.clientClosed.on('closed', () => endStream('Connection closed'))

        stream.on('response', (headers, flags) => {
          if(headers[GRPC_HEADER_STATUS] != 0) {
            endStream(Object.assign({
              [GRPC_HEADER_STATUS_NAME]: GrpcStatusNames[headers[GRPC_HEADER_STATUS]]
            }, headers))
          }
          if(headers.hasOwnProperty(GRPC_HEADER_ACCEPT_MESSAGE_ENCODING)) {
            const newRequestEncoding = headers[GRPC_HEADER_ACCEPT_MESSAGE_ENCODING];
            if(!encoding.GRPCEncodingsByName.hasOwnProperty(newRequestEncoding)) {
              endStream(new Error(`Encoding ${newRequestEncoding} is not supported`))
              return
            }
            requestOptions.requestEncoding = encoding.GRPCEncodingsByName[newRequestEncoding]
            console.log("Request encoding negotatiated", requestOptions.requestEncoding.name)
          }
          if(headers.hasOwnProperty(GRPC_HEADER_MESSAGE_ENCODING)) {
            const newResponseEncoding = headers[GRPC_HEADER_MESSAGE_ENCODING];
            if(!encoding.GRPCEncodingsByName.hasOwnProperty(newResponseEncoding)) {
              endStream(new Error(`Encoding ${newRequestEncoding} is not supported`))
              return
            }
            requestOptions.responseEncoding = encoding.GRPCEncodingsByName[newResponseEncoding]
            console.log("Response encoding negotatiated", requestOptions.responseEncoding.name)
          }
        })
        stream.on('data', nextBuffer => {
          responseBuffer = Buffer.concat([responseBuffer, nextBuffer])
          tryCallback()
        })
        stream.on('end', () => endStream())

        // TODO: allow streaming requests
        self.packMessage(requestOptions.requestEncoding, requestBuffer, chunk => stream.write(chunk))
          .then(() => stream.end())
      }).catch(err => {
        callback(err)
      })
    }
  }

  buildHeaders(service, method, options) {
    return {
      [HTTP2_HEADER_METHOD]: 'POST',
      [HTTP2_HEADER_SCHEME]: this.scheme,
      [HTTP2_HEADER_PATH]: `/${service.fullName.slice(1)}/${method.name}`,
      [HTTP2_HEADER_TE]: 'trailers',
      [HTTP2_HEADER_AUTHORITY]: `${this.host}:${this.port}`,
      [GRPC_HEADER_TIMEOUT]: `${options.timeoutValue}${options.timeoutUnit}`,
      [HTTP2_HEADER_CONTENT_TYPE]: 'application/grpc+proto',
      [GRPC_HEADER_MESSAGE_TYPE]: method.resolvedRequestType.fullName,
      [GRPC_HEADER_MESSAGE_ENCODING]: options.requestEncoding.name,
      [GRPC_HEADER_ACCEPT_MESSAGE_ENCODING]: options.responseEncoding.name,
    }
  }

  packMessage(encoding, buffer, fnChunkPacked) {
    return encoding.encode(buffer).then(encodedBuffer => {
      while(encodedBuffer.length > 0) {
        const chunk = encodedBuffer.slice(0, MaxFrameSize)
        encodedBuffer = encodedBuffer.slice(MaxFrameSize)

        const packed = Buffer.alloc(5 + chunk.length)
        packed.writeUInt8(encoding.compressed, 0)
        packed.writeUInt32BE(chunk.length, 1)
        chunk.copy(packed, 5)
        fnChunkPacked(packed)
      }
    })
  }

  unpackMessages(encoding, buffer, fnMessageUnpacked) {
    if(buffer.length >= 5) {
      const compression = buffer.readUInt8(0)
      const messageSize = buffer.readUInt32BE(1)

      if(buffer.length > (5 + messageSize)) {
        const bufferRemaining =
        fnBufferAltered(bufferRemaining)
        encoding.decode(buffer.slice(5, messageSize)).then(fnMessageUnpacked)
        return this.unpackMessages(buffer.slice(5 + messageSize))
      }
    }
    return buffer
  }

  close() {
    this.closed = true
    this.connectClient.then(client => client.close())
  }
}

class Http2Request {
  // TODO: move request logic in here
}

module.exports = {
  TimeoutUnits,
  Http2Channel,
}
