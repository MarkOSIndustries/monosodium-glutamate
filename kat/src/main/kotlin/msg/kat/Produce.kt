package msg.kat

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.util.LinkedList
import java.util.concurrent.Future

class Produce : KafkaTopicDataCommand(help = "Produce records to Kafka\nReads records from stdin and sends them to Kafka") {
//  val transactional by option("-t", "--transactional")


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
