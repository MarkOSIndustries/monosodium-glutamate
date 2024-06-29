package msg.proto.protobuf

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import com.google.protobuf.TypeRegistry

class ProtobufMessage(typeRegistry: TypeRegistry, messageDescriptor: Descriptors.Descriptor, message: Message) {
  val messageDescriptor: Descriptors.Descriptor
  val message: Message

  init {
    if (messageDescriptor.fullName != Any.getDescriptor().fullName) {
      this.messageDescriptor = messageDescriptor
      this.message = message
    } else {
      val anyTypeUrl = message.getField(messageDescriptor.findFieldByNumber(googleProtobufAny_TypeUrlField)) as String
      val anyValue = message.getField(messageDescriptor.findFieldByNumber(googleProtobufAny_ValueField)) as ByteString
      this.messageDescriptor = typeRegistry.getDescriptorForTypeUrl(anyTypeUrl)
      this.message = DynamicMessage.parseFrom(this.messageDescriptor, anyValue)
    }
  }

  companion object {
    private val googleProtobufAny_TypeUrlField = Any.getDescriptor().findFieldByName("type_url").number
    private val googleProtobufAny_ValueField = Any.getDescriptor().findFieldByName("value").number
  }
}
