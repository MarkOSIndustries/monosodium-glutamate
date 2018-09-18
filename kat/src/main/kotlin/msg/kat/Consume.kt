package msg.kat

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import msg.kafka.KafkaTopicCommand
import msg.kafka.TopicIterator
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.MaxOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import java.io.PrintStream
import java.time.Instant

class Consume : KafkaTopicCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  private val emitters = mapOf(
    "hex" to { out:PrintStream -> Emitters.lineDelimitedHexValues(out) },
    "base64" to { out:PrintStream -> Emitters.lineDelimitedBase64Values(out) },
    "binary" to { out:PrintStream -> Emitters.lengthPrefixedBinaryValues(out) },
    "msg.KafkaRecord" to { out:PrintStream -> Emitters.lengthPrefixedKafkaRecords(out) },
    "msg.TypedKafkaRecord" to { out:PrintStream -> Emitters.lengthPrefixedTypedKafkaRecords(out,schema ?: topic) }
  )

  private val encoding by option("--encoding", "-e", help = "the format to emit records in on stdout. HEX,Base64 are line delimited ASCII. Others are length-prefixed binary.").choice(*emitters.keys.toTypedArray()).default("msg.TypedKafkaRecord")
  private val schema by option("--schema", "-s", help = "the schema name to embed in output records. Only works with --encoding msg.TypedKafkaRecord", metavar = "uses topic name by default")
  private val startOffsetTypes = setOf("earliest","latest")
  private val fromOption by option("--from", "-f", help = "which offsets to start from", metavar="[${startOffsetTypes.joinToString("|")}|<timestampMs>]").default("earliest").validate {
    require(startOffsetTypes.contains(it) || it.toLongOrNull() != null) {"$it isn't a valid offset to start from.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}"}
  }
  private val endOffsetTypes = setOf("forever","latest")
  private val untilOption by option("--until", "-u", help = "which offsets to end at", metavar="[${endOffsetTypes.joinToString("|")}|<timestampMs>]").default("forever").validate {
    require(endOffsetTypes.contains(it) || it.toLongOrNull() != null) {"$it isn't a valid offset to end at.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}"}
  }

  override fun run() {
    TopicIterator(
      newConsumer(),
      topic,
      parseOffsetSpec(fromOption),
      parseOffsetSpec(untilOption)
    ).forEach(emitters[encoding]!!(System.out))
  }

  private fun parseOffsetSpec(spec:String):OffsetSpec {
    return when(spec) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "forever" -> MaxOffsetSpec()
      else -> TimestampOffsetSpec(spec.toLong())
    }
  }
}
