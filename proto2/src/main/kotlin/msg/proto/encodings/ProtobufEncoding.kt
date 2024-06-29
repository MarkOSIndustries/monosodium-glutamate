package msg.proto.encodings

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import msg.encodings.StringEncoding
import msg.encodings.Transport
import msg.encodings.delimiters.Delimiters
import msg.encodings.delimiters.StringNewlineDelimiter

sealed interface ProtobufEncoding<T> {
  fun toMessage(descriptor: Descriptors.Descriptor, data: T): Message
  fun fromMessage(message: Message): T
  fun getDelimiter(binaryPrefix: String): Transport<T>

  abstract class OfBinary : ProtobufEncoding<ByteArray> {
    override fun toMessage(descriptor: Descriptors.Descriptor, data: ByteArray): DynamicMessage {
      return DynamicMessage.parseFrom(descriptor, data)
    }
    override fun fromMessage(message: Message): ByteArray {
      return message.toByteArray()
    }
    final override fun getDelimiter(binaryPrefix: String): Transport<ByteArray> {
      return Delimiters.lengthPrefixedBinary[binaryPrefix]!!
    }
  }

  abstract class OfStringsRepresentingBinary : ProtobufEncoding<ByteArray>, StringEncoding.OfBinary {
    final override fun toMessage(descriptor: Descriptors.Descriptor, data: ByteArray): DynamicMessage {
      return DynamicMessage.parseFrom(descriptor, data)
    }
    final override fun fromMessage(message: Message): ByteArray {
      return message.toByteArray()
    }
    final override fun getDelimiter(binaryPrefix: String): Transport<ByteArray> {
      return StringNewlineDelimiter(this)
    }
  }

  abstract class OfStringsRepresentingJson : ProtobufEncoding<String>, StringEncoding.OfStrings {
    final override fun getDelimiter(binaryPrefix: String): Transport<String> {
      return StringNewlineDelimiter(this)
    }
  }
}
