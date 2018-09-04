const kafka = require('../kafka')
const protobuf = require('../protobuf')

module.exports = {
  consume
}

const offsetLookup = {
  earliest: kafka.getEarliestOffsets,
  latest: kafka.getLatestOffsets,
  timestamp: kafka.getOffsetsAtTime,
}

async function consume(kafkaBrokers, protobufPath, topics, schemaName, seekType, timestamp) {
  if(process.stdout.isTTY) {
    console.table({
      mode: 'consume',
      schema: schemaName,
      topics: topics.join(', '),
    })
  }

  const kafkaClient = await kafka.getKafkaClient(kafkaBrokers)
  const topicPartitionOffsets = await offsetLookup[seekType](kafkaClient, topics, timestamp)
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)

  let keepGoing = true
  process.once('SIGINT', () => keepGoing = false)
  process.once('SIGTERM', () => keepGoing = false)

  await kafka.consumeFromOffsets(kafkaClient, topicPartitionOffsets, (message) => {
    const jsonMessage = schema.decode(message.value).toJSON()
    console.log({
        partition:message.partition,
        offset:message.offset,
        utc_time:message.timestamp,
      },
      JSON.stringify(jsonMessage)// TODO: can I do single line but keep the nice highlighting of console.log(jsonMessage) ?
    )
    return keepGoing
  })
  kafkaClient.close()
}
