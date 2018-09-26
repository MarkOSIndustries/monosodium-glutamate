package msg.kat.encodings

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.Random

object Translators {
  private val random = Random()

  fun addRandomKey(topic: String, value: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val key = ByteArray(16)
    random.nextBytes(key)
    return ProducerRecord(topic, key, value)
  }

  fun fromKafkaRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val record = MSG.KafkaRecord.parseFrom(bytes)
    return ProducerRecord(topic, record.key.toByteArray(), record.value.toByteArray())
  }

  fun fromTypedKafkaRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    val record = MSG.TypedKafkaRecord.parseFrom(bytes)
    return ProducerRecord(topic, record.key.toByteArray(), record.value.value.toByteArray())
  }

  fun toValueBytes(record: ConsumerRecord<ByteArray, ByteArray>): ByteArray = record.value()

  fun toKafkaRecord(record: ConsumerRecord<ByteArray, ByteArray>): ByteArray {
    val builder = MSG.KafkaRecord.newBuilder()
      .setTopic(record.topic())
      .setPartition(record.partition())
      .setOffset(record.offset())
      .setTimestamp(record.timestamp())//Timestamp.newBuilder().setSeconds(record.timestamp()/1000).setNanos(1000000000 * (record.timestamp()%1000).toInt()).build()

    if(record.key() != null) {
      builder.key = ByteString.copyFrom(record.key())
    }
    if(record.value() != null) {
      // TODO: accept schema as input
      builder.value = ByteString.copyFrom(record.value())
    }
    return builder.build().toByteArray()
  }

  fun toTypedKafkaRecord(record: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    val builder = MSG.TypedKafkaRecord.newBuilder()
      .setTopic(record.topic())
      .setPartition(record.partition())
      .setOffset(record.offset())
      .setTimestamp(record.timestamp())//Timestamp.newBuilder().setSeconds(record.timestamp()/1000).setNanos(1000000000 * (record.timestamp()%1000).toInt()).build()

    if(record.key() != null) {
      builder.key = ByteString.copyFrom(record.key())
    }
    if(record.value() != null) {
      builder.value = Any.newBuilder().setValue(ByteString.copyFrom(record.value())).setTypeUrl(schema).build()
    }
    return builder.build().toByteArray()
  }

}
