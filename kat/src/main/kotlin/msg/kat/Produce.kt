package msg.kat

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.EOFException
import java.util.LinkedList
import java.util.concurrent.Future

class Produce : KafkaTopicDataCommand(help = "Produce records to Kafka\nReads records from stdin and sends them to Kafka") {
  override fun run() {
    val producer = newProducer(ByteArraySerializer::class, ByteArraySerializer::class)

    val futures = LinkedList<Future<RecordMetadata>>()

    try {
      val reader = encoding.reader(System.`in`)
      while (reader.hasNext()) {
        val bytes = reader.next()
        futures.add(producer.send(encoding.toProducerRecord(topic, bytes)))
        while (futures.isNotEmpty() && futures.first.isDone) {
          futures.pop().get() // make the future throw its exception if any
          System.out.print('.')
        }
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }
  }
}
