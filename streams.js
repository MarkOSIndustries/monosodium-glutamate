const stream = require('stream')

module.exports = {
  readLines,
  readJsonObjects,
  simpleLengthPrefixReader,
  simpleLengthPrefixWriter,
  readLengthPrefixedBuffers,
  writeLengthPrefixedBuffers,
  writeDelimited,
  sendMessagesToParent,
  readMessagesFromParent,
  readMessagesFromForked,
}

function readLines() {
  let buffer = ''
  return new stream.Transform({
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
}

function readJsonObjects() {
  return new stream.Transform({
    readableObjectMode: true,

    transform(chunk, encoding, done) {
      this.push(JSON.parse(chunk))
      done()
    }
  })
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

function simpleLengthPrefixReader(prefixFormat) {
  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixReadFn = `read${prefixFormat}`

  return {
    tryReadPrefix(buffer, lengthCallback) {
      if(buffer.length > prefixSize) {
        lengthCallback(buffer[prefixReadFn](), prefixSize)
        return true
      } else {
        return false
      }
    },
  }
}

function simpleLengthPrefixWriter(prefixFormat) {
  const prefixSize = prefixSizeByFormat[prefixFormat]
  const prefixWriteFn = `write${prefixFormat}`

  return {
    prefixSize(length) {
      return prefixSize
    },
    writePrefix(buffer, length) {
      buffer[prefixWriteFn](length)
    },
  }
}

function readLengthPrefixedBuffers({ tryReadPrefix }) {
  let chunkBuffer = Buffer.from([])
  return new stream.Transform({
    transform(chunk, encoding, done) {
      chunkBuffer = Buffer.concat([chunkBuffer, chunk])
      let chunkBufferLength = 0
      let prefixSize = 0
      while(tryReadPrefix(chunkBuffer, (length, bytesRead) => {
        chunkBufferLength = length
        prefixSize = bytesRead
      })) {
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
}

function writeLengthPrefixedBuffers({ prefixSize, writePrefix }) {
  return new stream.Transform({
    transform(chunk, encoding, done) {
      const chunkAsBuffer = ('string' === typeof chunk) ? 
        Buffer.from(chunk) :
        chunk

      const prefixBytes = prefixSize(chunkAsBuffer.length)
      const outputBuffer = Buffer.allocUnsafe(prefixBytes + chunkAsBuffer.length)
      writePrefix(outputBuffer, chunkAsBuffer.length)
      chunkAsBuffer.copy(outputBuffer, prefixBytes)
      this.push(outputBuffer)
      done()
    }
  })
}

function writeDelimited(delimiterBuffer) {
  return new stream.Transform({  
      transform(chunk, encoding, done) {
        try {
          this.push(chunk)
          this.push(delimiterBuffer)
          done()
        } catch(ex) {
          console.error('WTFFFFF', ex)
        }
      }
    })
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
