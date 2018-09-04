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

function _getOffsets(client, topics, time) {
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
        // Filter out topics with 'null' partition results - these indicate failure and mean the numbers wont make sense
        resolve(Object.assign({}, ...Object.keys(offsets).filter(topic => !offsets[topic].hasOwnProperty('null')).map(topic => ({[topic]:offsets[topic]}))))
      }
    })
  })
}

async function _flattenToTopicPartitionOffsets(offsets) {
  const topicPartitionOffsets = []
  Object.keys(offsets).forEach(topic => {
    topicPartitionOffsets.push(...Object.keys(offsets[topic])
          .map(partition => Number(partition))
          .filter(partition => !Number.isNaN(partition))
          .map(partition => ({ topic, partition: Number(partition), offset: offsets[topic][partition][0] })))
  })
  return topicPartitionOffsets
}

const OFFSET_EARLIEST = -2
const OFFSET_LATEST = -1

async function getOffsetsAtTime(client, topics, time) {
  const [timeBasedOffsets, earliestOffsets] = await Promise.all([_getOffsets(client, topics, time), _getOffsets(client, topics, OFFSET_EARLIEST)])
  // Merge time-based with earliest, preferrin time-based
  return _flattenToTopicPartitionOffsets(
    Object.assign({}, ...topics.map(topic => ({[topic]: Object.assign({}, earliestOffsets[topic], timeBasedOffsets[topic])})))
  )
}

async function getLatestOffsets(client, topics) {
  return _flattenToTopicPartitionOffsets(await _getOffsets(client, topics, OFFSET_LATEST))
}

async function getEarliestOffsets(client, topics) {
  return _flattenToTopicPartitionOffsets(await _getOffsets(client, topics, OFFSET_EARLIEST))
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

function produceRandomlyAcrossPartitions(client, topic) {
  const producer = new kafka.Producer(client, {
    requireAcks: 1,
    ackTimeoutMs: 100,
    // Partitioner type (default = 0, random = 1, cyclic = 2, keyed = 3, custom = 4), default 0
    partitionerType: 1
  })

  return {
    produce: function(messages) {
      return new Promise((resolve,reject) => {
        producer.send([{
          topic,
          messages
        }], (err, data) => {
          if(err) {
            reject(err)
          } else {
            resolve()
          }
        })
      })
    }
  }
}
