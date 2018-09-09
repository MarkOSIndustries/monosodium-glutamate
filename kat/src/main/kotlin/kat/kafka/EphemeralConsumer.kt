package kat.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration
import java.util.*

class EphemeralConsumer(vararg brokers: Broker) {
  class NoSuchTopicException(topic:String) : Exception("Couldn't find partitions for topic $topic")

  private val kc = KafkaConsumer<ByteArray,ByteArray>(mapOf(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
    ConsumerConfig.GROUP_ID_CONFIG to "ckc-${UUID.randomUUID()}",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false
  ) as Map<String, Any>?)

  private fun getTopicMetadata(topic:String):List<PartitionInfo>  {
    var allTopics: Map<String, List<PartitionInfo>> = emptyMap()
    // Sometimes the first call will return nothing.
    while(allTopics.isEmpty()) {
      allTopics = kc.listTopics()
    }
    return allTopics.getOrDefault(topic, emptyList())
  }



  fun consumeFrom(topic:String, offsetSpec:OffsetSpec, handleRecord: (ConsumerRecord<ByteArray, ByteArray>) -> Boolean) {
    val topicMetadata = getTopicMetadata(topic)
    if(topicMetadata.isEmpty()) {
      throw NoSuchTopicException(topic)
    }

    val partitions = topicMetadata.map { TopicPartition(it.topic(), it.partition()) }

    val offsets = offsetSpec.getOffsets(kc, partitions)

    kc.assign(partitions)
    offsets.forEach(kc::seek)

    var keepGoing = true
    while (keepGoing) {
      val batch = kc.poll(Duration.ofSeconds(1))
      batch.records(topic).forEach { record -> keepGoing = handleRecord(record) }
    }
  }
}
