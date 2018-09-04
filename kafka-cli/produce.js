const kafka = require('../kafka')
const protobuf = require('../protobuf')

module.exports = {
  produce
}

async function produce(kafkaBrokers, protobufPath, topic, schemaName) {
  const kafkaClient = await kafka.getKafkaClient(kafkaBrokers)
  const schema = protobuf.loadDirectory(protobufPath).lookupType(schemaName)
  const producer = kafka.produceRandomlyAcrossPartitions(kafkaClient, topic)

  process.stdin.setEncoding('utf8')

  let buffer = ''
  process.stdin.on('data', (chunk) => {
    if (chunk !== null) {
      buffer += chunk
      const lines = buffer.split(/[\r\n]/)
      buffer = lines.pop() // will be empty string if end char was newline

      producer.produce(lines.filter(l => l!=='').map(jsonMessage => {
        return schema.encode(schema.fromObject(JSON.parse(jsonMessage))).finish()
      }))
    }
  })

  process.stdin.on('end', () => {
    kafkaClient.close()
  })

  console.log(`Start typing ${schemaName} lines as JSON`)
}
