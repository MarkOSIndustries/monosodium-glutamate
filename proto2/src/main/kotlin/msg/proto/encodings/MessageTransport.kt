package msg.proto.encodings

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import java.io.InputStream
import java.io.PrintStream

class MessageTransport(private val messageDescriptor: Descriptors.Descriptor) {
  fun <T> reader(encoding: ProtobufEncoding<T>, input: InputStream): Iterator<Message> = MessageIterator(messageDescriptor, encoding, input)

  fun <T> writer(encoding: ProtobufEncoding<T>, output: PrintStream): (Message) -> Unit {
    val write = encoding.getTransport().writer(output)
    return { write(encoding.fromMessage(it)) }
  }

  private class MessageIterator<T>(
    private val messageDescriptor: Descriptors.Descriptor,
    private val encoding: ProtobufEncoding<T>,
    input: InputStream
  ) : Iterator<Message> {
    private val reader = encoding.getTransport().reader(input)

    override fun hasNext(): Boolean = reader.hasNext()

    override fun next(): Message = encoding.toMessage(messageDescriptor, reader.next())
  }
}
