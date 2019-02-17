package msg.kat.encodings

import com.google.protobuf.ByteString
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import java.io.InputStream
import java.io.PrintStream

class KafkaRecord : Encoding {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val record = MSG.KafkaRecord.parseFrom(bytes)
    return ProducerRecord(
      topic,
      null,
      record.key.toByteArray(),
      record.value.toByteArray(),
      record.headersList.map { header -> RecordHeader(header.key, header.value.asReadOnlyByteBuffer()) })
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    val builder = MSG.KafkaRecord.newBuilder()
      .setTopic(consumerRecord.topic())
      .setPartition(consumerRecord.partition())
      .setOffset(consumerRecord.offset())
      .setTimestamp(consumerRecord.timestamp()) // Timestamp.newBuilder().setSeconds(consumerRecord.timestamp()/1000).setNanos(1000000000 * (consumerRecord.timestamp()%1000).toInt()).build()

    if (consumerRecord.key() != null) {
      builder.key = ByteString.copyFrom(consumerRecord.key())
    }
    if (consumerRecord.value() != null) {
      builder.value = ByteString.copyFrom(consumerRecord.value())
    }
    builder.addAllHeaders(consumerRecord.headers().map { header -> MSG.KafkaHeader.newBuilder().setValue(ByteString.copyFrom(header.value())).setKey(header.key()).build() })

    return builder.build().toByteArray()
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return Encoding.lengthPrefixedBinaryValues(output)
  }
}
