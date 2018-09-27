package msg.kat.encodings

import com.google.common.io.BaseEncoding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.PrintStream

class Hex : Encoding {
  private val encoder = BaseEncoding.base16().lowerCase()

  override fun reader(input: InputStream): Iterator<ByteArray> {
    return input.bufferedReader().lineSequence().map { encoder.decode(it) }.iterator()
  }

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, Encoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return { output.println(encoder.encode(it)) }
  }
}
