package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.PrintStream

interface Encoding {
  fun reader(input: InputStream): Iterator<ByteArray>
  fun toProducerRecord(topic: String, bytes:ByteArray): ProducerRecord<ByteArray,ByteArray>

  fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray
  fun writer(output: PrintStream): (ByteArray)->Unit
}
