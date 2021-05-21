package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import msg.kafka.KafkaCommand
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer

class Topics : KafkaCommand(
  "Query topics\n\n" +
    "Retrieves the names of all topics from Kafka and prints to stdout"
) {
  private val query by argument(help = "Filter the topic list (case-insensitive, supports * wildcards)").default("*")
  private val not by option("--not", "-n", help = "Invert the filter to be exclusive rather than inclusive").flag(default = false)
  private val offsets by option("--offsets", "-o", help = "Include offset ranges for each topic").flag(default = false)

  override fun run() {
    val pattern = ".*${query.split("*").map { Regex.escape(it) }.joinToString(".*")}.*"
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)

    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    consumer.listTopics()
      .filterKeys { if (not) !regex.matches(it) else regex.matches(it) }
      .toSortedMap()
      .forEach { topicWithPartitionInfos ->
        println(topicWithPartitionInfos.key)
        if (offsets) {
          val topicPartitionsWithInfo = topicWithPartitionInfos.value.map { TopicPartition(it.topic(), it.partition()) to it }.toMap()
          val earliest = EarliestOffsetSpec().getOffsets(consumer, topicPartitionsWithInfo.keys)
          val latest = LatestOffsetSpec().getOffsets(consumer, topicPartitionsWithInfo.keys)
          earliest.forEach {
            val topicPartitionInfo = topicPartitionsWithInfo[it.key]!!
            println("  ${it.key} | Offset range [${it.value}, ${latest[it.key]}) | ISR <${topicPartitionInfo.inSyncReplicas().joinToString(",")}> | Leader ${topicPartitionInfo.leader()}")
          }
        }
      }
  }
}
