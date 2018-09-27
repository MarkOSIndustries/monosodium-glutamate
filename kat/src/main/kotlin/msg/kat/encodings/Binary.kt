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
    return ProducerRecord(topic, Encoding.randomKey(), bytes)
  }

  override fun fromConsumerRecord(consumerRecord: ConsumerRecord<ByteArray, ByteArray>, schema: String): ByteArray {
    return consumerRecord.value()
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return Encoding.lengthPrefixedBinaryValues(output)
  }
}
