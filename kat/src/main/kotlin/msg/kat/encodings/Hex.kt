package msg.kat.encodings

import com.google.common.io.BaseEncoding
import msg.encodings.StringEncoding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

class Hex : StringEncoding.OfBinary, KafkaEncoding {
  private val encoder = BaseEncoding.base16().lowerCase()

  override fun encode(data: ByteArray): String = encoder.encode(data)

  override fun decode(string: String): ByteArray = encoder.decode(string)

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, KafkaEncoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }
}
