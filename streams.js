const stream = require('stream')

module.exports = {
  streamUTF8Lines
}

function streamUTF8Lines(inStream) {
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
