package msg.kat

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.kafka.KafkaTopicCommand
import msg.kat.encodings.Binary
import msg.kat.encodings.Encodings
import msg.kat.encodings.Ingesters
import msg.kat.encodings.TypedKafkaRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.LongSerializer
import java.io.InputStream
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.Future

class Produce : KafkaTopicCommand(help = "Produce records to Kafka\nReads records from stdin and sends them to Kafka") {
//  val transactional by option("-t", "--transactional")

  private val encoding by option("--encoding", "-e", help = "the format to reader records (and in some cases keys) as from stdin. HEX,Base64 are line delimited ASCII. Others are length-prefixed binary.").choice(Encodings.byName).default(Binary())

  override fun run() {
    val producer = newProducer(ByteArraySerializer::class, ByteArraySerializer::class)
    val futures = LinkedList<Future<RecordMetadata>>()

    val reader = encoding.reader(System.`in`)
    while(reader.hasNext()) {
      val bytes = reader.next()
      futures.add(producer.send(encoding.toProducerRecord(topic, bytes)))

      while(futures.isNotEmpty() && futures.first.isDone) {
        futures.pop().get() // make the future throw its exception if any
        System.out.print('.')
      }
    }
  }
}
