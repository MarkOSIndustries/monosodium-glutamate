package msg.kat.encodings.formats

import msg.kat.encodings.KafkaEncoding
import msg.kat.kafka.RecordKeys
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

class Strings : KafkaEncoding.OfStrings() {
  override fun encode(data: String) = data
  override fun decode(string: String) = string
  override fun toProducerRecord(topic: String, data: String) = ProducerRecord(topic, RecordKeys.randomKey(), data.toByteArray(Charsets.UTF_8))
  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String) = String(consumerRecord.value(), Charsets.UTF_8)
  override fun toRecordKey(data: String): ByteArray = data.toByteArray(Charsets.UTF_8)
}
