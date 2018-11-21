package msg.kat

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.TopicIterator
import msg.kafka.offsets.ConfiguredOffsetSpec
import msg.kafka.offsets.EarliestOffsetSpec
import msg.kafka.offsets.LatestOffsetSpec
import msg.kafka.offsets.MaxOffsetSpec
import msg.kafka.offsets.OffsetSpec
import msg.kafka.offsets.TimestampOffsetSpec
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import sun.misc.Signal
import java.time.Instant
import java.util.Locale
import java.util.concurrent.CompletableFuture

class Consume : KafkaTopicDataCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  private val schema by option("--schema", "-s", help = "the schema name to embed in output records. Only works with --encoding msg.TypedKafkaRecord", metavar = "uses topic name by default")
  private val startOffsetTypes = setOf("earliest","latest")
  private val fromOption by option("--from", "-f", help = "which offsets to start from", metavar="[${startOffsetTypes.joinToString("|")}|<timestampMs>|<csvOffsets>]").default("earliest").validate {
    require(startOffsetTypes.contains(it) || it.toLongOrNull() != null || it.contains(':')) {"$it isn't a valid offset to start from.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}, and <csvOffsets> means a list of comma separated <partition>:<offset> entries"}
  }
  private val endOffsetTypes = setOf("forever","latest")
  private val untilOption by option("--until", "-u", help = "which offsets to end at", metavar="[${endOffsetTypes.joinToString("|")}|<timestampMs>|<csvOffsets>]").default("forever").validate {
    require(endOffsetTypes.contains(it) || it.toLongOrNull() != null || it.contains(':')) {"$it isn't a valid offset to end at.\n" +
      "Please choose from $metavar where <timestampMs> means a timestamp like ${Instant.now().toEpochMilli()}, and <csvOffsets> means a list of comma separated <partition>:<offset> entries"}
  }
  private val isolation by option("--isolation", "-i", help = "the isolation level to read with").choice(IsolationLevel.values().map{ it.toString().toLowerCase(Locale.ROOT) to it }.toMap()).default(IsolationLevel.READ_COMMITTED)
  private val limit by option("--limit", "-l", help = "the maximum number of messages to receive").long().default(Long.MAX_VALUE)

  override fun run() {
    val write = encoding.writer(System.out)

    val interrupted = CompletableFuture<Unit>()
    Signal.handle(Signal("INT")) { interrupted.complete(Unit) }

    var receivedCount = 0
    TopicIterator(
      newConsumer(ByteArrayDeserializer::class,ByteArrayDeserializer::class,
        ConsumerConfig.ISOLATION_LEVEL_CONFIG to isolation.toString().toLowerCase(Locale.ROOT)),
      topic,
      parseOffsetSpec(fromOption),
      parseOffsetSpec(untilOption),
      interrupted
    ).forEach {
      write(encoding.fromConsumerRecord(it, schema ?: topic))
      if(++receivedCount >= limit) {
        interrupted.complete(Unit)
      }
    }

    System.err.println("Received $receivedCount messages.")
  }

  private fun parseOffsetSpec(spec:String):OffsetSpec {
    return when(spec) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "forever" -> MaxOffsetSpec()
      else -> {
        if(spec.contains(':')) {
          ConfiguredOffsetSpec(spec.split(',').map {
            val partitionAndOffset = it.split(':')
            TopicPartition(topic, partitionAndOffset[0].toInt()) to partitionAndOffset[1].toLong()
          }.toMap())
        } else {
          TimestampOffsetSpec(spec.toLong())
        }
      }
    }
  }
}
