package kat.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration
import java.util.UUID

class EphemeralConsumer(vararg brokers: Broker) {
  private val kafkaConsumer = KafkaConsumer<ByteArray,ByteArray>(mapOf(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokers.joinToString(","),
    ConsumerConfig.CLIENT_ID_CONFIG to "kat",
    ConsumerConfig.GROUP_ID_CONFIG to "kat-${UUID.randomUUID()}",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false
  ))

  private fun getTopicMetadata(topic:String):List<PartitionInfo>  {
    var allTopics: Map<String, List<PartitionInfo>> = emptyMap()
    // Sometimes the first call will return nothing.
    while(allTopics.isEmpty()) {
      allTopics = kafkaConsumer.listTopics()
    }
    return allTopics.getOrDefault(topic, emptyList())
  }



  fun consumeFrom(topic:String, offsetSpec:OffsetSpec, handleRecord: (ConsumerRecord<ByteArray, ByteArray>) -> Boolean) {
    val topicMetadata = getTopicMetadata(topic)
    if(topicMetadata.isEmpty()) {
      throw NoSuchTopicException(topic)
    }

    val partitions = topicMetadata.map { TopicPartition(it.topic(), it.partition()) }

    val offsets = offsetSpec.getOffsets(kafkaConsumer, partitions)

    kafkaConsumer.assign(partitions)
    offsets.forEach(kafkaConsumer::seek)

    var keepGoing = true
    while (keepGoing) {
      val batch = kafkaConsumer.poll(Duration.ofSeconds(1))
      batch.records(topic).forEach { record -> keepGoing = handleRecord(record) }
    }
  }
}
