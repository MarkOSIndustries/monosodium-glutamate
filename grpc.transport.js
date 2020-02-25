const http2 = require('http2')
const stream = require('stream')
const encoding = require('./grpc.transport.encoding.js')
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

class ChannelManager {
  constructor(fnInitChannel) {
    this.channels = {}
    this.fnInitChannel = fnInitChannel || (() => {})
  }

  getChannel(host, port, scheme) {
    scheme = scheme || 'http'
    this.channels[host] = this.channels[host] || {}
    this.channels[host][port] = this.channels[host][port] || {}
    if(!this.channels[host][port][scheme]) {
      this.channels[host][port][scheme] = new Http2Channel(host,port,scheme)
      this.fnInitChannel(this.channels[host][port][scheme])
    }
    return this.channels[host][port][scheme]
  }
}

class Http2Channel {
  constructor(host, port, scheme) {
    const self = this
    this.host = host
    this.port = port
    this.scheme = scheme
    this.address = `${this.scheme}://${this.host}:${this.port}`
    this.closed = false

    this.events = new stream.Readable()
    self.connected = false
    this.events.on('request', () => {
      if(!self.connected) {
        self.connect()
      }
    })
  }

  connect() {
    const self = this
    this.events.emit('connecting', this)
    this.client = http2.connect(self.address)
    this.client.on('ping', (pingBuffer) => self.client.ping(pingBuffer))
    this.client.on('goaway', () => console.error('Http2Channel server requested channel shutdown'))
    this.client.on('close', () => {
      self.connected = false
      self.events.emit('disconnected', this)
    })
    this.client.on('error', err => console.error('Http2Channel error', err))
    this.client.on('connect', () => {
      self.events.emit('connected', this)
    })
    this.connected = true
  }

  rpcImpl(service, options) {
    const self = this
    return (method, requestBuffer, callback) => {
      if(method == null) {
        self.events.emit('cancelled')
        return
      }
      this.events.emit('request', requestBuffer)
      const request = new Http2Request(this, service, method, (err, responseBuffer) => {
        if(responseBuffer == null) {
          self.events.emit('idle')
        } else {
          self.events.emit('response', responseBuffer)
        }
        callback(err, responseBuffer)
      }, Object.assign(DefaultRequestOptions, options || {}))
      self.events.on('disconnected', () => request.abort(null))
      self.events.on('cancelled', () => request.abort(null))
      request.send(requestBuffer)
    }
  }

  on(eventName, fnHandle) {
    this.events.on(eventName, fnHandle)
  }

  close() {
    this.closed = true
    this.connectClient.then(client => client.close())
  }
}

class Http2Request {
  constructor(channel, service, method, callback, options) {
    const self = this
    this.responseBuffer = Buffer.from([])
    this.stream = channel.client.request(this.buildHeaders(channel.scheme, channel.host, channel.port, service, method, options))
    this.callback = callback
    this.options = options

    this.stream.on('response', (headers, flags) => {
      const status = headers[GRPC_HEADER_STATUS] || 0
      if(status != 0) {
        self.abort(Object.assign({
          [GRPC_HEADER_STATUS_NAME]: GrpcStatusNames[headers[GRPC_HEADER_STATUS]]
        }, headers))
      }
      const acceptedRequestEncodings = headers[GRPC_HEADER_ACCEPT_MESSAGE_ENCODING]
      if(acceptedRequestEncodings) {
        acceptedRequestEncodings.split(',').forEach(newRequestEncoding => {
          if(encoding.GRPCEncodingsByName.hasOwnProperty(newRequestEncoding)) {
            self.options.requestEncoding = encoding.GRPCEncodingsByName[newRequestEncoding]
          }
        })
      }
      const acceptedResponseEncodings = headers[GRPC_HEADER_MESSAGE_ENCODING]
      if(acceptedResponseEncodings) {
        acceptedResponseEncodings.split(',').forEach(newResponseEncoding => {
          if(encoding.GRPCEncodingsByName.hasOwnProperty(newResponseEncoding)) {
            self.options.responseEncoding = encoding.GRPCEncodingsByName[newResponseEncoding]
          }
        })
      }
    })
    this.stream.on('data', chunk => {
      self.responseBuffer = self.unpackMessages(self.options.responseEncoding, Buffer.concat([self.responseBuffer, chunk]), msg => self.callback(null, msg))
    })
    this.stream.on('end', () => self.abort(null))
  }

  abort(err) {
    this.callback(err, null)
    this.stream.destroy()
  }

  send(requestBuffer) {
    const self = this
    // TODO: allow streaming requests
    this.packMessage(this.options.requestEncoding, requestBuffer, chunk => self.stream.write(chunk))
      .then(() => self.stream.end())
  }

  buildHeaders(scheme, host, port, service, method, options) {
    return {
      [HTTP2_HEADER_METHOD]: 'POST',
      [HTTP2_HEADER_SCHEME]: scheme,
      [HTTP2_HEADER_PATH]: `/${service.fullName.slice(1)}/${method.name}`,
      [HTTP2_HEADER_TE]: 'trailers',
      [HTTP2_HEADER_AUTHORITY]: `${host}:${port}`,
      [GRPC_HEADER_TIMEOUT]: `${options.timeoutValue}${options.timeoutUnit}`,
      [HTTP2_HEADER_CONTENT_TYPE]: 'application/grpc+proto',
      [GRPC_HEADER_MESSAGE_TYPE]: method.resolvedRequestType.fullName,
      [GRPC_HEADER_MESSAGE_ENCODING]: options.requestEncoding.name,
      [GRPC_HEADER_ACCEPT_MESSAGE_ENCODING]: options.responseEncoding.name,
    }
  }

  packMessage(requestEncoding, buffer, fnChunkPacked) {
    return requestEncoding.encode(buffer).then(encodedBuffer => {
      while(encodedBuffer.length > 0) {
        const chunk = encodedBuffer.slice(0, MaxFrameSize)
        encodedBuffer = encodedBuffer.slice(MaxFrameSize)

        const packed = Buffer.alloc(5 + chunk.length)
        packed.writeUInt8(requestEncoding.compressed, 0)
        packed.writeUInt32BE(chunk.length, 1)
        chunk.copy(packed, 5)
        fnChunkPacked(packed)
      }
    })
  }

  unpackMessages(responseEncoding, buffer, fnMessageUnpacked) {
    if(buffer.length >= 5) {
      const compression = buffer.readUInt8(0,1)
      if(compression != responseEncoding.compressed) {
        console.error('Message compression mismatch, guessing...')
        if(compression === 0) {
          responseEncoding = encoding.IdentityEncoding
        } else {
          responseEncoding = encoding.GZIPEncoding
        }
      }
      const messageSize = buffer.readUInt32BE(1,4)

      if(buffer.length >= (5 + messageSize)) {
        responseEncoding.decode(buffer.slice(5, 5 + messageSize)).then(fnMessageUnpacked)
        return this.unpackMessages(responseEncoding, buffer.slice(5 + messageSize), fnMessageUnpacked)
      }
    }
    return buffer
  }
}

module.exports = {
  TimeoutUnits,
  ChannelManager,
}
