package msg.qs.encodings

import msg.schemas.MSG

class KafkaRecord : Encoding {
  override fun getKVPair(bytes: ByteArray): Pair<ByteArray, ByteArray> {
    val record = MSG.KafkaRecord.parseFrom(bytes)
    return record.key.toByteArray() to record.value.toByteArray()
  }
}
