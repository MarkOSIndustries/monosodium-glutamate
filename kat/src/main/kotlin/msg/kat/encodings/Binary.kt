package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer

class Binary : Encoding {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }

  override fun toProducerRecord(topic: String, bytes: ByteArray): ProducerRecord<ByteArray, ByteArray> {
    return Translators.addRandomKey(topic, bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return Translators.toValueBytes(consumerRecord)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return lengthPrefixedBinaryValues(output)
  }

  companion object {
    fun lengthPrefixedBinaryValues(out: OutputStream) : (ByteArray)->Unit {
      val sizeBufferArray = ByteArray(4)
      val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

      return {
        sizeBuffer.putInt(0, it.size)
        out.write(sizeBufferArray)
        out.write(it)
      }
    }
  }
}
