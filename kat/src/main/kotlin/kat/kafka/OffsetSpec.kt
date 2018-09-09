package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

interface OffsetSpec {
  fun <K,V>getOffsets(consumer:Consumer<K,V>, topicPartitions: List<TopicPartition>) : Map<TopicPartition,Long>
}
