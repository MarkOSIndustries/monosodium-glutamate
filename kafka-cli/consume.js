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
  const kafkaClient = await kafka.getKafkaClient(kafkaBrokers)
  const topicPartitionOffsets = await offsetLookup[seekType](kafkaClient, topics, timestamp)
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)

  let keepGoing = true
  process.once('SIGINT', () => keepGoing = false)
  process.once('SIGTERM', () => keepGoing = false)

  await kafka.consumeFromOffsets(kafkaClient, topicPartitionOffsets, (message) => {
    console.log(schema.decode(message.value).toJSON())
    return keepGoing
  })
  kafkaClient.close()
}
