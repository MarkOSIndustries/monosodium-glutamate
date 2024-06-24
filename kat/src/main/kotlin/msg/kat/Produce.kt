package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.EOFException
import java.util.LinkedList
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class Produce : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Produce records to Kafka

  Reads records from stdin and sends them to Kafka
  """.trimIndent()

  private val limit by option("--limit", "-l", help = "the maximum number of messages to produce").long().default(Long.MAX_VALUE)

  override fun run() {
    val producer = newProducer(ByteArraySerializer::class, ByteArraySerializer::class)

    val futures = LinkedList<Future<RecordMetadata>>()

    val producedCount = AtomicInteger(0)
    val ackedCount = AtomicInteger(0)
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.err.println("Produced $producedCount messages, $ackedCount ACKed.")
      }
    )

    try {
      val reader = delimiter().reader(System.`in`)
      while (reader.hasNext() && producedCount.getAndIncrement() < limit) {
        val bytes = reader.next()
        futures.add(producer.send(encoding.toProducerRecord(topic, bytes)))
        while (futures.isNotEmpty() && futures.first.isDone) {
          futures.pop().get() // make the future throw its exception if any
          ackedCount.incrementAndGet()
        }
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }

    while (futures.isNotEmpty()) {
      futures.pop().get() // make the future throw its exception if any
      ackedCount.incrementAndGet()
    }
  }
}
