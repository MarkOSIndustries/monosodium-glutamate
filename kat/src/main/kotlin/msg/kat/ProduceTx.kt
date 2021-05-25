package msg.kat

import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.util.UUID

@ExperimentalCompletionCandidates
class ProduceTx : KafkaTopicDataCommand(
  help = "Produce records to Kafka using transactions\n\n" +
    "Reads records from stdin and sends them to Kafka"
) {
  val commit by option("-c", "--commit", help = "How many records should be sent per transaction").int().default(5)

  override fun run() {
    val producer = newProducer(
      ByteArraySerializer::class, ByteArraySerializer::class,
      ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
      ProducerConfig.TRANSACTIONAL_ID_CONFIG to UUID.randomUUID().toString()
    )
    producer.initTransactions()

    val reader = encoding.reader(System.`in`)

    var recordsInTransaction = 0
    producer.beginTransaction()
    while (reader.hasNext()) {
      if (recordsInTransaction++ == commit) {
        producer.commitTransaction()
        System.out.print('.')
        recordsInTransaction = 0
        producer.beginTransaction()
      }
      val bytes = reader.next()
      producer.send(encoding.toProducerRecord(topic, bytes))
    }
  }
}
