package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.kafka.KafkaTopicCommand
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.LongSerializer
import java.io.DataInputStream
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.Future

class Produce : KafkaTopicCommand(help = "Produce records to Kafka\nReads records from stdin and sends them to Kafka") {

  override fun run() {
    val producer = newProducer(LongSerializer::class, ByteArraySerializer::class)
    val futures = LinkedList<Future<RecordMetadata>>()
    while(true) {
      val input = DataInputStream(System.`in`)
      val nextMessageSize = input.readInt()
      val byteArray = ByteArray(nextMessageSize)
      input.readFully(byteArray)

      futures.add(producer.send(ProducerRecord(topic, UUID.randomUUID().mostSignificantBits, byteArray)))

      while(futures.isNotEmpty() && futures.first.isDone) {
        futures.pop().get() // make he future throw it's exception if any
        System.out.print('.')
      }
    }
  }
}
