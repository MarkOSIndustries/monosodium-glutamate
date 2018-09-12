package msg.kat

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.Base64
import msg.schemas.MSG

object Emitters {
  fun lineDelimitedHexValues(out: PrintStream) : (ConsumerRecord<ByteArray, ByteArray>)->Unit {
    return { out.println(it.value().joinToString { byte -> String.format("%02X", byte) }) }
  }

  fun lineDelimitedBase64Values(out: PrintStream) : (ConsumerRecord<ByteArray, ByteArray>)->Unit {
    val encoder = Base64.getEncoder()
    return { out.println(encoder.encodeToString(it.value())) }
  }

  fun lengthPrefixedBinaryValues(out: OutputStream, serialise: (ConsumerRecord<ByteArray,ByteArray>)->ByteArray = { it.value() }) : (ConsumerRecord<ByteArray, ByteArray>)->Unit {
    val sizeBufferArray = ByteArray(4)
    val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

    return { record ->
      val recordAsBytes = serialise(record)
      sizeBuffer.putInt(0, recordAsBytes.size)
      out.write(sizeBufferArray)
      out.write(recordAsBytes)
    }
  }

  fun lengthPrefixedKafkaRecords(out: OutputStream) : (ConsumerRecord<ByteArray, ByteArray>)->Unit {
    return lengthPrefixedBinaryValues(out) { toKafkaRecord(it).toByteArray() }
  }

  fun lengthPrefixedTypedKafkaRecords(out: OutputStream) : (ConsumerRecord<ByteArray, ByteArray>)->Unit {
    return lengthPrefixedBinaryValues(out) { toTypedKafkaRecord(it).toByteArray() }
  }

  private fun toKafkaRecord(record: ConsumerRecord<ByteArray, ByteArray>):MSG.KafkaRecord {
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

  private fun toTypedKafkaRecord(record: ConsumerRecord<ByteArray, ByteArray>):MSG.TypedKafkaRecord {
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
}
