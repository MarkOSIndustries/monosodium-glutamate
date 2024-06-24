package msg.kat

import com.github.ajalt.clikt.core.Context
import msg.kafka.topicPartitions
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.utils.Utils
import java.io.EOFException

class Partition : KafkaTopicDataCommand() {
  override fun help(context: Context) = """
  Apply the default Java Kafka key partitioning strategy

  Reads keys from stdin and writes the topic partition they would be produced to by default to stdout
  """.trimIndent()

  override fun run() {
    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    val partitions = consumer.topicPartitions(topic)

    try {
      val reader = delimiter().reader(System.`in`)
      while (reader.hasNext()) {
        val bytes = reader.next()
        val partition = Utils.toPositive(Utils.murmur2(bytes)) % partitions.size
        println("$topic-$partition")
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }
  }
}
