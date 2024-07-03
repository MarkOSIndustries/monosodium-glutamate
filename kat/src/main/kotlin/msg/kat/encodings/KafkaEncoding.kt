package msg.kat.encodings

import msg.encodings.StringEncoding
import msg.encodings.Transport
import msg.encodings.delimiters.StringNewlineDelimiter
import msg.kat.kafka.RecordKeys
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

sealed interface KafkaEncoding<T> {
  fun toProducerRecord(topic: String, data: T): ProducerRecord<ByteArray, ByteArray>
  fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): T
  fun toRecordKey(data: T): ByteArray
  fun getTransport(): Transport<T>

  abstract class OfBinary(private val transport: Transport<ByteArray>) : KafkaEncoding<ByteArray> {
    override fun toProducerRecord(topic: String, data: ByteArray) = ProducerRecord(topic, RecordKeys.randomKey(), data)
    override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String) = consumerRecord.value()!!
    override fun toRecordKey(data: ByteArray) = data
    final override fun getTransport() = transport
  }

  abstract class OfStringsRepresentingBinary : KafkaEncoding<ByteArray>, StringEncoding.OfBinary {
    final override fun toProducerRecord(topic: String, data: ByteArray) = ProducerRecord(topic, RecordKeys.randomKey(), data)
    final override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String) = consumerRecord.value()!!
    final override fun toRecordKey(data: ByteArray) = data
    final override fun getTransport() = StringNewlineDelimiter(this)
  }

  abstract class OfStrings : KafkaEncoding<String>, StringEncoding.OfStrings {
    final override fun getTransport() = StringNewlineDelimiter(this)
  }
}
