package msg.kat

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.topicPartitions
import msg.kat.encodings.KafkaRecordTransport
import msg.progressbar.NoopProgressBar
import msg.progressbar.StderrProgressBar
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.utils.Utils
import java.io.EOFException
import java.util.concurrent.atomic.AtomicLong

class Partition : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Apply the default Java Kafka key partitioning strategy

  Reads keys from stdin and writes the topic partition they would be produced to by default to stdout.
  For msg.KafkaRecord and msg.TypedKafkaRecord encodings, keys will be taken from the "key" fields.
  For all other encodings, each record given will be interpreted as a key to be partitioned.
  """.trimIndent()

  private val limit by option("--limit", "-l", help = "the maximum number of keys to partition").long().default(Long.MAX_VALUE)

  override fun run() {
    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    val partitions = consumer.topicPartitions(topic)

    val partitionedCount = AtomicLong(0L)

    val progressBar = if (progress) StderrProgressBar(this.commandName) else NoopProgressBar()
    if (limit != Long.MAX_VALUE) {
      progressBar.setTotal(limit)
    }

    progressBar.use {
      try {
        val transport = KafkaRecordTransport(topic, topic)
        val reader = transport.keyReader(encoding(prefix), System.`in`)
        while (reader.hasNext() && partitionedCount.getAndIncrement() < limit) {
          val bytes = reader.next()
          val partition = Utils.toPositive(Utils.murmur2(bytes)) % partitions.size
          println("$topic-$partition")
          progressBar.setProgress(partitionedCount.get())
        }
      } catch (t: EOFException) {
        // Ignore, we just terminated between hasNext and next()
      }
    }
  }
}
