package msg.proto.encodings

import msg.encodings.Transport
import msg.proto.encodings.formats.Base64
import msg.proto.encodings.formats.Binary
import msg.proto.encodings.formats.Hex
import msg.proto.encodings.formats.Json
import msg.proto.encodings.formats.JsonBase64
import msg.proto.encodings.formats.JsonHex
import msg.proto.protobuf.ProtobufRoots

object ProtobufEncodings {
  val byName = mapOf<String, (ProtobufRoots, Transport<ByteArray>) -> ProtobufEncoding<*>>(
    "hex" to { _, _ -> Hex() },
    "base64" to { _, _ -> Base64() },
    "binary" to { _, transport -> Binary(transport) },
    "json" to { protobufRoots, _ -> Json(protobufRoots) },
    "json_base64" to { protobufRoots, _ -> JsonBase64(protobufRoots) },
    "json_hex" to { protobufRoots, _ -> JsonHex(protobufRoots) }
  )
}
