package msg.kat.encodings.formats

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import msg.encodings.Transport
import msg.kat.encodings.KafkaEncoding
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader

class TypedKafkaRecord(transport: Transport<ByteArray>) : KafkaEncoding.OfBinary(transport) {
  override fun toProducerRecord(topic: String, data: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val record = MSG.TypedKafkaRecord.parseFrom(data)
    return ProducerRecord(
      topic,
      null,
      record.key.toByteArray(),
      record.value.value.toByteArray(),
      record.headersList.map { header -> RecordHeader(header.keyBytes.asReadOnlyByteBuffer(), header.value.asReadOnlyByteBuffer()) }
    )
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    val builder = MSG.TypedKafkaRecord.newBuilder()
      .setTopic(consumerRecord.topic())
      .setPartition(consumerRecord.partition())
      .setOffset(consumerRecord.offset())
      .setTimestamp(consumerRecord.timestamp())

    if (consumerRecord.key() != null) {
      builder.key = ByteString.copyFrom(consumerRecord.key())
    }
    if (consumerRecord.value() != null) {
      builder.value = Any.newBuilder().setValue(ByteString.copyFrom(consumerRecord.value())).setTypeUrl("type/$schema").build()
    }

    builder.addAllHeaders(consumerRecord.headers().map { header -> MSG.KafkaHeader.newBuilder().setValue(ByteString.copyFrom(header.value())).setKey(header.key()).build() })

    return builder.build().toByteArray()
  }

  override fun toRecordKey(data: ByteArray): ByteArray {
    val record = MSG.TypedKafkaRecord.parseFrom(data)
    return record.key.toByteArray()
  }
}
