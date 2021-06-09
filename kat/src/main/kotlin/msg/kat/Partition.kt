package msg.kat

import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import msg.kafka.topicPartitions
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.utils.Utils
import java.io.EOFException

@ExperimentalCompletionCandidates
class Partition : KafkaTopicDataCommand(
  "Apply the default Java Kafka key partitioning strategy.\n\n" +
    "Reads keys from stdin and writes the topic partition they would be produced to by default to stdout"
) {

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
