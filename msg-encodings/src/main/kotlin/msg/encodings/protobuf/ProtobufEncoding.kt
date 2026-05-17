package msg.encodings.protobuf

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import msg.encodings.Encoding
import msg.encodings.StringEncoding
import msg.encodings.Transport
import msg.protobuf.JsonParser
import msg.protobuf.JsonPrinter
import msg.protobuf.ProtobufRoots

abstract class ProtobufEncoding<T>(
  private val underlyingEncoding: Encoding<T>,
) : Encoding<Message> {
  override fun getTransport(): Transport<Message> = ProtobufMessageTransport<T>(this)

  fun getUnderlyingEncoding(): Encoding<T> = underlyingEncoding

  abstract fun toMessage(data: T): Message

  abstract fun fromMessage(message: Message): T

  class AsBinary(
    private val descriptor: Descriptors.Descriptor,
    underlyingEncoding: Encoding<ByteArray>,
  ) : ProtobufEncoding<ByteArray>(underlyingEncoding) {
    override fun toMessage(data: ByteArray): DynamicMessage = DynamicMessage.parseFrom(descriptor, data)

    override fun fromMessage(message: Message): ByteArray = message.toByteArray()
  }

  class AsJson(
    private val descriptor: Descriptors.Descriptor,
    underlyingEncoding: StringEncoding<String>,
    protobufRoots: ProtobufRoots,
  ) : ProtobufEncoding<String>(underlyingEncoding) {
    private val parser = JsonParser(protobufRoots.typeRegistry)
    private val printer = JsonPrinter(protobufRoots.typeRegistry)

    override fun toMessage(data: String): Message = parser.parse(data, descriptor)

    override fun fromMessage(message: Message): String = printer.print(message)
  }
}
