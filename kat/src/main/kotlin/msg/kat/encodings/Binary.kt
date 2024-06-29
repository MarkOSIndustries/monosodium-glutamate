package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

class Binary : KafkaEncoding {
  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return ProducerRecord(topic, KafkaEncoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }
}
