package msg.kat.encodings

import com.google.common.io.BaseEncoding
import msg.encodings.ASCIIEncoding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

class Hex : ASCIIEncoding, Encoding {
  private val encoder = BaseEncoding.base16().lowerCase()

  override fun encode(bytes: ByteArray): String = encoder.encode(bytes)

  override fun decode(string: String): ByteArray = encoder.decode(string)

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, Encoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }
}
