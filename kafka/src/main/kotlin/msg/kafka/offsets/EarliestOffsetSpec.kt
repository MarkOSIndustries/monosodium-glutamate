package msg.kafka.offsets

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

class EarliestOffsetSpec : OffsetSpec {
  override fun <K,V>getOffsets(consumer:Consumer<K,V>, topicPartitions: Collection<TopicPartition>): Map<TopicPartition, Long> {
    return consumer.beginningOffsets(topicPartitions.toList())
  }
}
