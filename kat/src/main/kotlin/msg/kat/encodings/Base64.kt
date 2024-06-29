package msg.kat.encodings

import msg.encodings.StringEncoding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.Base64

class Base64 : StringEncoding.OfBinary, KafkaEncoding {
  private val encoder = Base64.getEncoder()
  private val decoder = Base64.getDecoder()

  override fun encode(data: ByteArray): String = encoder.encodeToString(data)

  override fun decode(string: String): ByteArray = decoder.decode(string)

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, KafkaEncoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }
}
