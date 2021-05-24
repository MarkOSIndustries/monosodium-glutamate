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

function transformInParentProcess(inputStreamDecoder, outputStreamEncoder, filter, shape, template, forkedWorkerCount, forkedWorkerArgs) {
  var messagesTransformed = 0
  var sendIndex = 0
  var recvIndex = 0
  var inputExhausted = false
  const workBuffer = new Map()
  const forkedWorkers = []

  function shutdown() {
    for (const forkedWorker of forkedWorkers) {
      forkedWorker.send({finished: true})
    }
    process.exit()
  }

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

  forkedWorkers.push(...Array(forkedWorkerCount).fill().map((_, workerIndex) => {
    const forkedWorker = fork(process.argv[1], forkedWorkerArgs)
    forkedWorker.on('error', (err) => {
      shutdown()
    })
    forkedWorker.on('exit', (code, signal) => {
      if(code != 0) {
        shutdown()
      }
    })
    process.on('SIGINT', () => forkedWorker.kill('SIGINT'))

    readMessagesFromForked(forkedWorker).pipe(processedMessages)

    return forkedWorker
  }))

  const batchSize = 100
  var sendBuffer = []

  const streamToWorkers = inputStreamDecoder.makeInputStream()
    .pipe(new stream.Transform({
      readableObjectMode: true,
      
      transform(message, encoding, done) {
        sendBuffer.push(Buffer.from(message).toString(workerEncoding))
        if(sendBuffer.length >= batchSize) {
          const index = sendIndex++
          forkedWorkers[index%forkedWorkers.length].send({index,messages:sendBuffer})
          sendBuffer = []
        }
        done()
      }
    }))
  streamToWorkers.on('finish', () => {
    streamToWorkers.end()
    
    const index = sendIndex++
    forkedWorkers[index%forkedWorkers.length].send({index,messages:sendBuffer})
    sendBuffer = []
    inputExhausted = true
  })

  process.on('exit', function () {
    process.stderr.write(`Transformed ${messagesTransformed} messages${os.EOL}`)
  })
}

function transformInForkedProcess(inputStreamDecoder, outputStreamEncoder, filter, shape, template) {
  readMessagesFromParent()
    .pipe(new stream.Transform({
      readableObjectMode: true,
      writableObjectMode: true,
      
      transform({index, messages, finished}, encoding, done) {
        if(finished) {
          process.exit()
        } else {
          const result = {index, messages: []}
          for(const message of messages) {
            const jsonObject = inputStreamDecoder.unmarshalJsonObject(Buffer.from(message, workerEncoding))
            if(filter(jsonObject)) {
              const shapedJsonObject = shape(jsonObject)
              result.messages.push(outputStreamEncoder.marshalJsonObject(shapedJsonObject).toString(workerEncoding))
            }
          }
          this.push(result)
        }
        done()
      }
    }))
    .pipe(sendMessagesToParent())
}