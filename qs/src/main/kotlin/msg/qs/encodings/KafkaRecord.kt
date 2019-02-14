package msg.qs.encodings

import msg.qs.encodings.Encoding.Companion.lengthPrefixedBinaryValues
import msg.schemas.MSG
import java.io.InputStream
import java.io.PrintStream

class KafkaRecord : Encoding {
  override fun decodeKeyValuePair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.KafkaRecord.parseFrom(bytes)
    return record.key.toByteArray() to record.value.toByteArray()
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return lengthPrefixedBinaryValues(output)
  }

  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }
}
