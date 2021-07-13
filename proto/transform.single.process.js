const os = require('os')
const stream = require('stream')
const { getProgressBars } = require('./transform.progress.bar.js')

module.exports = {
  transformInSingleProcess,
}

function transformInSingleProcess(inputStreamDecoder, outputStreamEncoder, filter, shape, showProgressBar) {
  var messagesReceived = 0
  var messagesTransformed = 0

  const {
      progressBars,
      filterProgressBar,
      transformProgressBar,
  } = getProgressBars(showProgressBar, inputStreamDecoder.getSchemaName())

  const thePipeline = stream.pipeline(
    inputStreamDecoder.getInputStream(),
    inputStreamDecoder.makeInputTransformer(),
    new stream.Transform({
      transform(data, encoding, done) {
        try {
          const jsonObject = inputStreamDecoder.unmarshalJsonObject(data)
          if(filter(jsonObject)) {
            const shapedJsonObject = shape(jsonObject)
            this.push(outputStreamEncoder.marshalJsonObject(shapedJsonObject))
            messagesTransformed++
            transformProgressBar.setTotal(messagesTransformed)
            transformProgressBar.update(messagesTransformed)
          }
          messagesReceived++
          filterProgressBar.setTotal(messagesReceived)
          filterProgressBar.update(messagesReceived)
        } catch(ex) {
          console.error(ex)
        }
        done()
      }
    }),
    outputStreamEncoder.makeOutputTransformer(),
    outputStreamEncoder.getOutputStream(),
    () => {
      progressBars.stop()
    })

  process.on('SIGINT', () => {
    progressBars.stop()
  })
  process.on('exit', function () {
    progressBars.stop()
    if(!showProgressBar) {
      process.stderr.write(`Transformed ${messagesTransformed} of ${messagesReceived} messages${os.EOL}`)
    }
  })
}
