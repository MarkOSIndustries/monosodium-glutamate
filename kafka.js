const kafka = require('kafka-node')
const { rng } = require('crypto')

module.exports = {
  getKafkaClient,
  getOffsetsAtTime,
  getEarliestOffsets,
  getLatestOffsets,
  consumeFromOffsets,
  produceRandomlyAcrossPartitions,
}

const clientId = 'monosodiumglutamate'

function getKafkaClient(brokers) {
  return new Promise((resolve,reject) => {
    const client = new kafka.KafkaClient({
      clientId: `${clientId}-${rng(4).readUInt32LE()}`,
      kafkaHost: brokers.map(b => `${b.host}:${b.port}`).join(','),
      autoConnect: false,
    })
    client.on('ready', () => {
      resolve(client)
    })
    client.on('error', (err) => {
      reject(err)
    })
    client.connect()
  })
}

function getOffsetsAtTime(client, topics, time) {
  return new Promise((resolve,reject) => {
    const offset = new kafka.Offset(client)

    const fetchRequests = []
    topics.forEach(topic => {
      fetchRequests.push(...Object.keys(client.topicMetadata[topic]).map(partition => ({ topic, partition, time, maxNum: 1 })))
    })

    offset.fetch(fetchRequests, (err, offsets) => {
      if(err) {
        reject(err)
      } else {
        const topicPartitionOffsets = []
        topics.forEach(topic => {
          topicPartitionOffsets.push(...Object.keys(offsets[topic])
                .map(partition => Number(partition))
                .filter(partition => !Number.isNaN(partition))
                .map(partition => ({ topic, partition: Number(partition), offset: offsets[topic][partition][0] })))
        })
        resolve(topicPartitionOffsets)
      }
    })
  })
}

function getOffsetsAtTime(client, topics, time) {
  return new Promise((resolve,reject) => {
    const offset = new kafka.Offset(client)

    const fetchRequests = []
    topics.forEach(topic => {
      fetchRequests.push(...Object.keys(client.topicMetadata[topic]).map(partition => ({ topic, partition, time, maxNum: 1 })))
    })

    offset.fetch(fetchRequests, (err, offsets) => {
      if(err) {
        reject(err)
      } else {
        const topicPartitionOffsets = []
        topics.forEach(topic => {
          topicPartitionOffsets.push(...Object.keys(offsets[topic])
                .map(partition => Number(partition))
                .filter(partition => !Number.isNaN(partition))
                .map(partition => ({ topic, partition: Number(partition), offset: offsets[topic][partition][0] })))
        })
        resolve(topicPartitionOffsets)
      }
    })
  })
}

function getLatestOffsets(client, topics) {
  return getOffsetsAtTime(client, topics, -1)
}

function getEarliestOffsets(client, topics) {
  return getOffsetsAtTime(client, topics, -2)
}

function consumeFromOffsets(client, topicPartitionOffsets, messageCallback) {
  return new Promise((resolve,reject) => {
    const consumer = new kafka.Consumer(
        client,
        topicPartitionOffsets,
        {
            groupId: `${clientId}-${rng(4).readUInt32LE()}`,
            autoCommit: false,
            fromOffset: true,
            encoding: 'buffer',
            keyEncoding: 'buffer' // also supports 'utf8', probably others
        }
    )

    consumer.on('message', (message) => {
      try {
        if(!messageCallback(message)) {
          consumer.close()
          resolve()
        }
      } catch(ex) {
        reject(ex)
      }
    })
  })
}

function produceRandomlyAcrossPartitions(client, topic, messages) {
  return new Promise((resolve,reject) => {
    const producer = new kafka.Producer(client, {
      requireAcks: 1,
      ackTimeoutMs: 100,
      // Partitioner type (default = 0, random = 1, cyclic = 2, keyed = 3, custom = 4), default 0
      partitionerType: 1
    })

    producer.send([{
      topic,
      messages
    }], (err, data) => {
      console.log('Produce callback', err, data)
      if(err) {
        reject(err)
      } else {
        resolve()
      }
    })
  })
}
