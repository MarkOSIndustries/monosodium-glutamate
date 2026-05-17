package msg.encodings.protobuf

import com.google.protobuf.Message
import msg.encodings.Transport
import java.io.InputStream
import java.io.PrintStream

class ProtobufMessageTransport<T>(
  private val protobufEncoding: ProtobufEncoding<T>,
) : Transport<Message> {
  override fun reader(input: InputStream): Iterator<Message> = MessageIterator(protobufEncoding, input)

  override fun writer(output: PrintStream): (Message) -> Unit {
    val write = protobufEncoding.getUnderlyingEncoding().getTransport().writer(output)
    return { write(protobufEncoding.fromMessage(it)) }
  }

  private class MessageIterator<T>(
    private val protobufEncoding: ProtobufEncoding<T>,
    input: InputStream,
  ) : Iterator<Message> {
    private val reader = protobufEncoding.getUnderlyingEncoding().getTransport().reader(input)

    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): Message = protobufEncoding.toMessage(reader.next())
  }
}
