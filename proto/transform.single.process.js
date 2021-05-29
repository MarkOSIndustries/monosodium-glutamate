const os = require('os')
const stream = require('stream')

module.exports = {
  transformInSingleProcess,
}

function transformInSingleProcess(inputStreamDecoder, outputStreamEncoder, filter, shape) {
  var messagesReceived = 0
  var messagesTransformed = 0

  inputStreamDecoder
    .makeInputStream()
    .pipe(new stream.Transform({
      readableObjectMode: true,
      
      transform(data, encoding, done) {
        try {
          const jsonObject = inputStreamDecoder.unmarshalJsonObject(data)
          if(filter(jsonObject)) {
            const shapedJsonObject = shape(jsonObject)
            this.push(shapedJsonObject)
            messagesTransformed++
          }
          messagesReceived++
        } catch(ex) {
          console.error(ex)
        }
        done()
      }
    }))
    .pipe(new stream.Transform({
      writableObjectMode: true,
      
      transform(jsonObject, encoding, done) {
        this.push(outputStreamEncoder.marshalJsonObject(jsonObject))
        done()
      }
    }))
    .pipe(outputStreamEncoder.makeOutputStream())

  process.on('exit', function () {
    process.stderr.write(`Transformed ${messagesTransformed} of ${messagesReceived} messages${os.EOL}`)
  })
}
