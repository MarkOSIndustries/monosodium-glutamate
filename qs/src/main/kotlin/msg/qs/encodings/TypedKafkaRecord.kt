package msg.qs.encodings

import msg.schemas.MSG
import java.io.InputStream
import java.io.PrintStream

class TypedKafkaRecord : Encoding {
  override fun decodeKeyValuePair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.TypedKafkaRecord.parseFrom(bytes)
    return record.key.toByteArray() to record.value.value.toByteArray()
  }

  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }

  override fun writer(output: PrintStream): (ByteArray) -> Unit {
    return Encoding.lengthPrefixedBinaryValues(output)
  }
}
