package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

class TimestampOffsetSpec(val timestampEpochMs: Long) : OffsetSpec {
  override fun <K,V>getOffsets(consumer:Consumer<K,V>, topicPartitions: List<TopicPartition>): Map<TopicPartition, Long> {
    return consumer
      .offsetsForTimes(topicPartitions.map { it to timestampEpochMs }.toMap())
      .mapValues { it.value.offset() }
  }
}
