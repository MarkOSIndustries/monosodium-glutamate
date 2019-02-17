const os = require('os')

module.exports = {
  transform,
}

function transform(inputStreamDecoder, outputStreamEncoder, filter, shape, template) {
  var messagesTransformed = 0
  inputStreamDecoder.readJsonObjects(jsonObject => {
    if(filter(jsonObject)) {
      const shapedJsonObject = shape(jsonObject)
      messagesTransformed = messagesTransformed + 1
      outputStreamEncoder.writeJsonObject(shapedJsonObject)
    }
  }, ex => console.error(ex))

  const exit = () => {
    process.stderr.write(`Transformed ${messagesTransformed} messages${os.EOL}`)
    process.exit()
  }

  process.on('SIGINT', function() {
    if(process.stdin.isTTY) {
      exit()
    } else {
      setInterval(() => {
        if(process.stdin.readableLength === 0) {
          exit()
        }
      }, 10)
    }
  })
}
