package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

fun <K, V> Consumer<K, V>.topicPartitions(topic: String, timeout: Duration): List<TopicPartition> {
  return partitionsFor(topic, timeout).map {
    TopicPartition(it.topic(), it.partition())
  }
}
