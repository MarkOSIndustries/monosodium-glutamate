package msg.kafka.offsets

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.TopicPartition

class TimestampAndPartitionsOffsetSpec(private val timestampEpochMs: Long, private val configuredPartitions: Collection<TopicPartition>) : TimeBasedOffsetSpec {
  override fun <K, V> getOffsets(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, Long> {
    val intersectedTopicPartitions = topicPartitions.intersect(configuredPartitions).toList()
    val latestOffsets = consumer.endOffsets(intersectedTopicPartitions)

    return getOffsetsWithTimestamps(consumer, intersectedTopicPartitions)
      .mapValues { it.value?.offset() ?: latestOffsets[it.key]!! }
  }

  override fun <K, V> getOffsetsWithTimestamps(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, OffsetAndTimestamp?> {
    val intersectedTopicPartitions = topicPartitions.intersect(configuredPartitions).toList()
    return consumer
      .offsetsForTimes(intersectedTopicPartitions.associateWith { timestampEpochMs })
  }
}
