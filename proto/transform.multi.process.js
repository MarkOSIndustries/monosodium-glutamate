const os = require('os')
const stream = require('stream')
const { fork } = require('child_process')
const {
  sendMessagesToParent,
  readMessagesFromParent,
  readMessagesFromForked,
} = require('../streams.js')

module.exports = {
  transformInParentProcess,
  transformInForkedProcess,
}

const workerEncoding = 'base64'

function transformInParentProcess(inputStreamDecoder, outputStreamEncoder, filter, shape, forkedWorkerCount, forkedWorkerArgs) {
  var messagesTransformed = 0
  var sendIndex = 0
  var recvIndex = 0
  var inputExhausted = false
  const workBuffer = new Map()
  const forkedWorkers = []

  function shutdown() {
    for (const forkedWorker of forkedWorkers) {
      forkedWorker.kill('SIGINT')
    }
    process.exit()
  }

  process.on('SIGINT', shutdown)

  var processedMessagesBuffer = []
  const processedMessages = new stream.Transform({
    readableObjectMode: true,
    writableObjectMode: true,
    
    transform({index, messages}, encoding, done) {
      workBuffer.set(index, messages)
      if(index == recvIndex) {
        while((messages = workBuffer.get(recvIndex))) {
          for(const message of messages) {
            this.push(Buffer.from(message, workerEncoding))
            messagesTransformed++
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

  processedMessages.pipe(outputStreamEncoder.makeOutputStream())

  processedMessages.setMaxListeners(forkedWorkerCount)

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

    readMessagesFromForked(forkedWorker).pipe(processedMessages)

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

  const streamToWorkers = inputStreamDecoder.makeInputStream()
    .pipe(new stream.Transform({
      readableObjectMode: true,
      
      transform(message, encoding, done) {
        sendBuffer.push(Buffer.from(message).toString(workerEncoding))
        done()
      },

      flush(done) {
        flushSendBuffer()
        done()
      },
    }))
  streamToWorkers.on('finish', () => {
    clearInterval(flushInterval)
    streamToWorkers.end()
    
    const index = sendIndex++
    forkedWorkers[index%forkedWorkers.length].send({index,messages:sendBuffer})
    sendBuffer = []
    inputExhausted = true

    flushSendBuffer()
  })

  process.on('exit', function () {
    process.stderr.write(`Transformed ${messagesTransformed} messages${os.EOL}`)
  })
}

function transformInForkedProcess(inputStreamDecoder, outputStreamEncoder, filter, shape) {
  process.on('SIGINT', () => {
    process.exit()
  })

  readMessagesFromParent()
    .pipe(new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform({index, messages}, encoding, done) {
        const result = {index, messages: []}
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
    }))
    .pipe(sendMessagesToParent())
}