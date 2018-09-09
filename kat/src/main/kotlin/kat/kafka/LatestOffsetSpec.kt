package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

class LatestOffsetSpec() : OffsetSpec {
  override fun <K,V>getOffsets(consumer:Consumer<K,V>, topicPartitions: List<TopicPartition>): Map<TopicPartition, Long> {
    return consumer.endOffsets(topicPartitions.toList())
  }
}
