package msg.kafka.offsets

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.TopicPartition

interface TimeBasedOffsetSpec : OffsetSpec {
  fun <K, V> getOffsetsWithTimestamps(consumer: Consumer<K, V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, OffsetAndTimestamp?>
}
