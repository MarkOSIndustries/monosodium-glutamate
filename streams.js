const stream = require('stream')

module.exports = {
  readUTF8Lines,
  readLineDelimitedJsonObjects,
  readLengthPrefixedBuffers,
  writeLengthPrefixedBuffers,
  writeDelimited,
  sendMessagesToParent,
  readMessagesFromParent,
  readMessagesFromForked,
}

function readUTF8Lines(inStream) {
  let buffer = ''
  const outStream = new stream.Transform({
    transform(chunk, encoding, done) {
      if (chunk !== null) {
        buffer += chunk
        const lines = buffer.split(/[\r\n]/)
        buffer = lines.pop() // will be empty string if end char was newline
        lines.filter(l => l!=='').forEach(l => this.push(l))
      }
      done()
    },

    flush(done) {
      const lines = buffer.split(/[\r\n]/)
      lines.filter(l => l!=='').forEach(l => this.push(l))
      done()
    }
  })

  inStream.setEncoding('utf8')

  inStream.pipe(outStream)

  return outStream
}

function readLineDelimitedJsonObjects(inStream) {
  const outStream = new stream.Transform({
    readableObjectMode: true,

    transform(chunk, encoding, done) {
      this.push(JSON.parse(chunk))
      done()
    }
  })

  readUTF8Lines(inStream).pipe(outStream)

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
  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixReadFn = `read${prefixFormat}`
  let chunkBuffer = Buffer.from([])
  const outStream = new stream.Transform({
    transform(chunk, encoding, done) {
      chunkBuffer = Buffer.concat([chunkBuffer, chunk])
      while(chunkBuffer.length > prefixSize) {
        const chunkBufferLength = chunkBuffer[prefixReadFn]()
        const totalBytesToRead = prefixSize + chunkBufferLength
        if(chunkBuffer.length >= totalBytesToRead) {
          this.push(chunkBuffer.slice(prefixSize,totalBytesToRead))
          chunkBuffer = chunkBuffer.slice(totalBytesToRead)
        } else {
          break
        }
      }
      done()
    }
  })

  
  inStream.pipe(outStream)

  return outStream
}

function writeLengthPrefixedBuffers(outStream, prefixFormat) {
  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixWriteFn = `write${prefixFormat}`

  const inStream = new stream.Transform({
    transform(chunk, encoding, done) {
      const chunkAsBuffer = ('string' === typeof chunk) ? 
        Buffer.from(chunk) :
        chunk

      const outputBuffer = Buffer.allocUnsafe(prefixSize + chunkAsBuffer.length)
      outputBuffer[prefixWriteFn](chunkAsBuffer.length)
      chunkAsBuffer.copy(outputBuffer, prefixSize)
      this.push(outputBuffer)
      done()
    }
  })

  inStream.pipe(outStream)

  return inStream
}

function writeDelimited(outStream, delimiterBuffer) {
  const inStream = new stream.Transform({    
    transform(chunk, encoding, done) {
      this.push(chunk)
      this.push(delimiterBuffer)
      done()
    }
  })

  inStream.pipe(outStream)

  return inStream
}

function sendMessagesToParent() {
  const childMessagesStream = new stream.Transform({
    writableObjectMode: true,

    write(jsonObject, encoding, done) {
      process.send(jsonObject)
      done()
    }
  })
  return childMessagesStream
}

function readMessagesFromParent() {
  const parentMessagesStream = new stream.Transform({
    readableObjectMode: true
  })
  process.on('message', (message) => {
    parentMessagesStream.push(message)
  })
  return parentMessagesStream
}


function readMessagesFromForked(forkedProcess) {
  const forkedMessagesStream = new stream.Transform({
    readableObjectMode: true
  })
  forkedProcess.on('message', (message) => {
    forkedMessagesStream.push(message)
  })
  return forkedMessagesStream
}
