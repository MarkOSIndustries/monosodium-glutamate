package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.Random

interface Encoding {
  fun reader(input: InputStream): Iterator<ByteArray>
  fun toProducerRecord(topic: String, bytes:ByteArray): ProducerRecord<ByteArray,ByteArray>

  fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray
  fun writer(output: PrintStream): (ByteArray)->Unit

  companion object {
    private val random = Random()

    fun randomKey(): ByteArray {
      val key = ByteArray(16)
      random.nextBytes(key)
      return key
    }

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
