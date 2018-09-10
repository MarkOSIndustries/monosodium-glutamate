package kat.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Duration
import java.util.LinkedList

class TopicIterator<K,V>(private val consumer: Consumer<K, V>, private val topic:String, offsetSpec:OffsetSpec) : Iterator<ConsumerRecord<K, V>> {
  val partitions = consumer.topicPartitions(topic, Duration.ofMinutes(1))
  val offsets = offsetSpec.getOffsets(consumer, partitions)
  val records = LinkedList<ConsumerRecord<K,V>>()

  init {
    if(partitions.isEmpty()) {
      throw NoSuchTopicException(topic)
    }
    consumer.assign(partitions)
    offsets.forEach(consumer::seek)
  }

  override fun hasNext() = true

  override fun next(): ConsumerRecord<K, V> {
    while(records.isEmpty()) {
      val batch = consumer.poll(Duration.ofSeconds(10))
      batch.records(topic).forEach(records::push)
    }
    return records.pop()
  }
}
