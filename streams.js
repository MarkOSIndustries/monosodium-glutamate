const stream = require('stream')

module.exports = {
  readUTF8Lines,
  readLengthPrefixedBuffers,
  writeLengthPrefixedBuffers,
}

function readUTF8Lines(inStream) {
  const outStream = new stream.Writable()

  inStream.setEncoding('utf8')

  let buffer = ''
  inStream.on('data', (chunk) => {
    if (chunk !== null) {
      buffer += chunk
      const lines = buffer.split(/[\r\n]/)
      buffer = lines.pop() // will be empty string if end char was newline

      lines.filter(l => l!=='').forEach(l => outStream.emit('line', l))
    }
  })

  inStream.on('end', () => {
    outStream.emit('end', {})
  })

  return outStream
}

const prefixSizeByFormat = {
  'UInt32LE': 4,
  'UInt32BE': 4,
  'UInt16LE': 2,
  'UInt16BE': 2,
  'UInt8': 1,
}

function readLengthPrefixedBuffers(inStream, prefixFormat) {
  const outStream = new stream.Writable()

  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixReadFn = `read${prefixFormat}`

  var chunkBuffer = Buffer.from([])
  inStream.on('data', chunk => {
    chunkBuffer = Buffer.concat([chunkBuffer, chunk])
    while(chunkBuffer.length > prefixSize) {
      const binaryBufferLength = chunkBuffer[prefixReadFn]()
      const totalBytesToRead = prefixSize + binaryBufferLength
      if(chunkBuffer.length >= totalBytesToRead) {
        outStream.emit('data', chunkBuffer.slice(prefixSize,totalBytesToRead))
        chunkBuffer = chunkBuffer.slice(totalBytesToRead)
      }
    }
  })

  inStream.on('end', () => {
    outStream.emit('end', {})
  })

  return outStream
}

function writeLengthPrefixedBuffers(outStream, prefixFormat) {
  const inStream = new stream.Writable({
    write(chunk,encoding,cb) {
      if('string' === typeof chunk) {
        this.emit('data', Buffer.from(chunk))
      } else {
        this.emit('data', chunk)
      }
      cb()
    }
  })

  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixWriteFn = `write${prefixFormat}`

  inStream.on('data', binaryBuffer => {
    const outputBuffer = Buffer.allocUnsafe(prefixSize + binaryBuffer.length)
    outputBuffer[prefixWriteFn](binaryBuffer.length)
    binaryBuffer.copy(outputBuffer, prefixSize)
    outStream.write(outputBuffer)
  })

  return inStream
}
