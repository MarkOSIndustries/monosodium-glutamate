package msg.kat

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.TopicIterator
import msg.kafka.offsets.OffsetSpecs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.IsolationLevel
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class Consume : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Consume records from Kafka

  Reads records from Kafka and emits length-prefixed binary records on stdout
  """.trimIndent()

  private val schema by option("--schema", "-s", help = "the schema name to embed in output records. Only works with --encoding msg.TypedKafkaRecord", metavar = "uses topic name by default")
  private val fromOption by option(
    "--from", "-f",
    help = "which offsets to start from",
    metavar = "[${OffsetSpecs.validFromSpecs.joinToString("|")}]",
    completionCandidates = CompletionCandidates.Fixed(OffsetSpecs.earliest, OffsetSpecs.latest)
  ).default(OffsetSpecs.earliest).validate {
    require(OffsetSpecs.parseOffsetSpec(it, topic) != null) {
      """
        $it isn't a valid offset to start from.
        Please choose from ${metavar(context)}
      """.trimIndent()
    }
  }
  private val untilOption by option(
    "--until", "-u",
    help = "which offsets to end at",
    metavar = "[${OffsetSpecs.validUntilSpecs.joinToString("|")}]",
    completionCandidates = CompletionCandidates.Fixed(OffsetSpecs.latest, OffsetSpecs.forever)
  ).default(OffsetSpecs.forever).validate {
    require(OffsetSpecs.parseOffsetSpec(it, topic) != null) {
      """
        $it isn't a valid offset to end at.
        Please choose from ${metavar(context)}
      """.trimIndent()
    }
  }
  private val isolation by option("--isolation", "-i", help = "the isolation level to read with").choice(IsolationLevel.entries.associate { it.toString().lowercase(Locale.ROOT) to it }).default(IsolationLevel.READ_COMMITTED)
  private val limit by option("--limit", "-l", help = "the maximum number of messages to receive").long().default(Long.MAX_VALUE)

  override fun run() {
    val write = delimiter().writer(System.out)

    val interrupted = CompletableFuture<Unit>()

    val receivedCount = AtomicInteger(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Received $receivedCount messages.")
      }
    )

    TopicIterator(
      newConsumer(
        ByteArrayDeserializer::class, ByteArrayDeserializer::class,
        ConsumerConfig.ISOLATION_LEVEL_CONFIG to isolation.toString().lowercase(Locale.ROOT)
      ),
      topic,
      OffsetSpecs.parseOffsetSpec(fromOption, topic)!!,
      OffsetSpecs.parseOffsetSpec(untilOption, topic)!!,
      interrupted
    ).forEach {
      try {
        write(encoding.fromConsumerRecord(it, schema ?: topic))
        if (receivedCount.incrementAndGet() >= limit) {
          interrupted.complete(Unit)
        }
      } catch (t: IOException) {
        // Ignore, this will be either:
        // - we just terminated between hasNext and next()
        // - the output stream was closed
        interrupted.complete(Unit)
      }
    }
  }
}
