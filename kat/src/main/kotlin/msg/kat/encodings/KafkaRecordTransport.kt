package msg.kat.encodings

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.InputStream
import java.io.PrintStream

class KafkaRecordTransport(private val topic: String, private val schema: String) {
  fun <T> reader(encoding: KafkaEncoding<T>, input: InputStream): Iterator<ProducerRecord<ByteArray, ByteArray>> = ProducerRecordIterator(topic, encoding, input)
  fun <T> keyReader(encoding: KafkaEncoding<T>, input: InputStream): Iterator<ByteArray> = KeyIterator(encoding, input)

  private class KeyIterator<T>(private val encoding: KafkaEncoding<T>, input: InputStream) : Iterator<ByteArray> {
    private val reader = encoding.getTransport().reader(input)
    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): ByteArray = encoding.toRecordKey(reader.next())
  }

  fun <T> writer(encoding: KafkaEncoding<T>, output: PrintStream): (ConsumerRecord<ByteArray, ByteArray>) -> Unit {
    val write = encoding.getTransport().writer(output)
    return { write(encoding.fromConsumerRecord(it, schema)) }
  }

  private class ProducerRecordIterator<T>(
    private val topic: String,
    private val encoding: KafkaEncoding<T>,
    input: InputStream
  ) : Iterator<ProducerRecord<ByteArray, ByteArray>> {
    private val reader = encoding.getTransport().reader(input)

    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): ProducerRecord<ByteArray, ByteArray> = encoding.toProducerRecord(topic, reader.next())
  }
}
