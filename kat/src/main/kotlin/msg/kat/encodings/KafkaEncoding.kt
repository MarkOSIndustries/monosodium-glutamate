package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.Random

interface KafkaEncoding {
  fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray>
  fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray

  companion object {
    private val random = Random()

    fun randomKey(): ByteArray {
      val key = ByteArray(16)
      random.nextBytes(key)
      return key
    }
  }
}
