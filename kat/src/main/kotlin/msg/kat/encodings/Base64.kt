package msg.kat.encodings

import msg.encodings.ASCIIEncoding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.Base64

class Base64 : ASCIIEncoding, Encoding {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

  override fun decode(string: String): ByteArray = decoder.decode(string)

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, Encoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }
}
