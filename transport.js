const http2 = require('http2')
const protobuf = require('protobufjs')
const stream = require('stream')

// Simple Impl of:
// https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

const GRPCEncodingType = {
  Identity: 'identity',
  GZIP: 'gzip',
  Deflate: 'deflate',
  Snappy: 'snappy',
}
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
          if(connected || closed) return
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
    return (method, requestBuffer, callback) => {
      this.clientNeeded.emit('needed')
      this.connectClient.then(client => {
        const stream = client.request(this.buildHeaders(service, method, Object.assign(DefaultRequestOptions, options || {})))

        const responseBuffer = Buffer.concat([])
        function tryCallback() {
          // TODO: make decompression an external concern, this assumes 'identity'
          while(responseBuffer.length >= 5) {
            const compression = responseBuffer.readUInt8(0)
            const messageSize = responseBuffer.readUInt32BE(1)

            if(responseBuffer.length > 5 + messageSize) {
              callback(null, responseBuffer.slice(5, messageSize))
              responseBuffer = responseBuffer.slice(5 + messageSize)
            }
          }
        }

        function endStream(err) {
          tryCallback()
          callback(err || null, null) // tell protobufjs we're finished
          // stream.close()
          stream.destroy()
        }

        this.clientClosed.on('closed', () => endStream('Connection closed'))

        stream.on('response', (headers, flags) => {
          if(headers[GRPC_HEADER_STATUS] !== 0) {
            endStream(Object.assign({
              [GRPC_HEADER_STATUS_NAME]: GrpcStatusNames[headers[GRPC_HEADER_STATUS]]
            }, headers))
          }
        })
        stream.on('data', nextBuffer => {
          responseBuffer = Buffer.concat([responseBuffer, nextBuffer])
          tryCallback()
        })
        stream.on('end', () => endStream())

        // TODO: allow streaming requests
        // TODO: make compression an external concern, this assumes 'identity'
        let lengthPrefixedRequestBuffer = Buffer.alloc(5 + requestBuffer.length)
        lengthPrefixedRequestBuffer.writeUInt8(0, 0)
        lengthPrefixedRequestBuffer.writeUInt32BE(requestBuffer.length, 1)
        requestBuffer.copy(lengthPrefixedRequestBuffer, 5)
        // TODO: check request size and break into allowable chunks using stream.write
        stream.end(lengthPrefixedRequestBuffer)
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
      [GRPC_HEADER_MESSAGE_ENCODING]: GRPCEncodingType.Identity,
      [GRPC_HEADER_ACCEPT_MESSAGE_ENCODING]: GRPCEncodingType.Identity,
    }
  }

  close() {
    this.closed = true
    this.connectClient.then(client => client.close())
  }
}




module.exports = {
  TimeoutUnits,
  Http2Channel,
}
