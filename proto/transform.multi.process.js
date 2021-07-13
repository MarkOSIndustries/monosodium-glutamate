const os = require('os')
const stream = require('stream')
const { fork } = require('child_process')
const {
  sendMessagesToParent,
  readMessagesFromParent,
  readMessagesFromForked,
} = require('../streams.js')
const { getProgressBars } = require('./transform.progress.bar.js')

module.exports = {
  transformInParentProcess,
  transformInForkedProcess,
}

const workerEncoding = 'base64'



function transformInParentProcess(inputStreamDecoder, outputStreamEncoder, filter, shape, forkedWorkerCount, forkedWorkerArgs, showProgressBar) {
  var messagesToFilter = 0
  var messagesFiltered = 0
  var messagesToTransform = 0
  var messagesTransformed = 0
  var sendIndex = 0
  var recvIndex = 0
  var inputExhausted = false
  const workBuffer = new Map()
  const forkedWorkers = []

  const {
      progressBars,
      filterProgressBar,
      transformProgressBar,
  } = getProgressBars(showProgressBar, inputStreamDecoder.getSchemaName())

  function shutdown() {
    for (const forkedWorker of forkedWorkers) {
      forkedWorker.kill('SIGINT')
    }
    process.exit()
  }

  process.on('SIGINT', shutdown)

  const processedMessagesPipeline = new stream.Transform({
    readableObjectMode: true,
    writableObjectMode: true,
    
    transform({index, messages, progress}, encoding, done) {
      workBuffer.set(index, {messages, progress})
      messagesToTransform += messages.length
      transformProgressBar.setTotal(messagesToTransform)
      if(index == recvIndex) {
        while((nextInSequence = workBuffer.get(recvIndex))) {
          messagesFiltered += nextInSequence.progress
          filterProgressBar.update(messagesFiltered)
          for(const message of nextInSequence.messages) {
            this.push(Buffer.from(message, workerEncoding))
            messagesTransformed++
            transformProgressBar.update(messagesTransformed)
          }
          workBuffer.delete(recvIndex)
          recvIndex++
        }
      }

      if(inputExhausted && recvIndex == sendIndex) {
        shutdown()
      }
      
      done()
    }
  })
  stream.pipeline(
      processedMessagesPipeline,
      outputStreamEncoder.makeOutputTransformer(),
      outputStreamEncoder.getOutputStream(),
      () => {})

  processedMessagesPipeline.setMaxListeners(forkedWorkerCount+1)

  forkedWorkers.push(...Array(forkedWorkerCount).fill().map((_, workerIndex) => {
    const forkedWorker = fork(process.argv[1], forkedWorkerArgs, {silent: true})
    forkedWorker.on('error', (err) => {
      shutdown()
    })
    forkedWorker.on('exit', (code, signal) => {
      if(code != 0) {
        shutdown()
      }
    })
    forkedWorker.stderr.on('data', data => {
      process.stderr.write(data)
    })

    readMessagesFromForked(forkedWorker).pipe(processedMessagesPipeline)

    return forkedWorker
  }))

  var sendBuffer = []

  function flushSendBuffer() {
    if(sendBuffer.length > 0) {
      const index = sendIndex++
      forkedWorkers[index%forkedWorkers.length].send({index,messages:sendBuffer})
      sendBuffer = []
    }
  }

  const flushInterval = setInterval(() => {
    flushSendBuffer()
  }, 10)

  stream.pipeline(
    inputStreamDecoder.getInputStream(),
    inputStreamDecoder.makeInputTransformer(),
    new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform(message, encoding, done) {
        messagesToFilter++
        filterProgressBar.setTotal(messagesToFilter)
        sendBuffer.push(Buffer.from(message).toString(workerEncoding))
        done()
      },

      flush(done) {
        flushSendBuffer()
        done()
      },
    }),
    err => {
      clearInterval(flushInterval)
      
      const index = sendIndex++
      forkedWorkers[index%forkedWorkers.length].send({index,messages:sendBuffer})
      sendBuffer = []
      inputExhausted = true

      flushSendBuffer()
    })

  process.on('exit', function () {
    progressBars.stop()
    if(!showProgressBar) {
      process.stderr.write(`Transformed ${messagesTransformed} of ${messagesFiltered} messages${os.EOL}`)
    }
  })
}

function transformInForkedProcess(inputStreamDecoder, outputStreamEncoder, filter, shape) {
  process.on('SIGINT', () => {
    process.exit()
  })

  stream.pipeline(
    readMessagesFromParent(),
    new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform({index, messages}, encoding, done) {
        const result = {index, messages: [], progress: messages.length}
        for(const message of messages) {
          try {
            const jsonObject = inputStreamDecoder.unmarshalJsonObject(Buffer.from(message, workerEncoding))
            if(filter(jsonObject)) {
              const shapedJsonObject = shape(jsonObject)
              result.messages.push(outputStreamEncoder.marshalJsonObject(shapedJsonObject).toString(workerEncoding))
            }
          } catch(ex) {
            console.error(ex)
          }
        }
        this.push(result)
        done()
      }
    }),
    sendMessagesToParent(),
    err => {
      if(err) {
        console.error(err)
      }
    })
}