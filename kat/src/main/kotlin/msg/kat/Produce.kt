package msg.kat

import com.github.ajalt.clikt.parameters.options.option
import msg.kafka.Brokers
import msg.kafka.Producer
import java.io.DataInputStream

class Produce : KafkaTopicCommand(help = "Produce records to Kafka\nReads stdin as length-prefixed binary records and sends them to Kafka") {
  val encoding by option()

  override fun run() {
    val producer = newProducer()

    while(true) {
      val input = DataInputStream(System.`in`)
      val nextMessageSize = input.readInt()
      val byteArray = ByteArray(nextMessageSize)
      input.readFully(byteArray)
      producer.produce(topic, byteArray)
      System.out.print('.')
    }
  }
}
