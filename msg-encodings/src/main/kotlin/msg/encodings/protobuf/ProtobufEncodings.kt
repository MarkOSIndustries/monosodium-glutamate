package msg.encodings.protobuf

import com.google.protobuf.Descriptors
import msg.encodings.Transport
import msg.encodings.formats.Binary
import msg.encodings.formats.BinaryAsBase64
import msg.encodings.formats.BinaryAsHex
import msg.encodings.formats.Strings
import msg.encodings.formats.StringsAsBase64
import msg.encodings.formats.StringsAsHex
import msg.protobuf.ProtobufRoots

object ProtobufEncodings {
  val byName =
    mapOf<String, (Descriptors.Descriptor, ProtobufRoots, Transport<ByteArray>) -> ProtobufEncoding<*>>(
      "hex" to { descriptor, _, _ -> ProtobufEncoding.AsBinary(descriptor, BinaryAsHex()) },
      "base64" to { descriptor, _, _ -> ProtobufEncoding.AsBinary(descriptor, BinaryAsBase64()) },
      "binary" to { descriptor, _, transport -> ProtobufEncoding.AsBinary(descriptor, Binary(transport)) },
      "json" to { descriptor, protobufRoots, _ -> ProtobufEncoding.AsJson(descriptor, Strings(), protobufRoots) },
      "json_base64" to { descriptor, protobufRoots, _ -> ProtobufEncoding.AsJson(descriptor, StringsAsBase64(), protobufRoots) },
      "json_hex" to { descriptor, protobufRoots, _ -> ProtobufEncoding.AsJson(descriptor, StringsAsHex(), protobufRoots) },
    )
}
