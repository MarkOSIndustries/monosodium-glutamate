package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import msg.kafka.KafkaTopicCommand
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.topicPartitions
import java.time.Duration

class Offsets : KafkaTopicCommand("Query offsets by timestamp\nRetrieves the offsets for each partition at a given timestamp and prints to stdout") {
  val timestamp by argument("timestamp", "the epoch milliseconds timestamp to find offsets from")

  override fun run() {
    val consumer = newConsumer()
    val partitions = consumer.topicPartitions(topic, Duration.ofMinutes(1))

    TimestampOffsetSpec(timestamp.toLong()).getOffsetsWithTimestamps(consumer, partitions).forEach {
      if(it.value == null) {
        println("${it.key} has no offset after $timestamp")
      } else {
        println("${it.key} has offset ${it.value!!.offset()} at ${it.value!!.timestamp()}")
      }
    }
  }
}

