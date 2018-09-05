const kafka = require('kafka-node')
const { rng } = require('crypto')
const stream = require('stream')

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
        // Filter out topics with 'null' partition results - these occur because of a bug in handling empty offset responses in kafka-node
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
    const msgs = new stream.Writable()
    msgs.options = { encoding: 'buffer', keyEncoding: 'buffer' } // make it look like a consumer

    const fetchMaxWaitMs = 100
    const fetchMinBytes = 1
    const fetchMaxBytes = 1024 * 1024 // 1Mb
    const fetchRequestsByTopicByPartition = {}
    topicPartitionOffsets.forEach(tpo => {
      tpo.maxBytes = fetchMaxBytes

      fetchRequestsByTopicByPartition[tpo.topic] = fetchRequestsByTopicByPartition[tpo.topic] || {}
      fetchRequestsByTopicByPartition[tpo.topic][tpo.partition] = tpo
    })

    const updateOffset = (msg) => fetchRequestsByTopicByPartition[msg.topic][msg.partition].offset = msg.offset+1
    const getMoreMessages = () => { client.sendFetchRequest(msgs, topicPartitionOffsets, fetchMaxWaitMs, fetchMinBytes) }
    var keepGoing = true
    var messageCount = 0
    msgs.on('message', (message) => {
      if(!keepGoing) return
      messageCount+=1
      updateOffset(message)
      keepGoing = Boolean(messageCallback(message))
    })
    msgs.on('done', () => {
      if(keepGoing) {
        getMoreMessages()
      } else {
        resolve({messageCount})
      }
    })

    getMoreMessages()
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
