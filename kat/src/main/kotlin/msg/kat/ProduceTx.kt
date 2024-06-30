package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.EOFException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class ProduceTx : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Produce records to Kafka using transactions

  Reads records from stdin and sends them to Kafka
  """.trimIndent()

  private val commit by option("-c", "--commit", help = "How many records should be sent per transaction").int().default(5)
  private val limit by option("--limit", "-l", help = "the maximum number of messages to produce").long().default(Long.MAX_VALUE)

  override fun run() {
    val producer = newProducer(
      ByteArraySerializer::class, ByteArraySerializer::class,
      ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
      ProducerConfig.TRANSACTIONAL_ID_CONFIG to UUID.randomUUID().toString()
    )
    producer.initTransactions()

    val producedCount = AtomicInteger(0)
    val commitCount = AtomicInteger(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Produced $producedCount messages, $commitCount commits.")
      }
    )

    val reader = delimiter().reader(System.`in`)

    var recordsInTransaction = 0
    producer.beginTransaction()
    try {
      while (reader.hasNext() && producedCount.getAndIncrement() < limit) {
        if (recordsInTransaction++ == commit) {
          producer.commitTransaction()
          commitCount.incrementAndGet()
          recordsInTransaction = 0
          producer.beginTransaction()
        }
        val bytes = reader.next()
        producer.send(encoding.toProducerRecord(topic, bytes))
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    } finally {
      producer.commitTransaction()
      commitCount.incrementAndGet()
    }
  }
}
