package msg.kafka

import msg.kafka.offsets.OffsetSpec
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.CompletableFuture

class TopicIterator<K, V>(private val consumer: Consumer<K, V>, private val topic: String, startOffsetInclusiveSpec: OffsetSpec, endOffsetExclusiveSpec: OffsetSpec, private val interrupted: CompletableFuture<Unit> = CompletableFuture()) : Iterator<ConsumerRecord<K, V>> {
  private val partitions = consumer.topicPartitions(topic, Duration.ofMinutes(1)).toMutableSet()
  private val startOffsets = startOffsetInclusiveSpec.getOffsets(consumer, partitions)
  private val endOffsets = endOffsetExclusiveSpec.getOffsets(consumer, partitions)
  private val records = LinkedList<ConsumerRecord<K, V>>()

  init {
    if (partitions.isEmpty()) {
      throw NoSuchTopicException(topic)
    }

    // Remove partitions we don't have a start AND end offset for
    partitions.removeAll(partitions.filterNot { startOffsets.keys.intersect(endOffsets.keys).contains(it) })

    // Remove partitions we'll never get records for
    partitions.removeAll(endOffsets.filterNot { startOffsets.getOrDefault(it.key, Long.MAX_VALUE) < it.value }.keys)

    consumer.assign(partitions)
    startOffsets.filterKeys(partitions::contains).forEach(consumer::seek)
    ensureQueueDoesntRunEmpty()
  }

  override fun hasNext(): Boolean {
    ensureQueueDoesntRunEmpty()
    return records.isNotEmpty() && !interrupted.isDone
  }

  override fun next(): ConsumerRecord<K, V> {
    ensureQueueDoesntRunEmpty()
    return records.pop()
  }

  private fun ensureQueueDoesntRunEmpty() {
    while (partitions.isNotEmpty() && records.isEmpty() && !interrupted.isDone) {
      if (partitions.size != consumer.assignment().size) {
        consumer.assign(partitions)
      }
      val batch = consumer.poll(Duration.ofSeconds(1))
      batch.records(topic).forEach { record ->
        val topicPartition = TopicPartition(record.topic(), record.partition())
        val endOffset = endOffsets.getOrDefault(topicPartition, 0)
        if (record.offset() < endOffset) {
          records.add(record)
        }
      }
      partitions.removeIf { consumer.position(it) >= endOffsets.getOrDefault(it, 0) }
    }
  }
}
