package msg.kat

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.TopicIterator
import msg.kafka.offsets.OffsetSpecs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.requests.IsolationLevel
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCompletionCandidates
class Consume : KafkaTopicDataCommand(
  help = "Consume records from Kafka\n\n" +
    "Reads records from Kafka and emits length-prefixed binary records on stdout"
) {
  private val schema by option("--schema", "-s", help = "the schema name to embed in output records. Only works with --encoding msg.TypedKafkaRecord", metavar = "uses topic name by default")
  private val fromOption by option(
    "--from", "-f",
    help = "which offsets to start from",
    metavar = "[${OffsetSpecs.validFromSpecs.joinToString("|")}]",
    completionCandidates = CompletionCandidates.Fixed(OffsetSpecs.earliest, OffsetSpecs.latest)
  ).default(OffsetSpecs.earliest).validate {
    require(OffsetSpecs.parseOffsetSpec(it, topic) != null) {
      "$it isn't a valid offset to start from.\n" +
        "Please choose from $metavar"
    }
  }
  private val untilOption by option(
    "--until", "-u",
    help = "which offsets to end at",
    metavar = "[${OffsetSpecs.validUntilSpecs.joinToString("|")}]",
    completionCandidates = CompletionCandidates.Fixed(OffsetSpecs.latest, OffsetSpecs.forever)
  ).default(OffsetSpecs.forever).validate {
    require(OffsetSpecs.parseOffsetSpec(it, topic) != null) {
      "$it isn't a valid offset to end at.\n" +
        "Please choose from $metavar"
    }
  }
  private val isolation by option("--isolation", "-i", help = "the isolation level to read with").choice(IsolationLevel.values().map { it.toString().toLowerCase(Locale.ROOT) to it }.toMap()).default(IsolationLevel.READ_COMMITTED)
  private val limit by option("--limit", "-l", help = "the maximum number of messages to receive").long().default(Long.MAX_VALUE)

  override fun run() {
    val write = encoding.writer(System.out)

    val interrupted = CompletableFuture<Unit>()

    var receivedCount = AtomicInteger(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Received $receivedCount messages.")
      }
    )

    TopicIterator(
      newConsumer(
        ByteArrayDeserializer::class, ByteArrayDeserializer::class,
        ConsumerConfig.ISOLATION_LEVEL_CONFIG to isolation.toString().toLowerCase(Locale.ROOT)
      ),
      topic,
      OffsetSpecs.parseOffsetSpec(fromOption, topic)!!,
      OffsetSpecs.parseOffsetSpec(untilOption, topic)!!,
      interrupted
    ).forEach {
      write(encoding.fromConsumerRecord(it, schema ?: topic))
      if (receivedCount.incrementAndGet() >= limit) {
        interrupted.complete(Unit)
      }
    }
  }
}
