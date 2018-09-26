package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import msg.kafka.KafkaTopicCommand
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration

class Offsets : KafkaTopicCommand("Query offsets by timestamp\nRetrieves the offsets for each partition at a given timestamp and prints to stdout") {
  val timestamp by argument("timestamp", "the epoch milliseconds timestamp to find offsets from, or one of the strings 'latest' or 'earliest'")

  override fun run() {
    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    val partitions = consumer.topicPartitions(topic, Duration.ofMinutes(1))

    val offsetSpec: OffsetSpec = when(timestamp) {
      "latest" -> LatestOffsetSpec()
      "earliest" -> EarliestOffsetSpec()
      else -> TimestampOffsetSpec(timestamp.toLong())
    }

    if(offsetSpec is TimestampOffsetSpec) {
      offsetSpec.getOffsetsWithTimestamps(consumer, partitions).forEach {
        if(it.value == null) {
          println("${it.key} has no offset after $timestamp")
        } else {
          println("${it.key} has offset ${it.value!!.offset()} at ${it.value!!.timestamp()}")
        }
      }
    } else {
      offsetSpec.getOffsets(consumer, partitions).forEach {
        println("${it.key} has offset ${it.value}")
      }
    }
  }
}

