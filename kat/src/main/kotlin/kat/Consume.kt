package kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import kat.kafka.Brokers
import kat.kafka.EarliestOffsetSpec
import kat.kafka.EphemeralConsumer
import kat.kafka.LatestOffsetSpec
import kat.kafka.TimestampOffsetSpec
import kat.kafka.TopicIterator
import java.nio.ByteBuffer
import java.time.Instant

class Consume : KafkaTopicCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  val seek by argument("seek", "one of 'earliest', 'latest', or 'timestamp'").default("earliest")
  val timestamp by argument("timestamp", "the epoch milliseconds timestamp to seek to").default(Instant.now().toEpochMilli().toString())
  val encoding by option()

  override fun run() {
    val ephemeralConsumer = EphemeralConsumer(*Brokers.from(brokers))

    val sizeBufferArray = ByteArray(4)
    val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

    val offsetSpec = when(seek.trim().toLowerCase()) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "timestamp" -> TimestampOffsetSpec(timestamp.toLong())
      else -> throw RuntimeException("Unexpected seek option - $seek")
    }

    TopicIterator(ephemeralConsumer, topic, offsetSpec).forEach { record ->
      // TODO: move swithcing to a lambda return func
      when(encoding) {
        "hex" -> {
          record.value().map { String.format("%02X", it) }.forEach { System.out.print(it) }
          println()
        }
        else -> {
          sizeBuffer.putInt(0, record.value().size)
          System.out.write(sizeBufferArray)
          System.out.write(record.value())
        }
      }
    }
  }
}
