package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import msg.kafka.Brokers
import msg.kafka.EarliestOffsetSpec
import msg.kafka.EphemeralConsumer
import msg.kafka.LatestOffsetSpec
import msg.kafka.TimestampOffsetSpec
import msg.kafka.TopicIterator
import java.nio.ByteBuffer
import java.time.Instant
import msg.schemas.MSG
import org.apache.kafka.clients.consumer.ConsumerRecord

class Consume : KafkaTopicCommand(help = "Consume records from Kafka\nReads records from Kafka and emits length-prefixed binary records on stdout") {
  val seek by argument("seek", "one of 'earliest', 'latest', or 'timestamp'").default("earliest")
  val timestamp by argument("timestamp", "the epoch milliseconds timestamp to seek to").default(Instant.now().toEpochMilli().toString())
  val encoding by option()

  override fun run() {
    val ephemeralConsumer = EphemeralConsumer(*Brokers.from(brokers))

    val sizeBufferArray = ByteArray(4)
    val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

    val offsetSpec = when(seek.trim().toLowerCase()) {
      "earliest" -> EarliestOffsetSpec()
      "latest" -> LatestOffsetSpec()
      "timestamp" -> TimestampOffsetSpec(timestamp.toLong())
      else -> throw RuntimeException("Unexpected seek option - $seek")
    }

    TopicIterator(ephemeralConsumer, topic, offsetSpec).forEach { record ->
      // TODO: move swithcing to a lambda return func
      when(encoding) {
        "hex" -> {
          record.value().map { String.format("%02X", it) }.forEach { System.out.print(it) }
          println()
        }
        "msg.KafkaRecord" -> {
          val kafkaRecord = toProto(record).toByteArray()
          sizeBuffer.putInt(0, kafkaRecord.size)
          System.out.write(sizeBufferArray)
          System.out.write(kafkaRecord)
        }
        "msg.TypedKafkaRecord" -> {
          val kafkaRecord = toTypedProto(record).toByteArray()
          sizeBuffer.putInt(0, kafkaRecord.size)
          System.out.write(sizeBufferArray)
          System.out.write(kafkaRecord)
        }
        else -> {
          sizeBuffer.putInt(0, record.value().size)
          System.out.write(sizeBufferArray)
          System.out.write(record.value())
        }
      }
    }
  }
}

private fun toProto(record: ConsumerRecord<ByteArray, ByteArray>):MSG.KafkaRecord {
  val builder = MSG.KafkaRecord.newBuilder()
    .setTopic(record.topic())
    .setPartition(record.partition())
    .setOffset(record.offset())
    .setTimestamp(record.timestamp())//Timestamp.newBuilder().setSeconds(record.timestamp()/1000).setNanos(1000000000 * (record.timestamp()%1000).toInt()).build()

  if(record.key() != null) {
    builder.setKey(ByteString.copyFrom(record.key()))
  }
  if(record.value() != null) {
    // TODO: accept schema as input
    builder.setValue(ByteString.copyFrom(record.value()))
  }
  return builder.build()
}

private fun toTypedProto(record: ConsumerRecord<ByteArray, ByteArray>):MSG.TypedKafkaRecord {
  val builder = MSG.TypedKafkaRecord.newBuilder()
    .setTopic(record.topic())
    .setPartition(record.partition())
    .setOffset(record.offset())
    .setTimestamp(record.timestamp())//Timestamp.newBuilder().setSeconds(record.timestamp()/1000).setNanos(1000000000 * (record.timestamp()%1000).toInt()).build()

  if(record.key() != null) {
    builder.setKey(ByteString.copyFrom(record.key()))
  }
  if(record.value() != null) {
    // TODO: accept schema as input
    builder.setValue( Any.newBuilder().setValue(ByteString.copyFrom(record.value())).setTypeUrl(record.topic()).build() )
  }
  return builder.build()
}
