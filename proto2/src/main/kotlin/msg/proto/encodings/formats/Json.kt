package msg.proto.encodings.formats

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import msg.proto.encodings.ProtobufEncoding
import msg.proto.protobuf.JsonParser
import msg.proto.protobuf.JsonPrinter
import msg.proto.protobuf.ProtobufRoots

class Json(
  protobufRoots: ProtobufRoots,
) : ProtobufEncoding.OfStringsRepresentingJson() {
  private val parser = JsonParser(protobufRoots.typeRegistry)
  private val printer = JsonPrinter(protobufRoots.typeRegistry)

  override fun encode(data: String): String = data

  override fun decode(string: String): String = string

  override fun toMessage(
    descriptor: Descriptors.Descriptor,
    data: String,
  ): Message = parser.parse(data, descriptor)

  override fun fromMessage(message: Message): String = printer.print(message)
}
