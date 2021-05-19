package msg.kafka.offsets

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.TopicPartition

class TimestampOffsetSpec(private val timestampEpochMs: Long) : TimeBasedOffsetSpec {
  override fun <K, V> getOffsets(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, Long> {
    val latestOffsets = consumer.endOffsets(topicPartitions.toList())

    return getOffsetsWithTimestamps(consumer, topicPartitions)
      .mapValues { it.value?.offset() ?: latestOffsets[it.key]!! }
  }

  override fun <K, V> getOffsetsWithTimestamps(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, OffsetAndTimestamp?> {
    return consumer
      .offsetsForTimes(topicPartitions.associateWith { timestampEpochMs })
  }
}
