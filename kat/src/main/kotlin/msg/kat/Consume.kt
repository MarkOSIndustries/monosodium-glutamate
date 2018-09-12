package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.Brokers
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.EphemeralConsumer
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import msg.kafka.TopicIterator
import msg.kafka.offsets.MaxOffsetSpec
import msg.kafka.offsets.OffsetSpec
import java.time.Instant

class Consume : KafkaTopicCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  private val makeOffsetSpec:Map<String,(Long)->OffsetSpec> = mapOf(
    "earliest" to { _ -> EarliestOffsetSpec() },
    "latest" to { _ ->LatestOffsetSpec() },
    "timestamp" to { timestamp ->TimestampOffsetSpec(timestamp) },
    "ts" to { timestamp ->TimestampOffsetSpec(timestamp) },
    "forever" to { _ ->MaxOffsetSpec() }
  )

  private val emitters = mapOf(
    "hex" to Emitters.lineDelimitedHexValues(System.out),
    "base64" to Emitters.lineDelimitedBase64Values(System.out),
    "binary" to Emitters.lengthPrefixedBinaryValues(System.out),
    "msg.KafkaRecord" to Emitters.lengthPrefixedKafkaRecords(System.out),
    "msg.TypedKafkaRecord" to Emitters.lengthPrefixedTypedKafkaRecords(System.out)
  )

  private val encoding by argument(help = "the format to emit records in on stdout").choice(*emitters.keys.toTypedArray()).default("binary")
  private val startOffsets by argument(help = "which offsets to start at (ts is shorthand for timestamp)").choice("earliest","latest","timestamp","ts").default("earliest")
  private val endOffsets by argument(help = "which offsets to stop at (ts is shorthand for timestamp)").choice("forever","latest","timestamp","ts").default("forever")
  private val fromTime by option("--from", "-f", "--start", "-s", help = "earliest epoch milliseconds timestamp to fetch (exclusive)").long().default(Instant.now().toEpochMilli())
  private val untilTime by option("--until", "-u", "--end", "-e", help = "latest epoch milliseconds timestamp to fetch (inclusive)").long().default(Instant.now().toEpochMilli())

  override fun run() {
    val ephemeralConsumer = EphemeralConsumer(*Brokers.from(brokers))
    val from = makeOffsetSpec[startOffsets]!!(fromTime)
    val until = makeOffsetSpec[endOffsets]!!(untilTime)
    val emitter = emitters[encoding]!!

    TopicIterator(ephemeralConsumer, topic, from, until).forEach(emitter)
  }
}
