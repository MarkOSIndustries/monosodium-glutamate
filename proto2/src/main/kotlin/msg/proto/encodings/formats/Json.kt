package msg.proto.encodings.formats

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import msg.proto.encodings.ProtobufEncoding
import msg.proto.protobuf.ProtobufRoots

class Json(protobufRoots: ProtobufRoots) : ProtobufEncoding.OfStringsRepresentingJson() {
  private val parser = JsonFormat.parser().usingTypeRegistry(protobufRoots.typeRegistry).ignoringUnknownFields()
  private val printer = JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace().alwaysPrintFieldsWithNoPresence().usingTypeRegistry(protobufRoots.typeRegistry)

  override fun encode(data: String): String = data
  override fun decode(string: String): String = string

  override fun toMessage(descriptor: Descriptors.Descriptor, data: String): DynamicMessage {
    return DynamicMessage.newBuilder(descriptor).let {
      parser.merge(data, it)
      it.build()
    }
  }

  override fun fromMessage(message: Message): String {
    return printer.print(message)
  }
}
