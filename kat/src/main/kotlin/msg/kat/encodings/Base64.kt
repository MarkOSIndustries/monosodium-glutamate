package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.PrintStream
import java.util.Base64

class Base64 : Encoding {
  private val encoder = Base64.getEncoder()

  override fun reader(input: InputStream): Iterator<ByteArray> {
    return input.bufferedReader().lineSequence().map { Base64.getDecoder().decode(it) }.iterator()
  }

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return Translators.addRandomKey(topic, bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return Translators.toValueBytes(consumerRecord)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return { output.println(encoder.encodeToString(it)) }
  }
}
