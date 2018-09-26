package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.PrintStream

class Binary : Encoding {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return Ingesters.lengthPrefixedBinaryValues(input)
  }

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return Translators.addRandomKey(topic, bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return Translators.toValueBytes(consumerRecord)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return Emitters.lengthPrefixedBinaryValues(output)
  }
}
