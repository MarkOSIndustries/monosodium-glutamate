package msg.qs.encodings

import msg.schemas.MSG

class TypedKafkaRecord : Encoding {
  override fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.TypedKafkaRecord.parseFrom(bytes)
    return record.key.toByteArray() to record.value.value.toByteArray()
  }
}
