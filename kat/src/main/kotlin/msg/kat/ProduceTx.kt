package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import msg.kat.encodings.KafkaRecordTransport
import msg.progressbar.NoopProgressBar
import msg.progressbar.StderrProgressBar
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.EOFException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ProduceTx : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Produce records to Kafka using transactions

  Reads records from stdin and sends them to Kafka
  """.trimIndent()

  private val commit by option("-c", "--commit", help = "How many records should be sent per transaction").int().default(500)
  private val limit by option("--limit", "-l", help = "the maximum number of messages to produce").long().default(Long.MAX_VALUE)

  override fun run() {
    val producer = newProducer(
      ByteArraySerializer::class, ByteArraySerializer::class,
      ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
      ProducerConfig.TRANSACTIONAL_ID_CONFIG to UUID.randomUUID().toString()
    )
    producer.initTransactions()

    val producedCount = AtomicLong(0L)
    val commitCount = AtomicLong(0L)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Produced $producedCount messages, $commitCount commits.")
      }
    )

    val progressBar = if (progress) StderrProgressBar(this.commandName) else NoopProgressBar()
    if (limit != Long.MAX_VALUE) {
      progressBar.setTotal(limit)
    }

    val transport = KafkaRecordTransport(topic, topic)
    val reader = transport.reader(encoding(prefix), System.`in`)

    progressBar.use {
      var recordsInTransaction = 0
      producer.beginTransaction()
      try {
        while (reader.hasNext() && producedCount.get() < limit) {
          val producerRecord = reader.next()
          producer.send(producerRecord)
          progressBar.setProgress(producedCount.incrementAndGet())
          if (recordsInTransaction++ == commit) {
            producer.commitTransaction()
            commitCount.incrementAndGet()
            recordsInTransaction = 0
            producer.beginTransaction()
          }
        }
      } catch (t: EOFException) {
        // Ignore, we just terminated between hasNext and next()
      } finally {
        producer.commitTransaction()
        commitCount.incrementAndGet()
        progressBar.setProgress(producedCount.get())
      }
    }
  }
}
