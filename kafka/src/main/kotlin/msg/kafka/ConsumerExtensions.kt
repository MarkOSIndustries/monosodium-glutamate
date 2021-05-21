package msg.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition

fun <K, V> Consumer<K, V>.topicPartitions(topic: String): List<TopicPartition> {
  return partitionsFor(topic).map {
    TopicPartition(it.topic(), it.partition())
  }
}
