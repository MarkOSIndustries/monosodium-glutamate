package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import msg.kafka.KafkaTopicCommand
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.OffsetSpecs
import msg.kafka.offsets.TimeBasedOffsetSpec
import msg.kafka.topicPartitions
import org.apache.kafka.common.serialization.ByteArrayDeserializer

class Offsets : KafkaTopicCommand() {
  override fun help(context: Context) = """
  Query offsets by timestamp

  Retrieves the offsets for each partition at a given timestamp and prints to stdout
  """.trimIndent()

  private val at by argument("at", "the query to use to find offsets choose from [${OffsetSpecs.validFromSpecs.joinToString("|")}]").validate {
    require(OffsetSpecs.parseOffsetSpec(it, topic) != null) {
      "$it isn't a valid offset query.\n" +
        "Please choose from [${OffsetSpecs.validFromSpecs.joinToString("|")}]"
    }
  }

  override fun run() {
    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    val partitions = consumer.topicPartitions(topic)

    val offsetSpec: OffsetSpec = OffsetSpecs.parseOffsetSpec(at, topic)!!

    if (offsetSpec is TimeBasedOffsetSpec) {
      offsetSpec.getOffsetsWithTimestamps(consumer, partitions).entries.sortedBy { it.key.partition() }.forEach {
        if (it.value == null) {
          println("${it.key} has no offset after $at")
        } else {
          println("${it.key} has offset ${it.value!!.offset()} at ${it.value!!.timestamp()}")
        }
      }
    } else {
      offsetSpec.getOffsets(consumer, partitions).entries.sortedBy { it.key.partition() }.forEach {
        println("${it.key} has offset ${it.value}")
      }
    }
  }
}
