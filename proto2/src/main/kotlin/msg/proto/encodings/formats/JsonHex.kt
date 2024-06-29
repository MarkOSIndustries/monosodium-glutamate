package msg.proto.encodings.formats

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import msg.proto.encodings.ProtobufEncoding
import msg.proto.protobuf.ProtobufRoots

class JsonHex(protobufRoots: ProtobufRoots) : ProtobufEncoding.OfStringsRepresentingJson() {
  private val hex = Hex()
  private val json = Json(protobufRoots)

  override fun encode(data: String): String = hex.encode(data.encodeToByteArray())
  override fun decode(string: String): String = hex.decode(string).decodeToString()

  override fun toMessage(descriptor: Descriptors.Descriptor, data: String): DynamicMessage = json.toMessage(descriptor, data)
  override fun fromMessage(message: Message): String = json.fromMessage(message)
}
