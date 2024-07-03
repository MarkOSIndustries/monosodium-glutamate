package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import msg.kat.encodings.KafkaRecordTransport
import msg.progressbar.NoopProgressBar
import msg.progressbar.StderrProgressBar
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.EOFException
import java.util.LinkedList
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

class Produce : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Produce records to Kafka

  Reads records from stdin and sends them to Kafka
  """.trimIndent()

  private val limit by option("--limit", "-l", help = "the maximum number of messages to produce").long().default(Long.MAX_VALUE)

  override fun run() {
    val producer = newProducer(ByteArraySerializer::class, ByteArraySerializer::class)

    val futures = LinkedList<Future<RecordMetadata>>()

    val producedCount = AtomicLong(0L)
    val ackedCount = AtomicLong(0L)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Produced $producedCount messages, $ackedCount ACKed.")
      }
    )

    val progressBar = if (progress) StderrProgressBar(this.commandName) else NoopProgressBar()
    if (limit != Long.MAX_VALUE) {
      progressBar.setTotal(limit)
    }

    progressBar.use {
      try {
        val transport = KafkaRecordTransport(topic, topic)
        val reader = transport.reader(encoding(prefix), System.`in`)
        while (reader.hasNext() && producedCount.get() < limit) {
          val producerRecord = reader.next()
          futures.add(producer.send(producerRecord))
          producedCount.incrementAndGet()

          while (futures.isNotEmpty() && futures.first.isDone) {
            futures.pop().get() // make the future throw its exception if any
            progressBar.setProgress(ackedCount.incrementAndGet())
          }
        }
      } catch (t: EOFException) {
        // Ignore, we just terminated between hasNext and next()
      }

      while (futures.isNotEmpty()) {
        futures.pop().get() // make the future throw its exception if any
        progressBar.setProgress(ackedCount.incrementAndGet())
      }
    }
  }
}
