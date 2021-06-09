package msg.qs.encodings

import msg.schemas.MSG

class PutRequest : Encoding {
  override fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.PutRequest.parseFrom(bytes)
    return record.key.toByteArray() to record.value.toByteArray()
  }
}
