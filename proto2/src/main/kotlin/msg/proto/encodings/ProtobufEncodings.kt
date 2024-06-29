package msg.proto.encodings

import msg.proto.encodings.formats.Base64
import msg.proto.encodings.formats.Binary
import msg.proto.encodings.formats.Hex
import msg.proto.encodings.formats.Json
import msg.proto.encodings.formats.JsonBase64
import msg.proto.encodings.formats.JsonHex
import msg.proto.protobuf.ProtobufRoots

object ProtobufEncodings {
  val byName = mapOf<String, (ProtobufRoots) -> ProtobufEncoding<*>>(
    "hex" to { Hex() },
    "base64" to { Base64() },
    "binary" to { Binary() },
    "json" to { Json(it) },
    "json_base64" to { JsonBase64(it) },
    "json_hex" to { JsonHex(it) }
  )
}
