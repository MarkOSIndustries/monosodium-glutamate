package msg.kat.encodings

import msg.encodings.Transport
import msg.kat.encodings.formats.Base64
import msg.kat.encodings.formats.Binary
import msg.kat.encodings.formats.Hex
import msg.kat.encodings.formats.KafkaRecord
import msg.kat.encodings.formats.Strings
import msg.kat.encodings.formats.TypedKafkaRecord

object KafkaEncodings {
  val byName = mapOf<String, (Transport<ByteArray>) -> KafkaEncoding<*>>(
    "hex" to { Hex() },
    "base64" to { Base64() },
    "binary" to { Binary(it) },
    "msg.KafkaRecord" to { KafkaRecord(it) },
    "msg.TypedKafkaRecord" to { TypedKafkaRecord(it) },
  )
}
