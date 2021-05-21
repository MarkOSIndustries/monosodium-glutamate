package msg.kat

import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import msg.kafka.KafkaTopicCommand
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration

@ExperimentalCompletionCandidates
class Timestamp : KafkaTopicCommand(
  "Query timestamp by offset\n\n" +
    "Retrieves the timestamp a given offset on a given topic partition and prints to stdout"
) {
  val partition by argument("partition", "the partition to query").int()
  val offset by argument("offset", "the offset to get the timestamp for").long()

  override fun run() {
    val consumer = newConsumer(
      ByteArrayDeserializer::class, ByteArrayDeserializer::class,
      ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1
    )
    val topicPartition = TopicPartition(topic, partition)
    consumer.assign(listOf(topicPartition))
    consumer.seek(topicPartition, offset)
    val records = consumer.poll(Duration.ofSeconds(timeoutSeconds.toLong()))
    println("${records.first().timestamp()} @ offset ${records.first().offset()}")
  }
}
