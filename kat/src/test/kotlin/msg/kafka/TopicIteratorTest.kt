package msg.kafka

import msg.kafka.offsets.ConfiguredOffsetSpec
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.OffsetSpec
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.Node
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TopicIteratorTest {
  private val topic = "topic_iterator_test"

  @Test
  fun should_iterate_from_earliest_to_latest() {
    test_bounded_range(topic, 2, 1000, 2000,
      EarliestOffsetSpec(),
      LatestOffsetSpec())
  }
  @Test
  fun should_iterate_from_earliest_to_specific_offset() {
    test_bounded_range(topic, 2, 1000, 400,
      EarliestOffsetSpec(),
      ConfiguredOffsetSpec(mapOf(TopicPartition(topic, 0) to 400L)))
  }
  @Test
  fun should_iterate_from_specific_offset_to_latest() {
    test_bounded_range(topic, 2, 1000, 600,
      ConfiguredOffsetSpec(mapOf(TopicPartition(topic, 0) to 400L)),
      LatestOffsetSpec())
  }
  @Test
  fun should_iterate_from_specific_offset_to_specific_offset() {
    test_bounded_range(topic, 2, 1000, 200,
      ConfiguredOffsetSpec(mapOf(TopicPartition(topic, 0) to 400L)),
      ConfiguredOffsetSpec(mapOf(TopicPartition(topic, 0) to 600L)))
  }

  fun test_bounded_range(topic: String, partitions: Int, recordsPerPartition: Long, expectedRecordCount: Int, from: OffsetSpec, until: OffsetSpec) {
    val mockConsumer = MockConsumer<String, String>(OffsetResetStrategy.EARLIEST)
    val records = mockConsumer.initTopic(topic, partitions, recordsPerPartition)

    val topicIterator = TopicIterator(mockConsumer, topic, from, until)
    var actualRecordCount = 0
    topicIterator.forEach { record ->
      actualRecordCount++
      assertTrue(records.contains(record))
    }
    assertEquals(expectedRecordCount, actualRecordCount)
  }
}

private fun MockConsumer<String, String>.initTopic(topic: String, partitions: Int, recordsPerPartition: Long): List<ConsumerRecord<String, String>> {
  val topicPartitions = (0 until partitions).map { TopicPartition(topic, it) }

  this.assign(topicPartitions)
  this.updateBeginningOffsets(topicPartitions.map { it to 0L }.toMap())
  this.updateEndOffsets(topicPartitions.map { it to recordsPerPartition }.toMap())
  this.updatePartitions(topic, topicPartitions.map { PartitionInfo(it.topic(), it.partition(), Node.noNode(), arrayOf(Node.noNode()), arrayOf(Node.noNode())) })

  val result = ArrayList<ConsumerRecord<String, String>>()
  for (topicPartition in topicPartitions) {
    for (offset in 0 until recordsPerPartition) {
      val record = ConsumerRecord(topicPartition.topic(), topicPartition.partition(), offset, "key$offset", "value$offset")
      result.add(record)
      this.addRecord(record)
    }
  }
  return result
}
