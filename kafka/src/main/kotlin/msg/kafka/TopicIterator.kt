package msg.kafka

import msg.kafka.offsets.OffsetSpec
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.CompletableFuture

class TopicIterator<K, V>(private val consumer: Consumer<K, V>, private val topic: String, startOffsetInclusiveSpec: OffsetSpec, endOffsetExclusiveSpec: OffsetSpec, private val interrupted: CompletableFuture<Unit> = CompletableFuture()) : Iterator<ConsumerRecord<K, V>> {
  private val partitions = consumer.topicPartitions(topic).toMutableSet()
  private val startOffsets = startOffsetInclusiveSpec.getOffsets(consumer, partitions)
  private val endOffsets = endOffsetExclusiveSpec.getOffsets(consumer, partitions)
  private val records = LinkedList<ConsumerRecord<K, V>>()

  init {
    if (partitions.isEmpty()) {
      throw NoSuchTopicException(topic)
    }

    val partitionIntersection = startOffsets.keys.intersect(endOffsets.keys)

    // Remove partitions we don't have a start AND end offset for
    partitions.removeAll(partitions.filterNot { partitionIntersection.contains(it) })

    // Remove partitions we'll never get records for
    partitions.removeAll(partitionIntersection.filterNot { startOffsets[it]!! < endOffsets[it]!! })

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
        val endOffset = endOffsets[topicPartition]!!
        if (record.offset() < endOffset) {
          records.add(record)
        }
      }
      partitions.removeIf { consumer.position(it) >= endOffsets[it]!! }
    }
  }
}
