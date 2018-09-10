package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

class TimestampOffsetSpec(val timestampEpochMs: Long) : OffsetSpec {
  override fun <K,V>getOffsets(consumer:Consumer<K,V>, topicPartitions: List<TopicPartition>): Map<TopicPartition, Long> {
    val latestOffsets = consumer.endOffsets(topicPartitions.toList())

    return consumer
      .offsetsForTimes(topicPartitions.map { it to timestampEpochMs }.toMap())
      .mapValues {
        if(it.value != null) {
          it.value.offset()
        } else {
          latestOffsets[it.key]!!
        }
      }
  }
}
