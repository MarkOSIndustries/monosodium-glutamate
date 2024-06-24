package msg.kat.encodings

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader

class TypedKafkaRecord : Encoding {
  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val record = MSG.TypedKafkaRecord.parseFrom(bytes)
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
      .setTimestamp(consumerRecord.timestamp()) // Timestamp.newBuilder().setSeconds(consumerRecord.timestamp()/1000).setNanos(1000000000 * (consumerRecord.timestamp()%1000).toInt()).build()

    if (consumerRecord.key() != null) {
      builder.key = ByteString.copyFrom(consumerRecord.key())
    }
    if (consumerRecord.value() != null) {
      builder.value = Any.newBuilder().setValue(ByteString.copyFrom(consumerRecord.value())).setTypeUrl(schema).build()
    }

    builder.addAllHeaders(consumerRecord.headers().map { header -> MSG.KafkaHeader.newBuilder().setValue(ByteString.copyFrom(header.value())).setKey(header.key()).build() })

    return builder.build().toByteArray()
  }
}
