const os = require('os')
const stream = require('stream')

module.exports = {
  transform,
}

function transform(inputStreamDecoder, outputStreamEncoder, filter, shape, template) {
  var messagesTransformed = 0

  inputStreamDecoder
    .streamJsonObjects(ex => console.error(ex))
    .pipe(new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(jsonObject, encoding, done) {
        if(filter(jsonObject)) {
          const shapedJsonObject = shape(jsonObject)
          messagesTransformed = messagesTransformed + 1
          this.push(shapedJsonObject)
        }
        done()
      }
    }))
    .pipe(outputStreamEncoder.streamJsonObjects())

  process.on('exit', function () {
    process.stderr.write(`Transformed ${messagesTransformed} messages${os.EOL}`)
  })
}
