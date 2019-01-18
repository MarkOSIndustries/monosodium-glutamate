package msg.qs.encodings

import msg.schemas.MSG
import java.io.InputStream

class PutRequest : Encoding {
  override fun reader(input: InputStream): Iterator<ByteArray> {
    return LengthPrefixedByteArrayIterator(input)
  }

  override fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.PutRequest.parseFrom(bytes)
    return record.key.toByteArray() to record.value.toByteArray()
  }
}
