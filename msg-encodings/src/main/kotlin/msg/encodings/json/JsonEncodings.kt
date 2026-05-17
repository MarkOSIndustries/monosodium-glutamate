package msg.encodings.json

import msg.encodings.Transport
import msg.encodings.formats.Binary
import msg.encodings.formats.BinaryAsBase64
import msg.encodings.formats.BinaryAsHex
import msg.encodings.formats.Strings
import msg.encodings.formats.StringsAsBase64
import msg.encodings.formats.StringsAsHex

object JsonEncodings {
  val byName =
    mapOf<String, (Transport<ByteArray>) -> JsonEncoding<*>>(
      "json" to { _ -> JsonEncoding.AsJson(Strings()) },
      "json_hex" to { _ -> JsonEncoding.AsJson(StringsAsHex()) },
      "json_base64" to { _ -> JsonEncoding.AsJson(StringsAsBase64()) },
      "jsonb" to { transport -> JsonEncoding.AsJsonB(Binary(transport)) },
      "jsonb_hex" to { _ -> JsonEncoding.AsJsonB(BinaryAsHex()) },
      "jsonb_base64" to { _ -> JsonEncoding.AsJsonB(BinaryAsBase64()) },
    )
}
