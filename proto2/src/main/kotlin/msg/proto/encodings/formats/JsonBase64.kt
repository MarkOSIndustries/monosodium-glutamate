package msg.proto.encodings.formats

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import msg.proto.encodings.ProtobufEncoding
import msg.proto.protobuf.ProtobufRoots

class JsonBase64(protobufRoots: ProtobufRoots) : ProtobufEncoding.OfStringsRepresentingJson() {
  private val base64 = Base64()
  private val json = Json(protobufRoots)

  override fun encode(data: String): String = base64.encode(data.encodeToByteArray())
  override fun decode(string: String): String = base64.decode(string).decodeToString()

  override fun toMessage(descriptor: Descriptors.Descriptor, data: String): Message = json.toMessage(descriptor, data)
  override fun fromMessage(message: Message): String = json.fromMessage(message)
}
