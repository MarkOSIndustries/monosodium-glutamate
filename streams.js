const stream = require('stream')

module.exports = {
  readUTF8Lines,
  readLineDelimitedJsonObjects,
  readLengthPrefixedBuffers,
  writeLengthPrefixedBuffers,
  writeDelimited,
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

      lines.filter(l => l!=='').forEach(l => outStream.emit('data', l))
    }
  })

  inStream.on('end', () => {
    const lines = buffer.split(/[\r\n]/)
    lines.filter(l => l!=='').forEach(l => outStream.emit('data', l))
    outStream.emit('end', {})
  })

  return outStream
}

function readLineDelimitedJsonObjects(inStream) {
    const outStream = new stream.Writable()
    readUTF8Lines(inStream).on('data', line => {
      outStream.emit('data', JSON.parse(line))
    })
    return outStream
}

const prefixSizeByFormat = {
  'Int32LE': 4,
  'Int32BE': 4,
  'Int16LE': 2,
  'Int16BE': 2,
  'Int8': 1,
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

function writeDelimited(outStream, delimiterBuffer) {
  const inStream = new stream.Writable({
    write(chunk,encoding,cb) {
      this.emit('data', chunk)
      cb()
    }
  })

  inStream.on('data', data => {
    outStream.write(data)
    outStream.write(delimiterBuffer)
  })

  return inStream
}
