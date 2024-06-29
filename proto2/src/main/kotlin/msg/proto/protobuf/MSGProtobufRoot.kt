package msg.proto.protobuf

import com.google.protobuf.Descriptors
import msg.schemas.MSG

class MSGProtobufRoot : ProtobufRoot {
  override fun getMessageDescriptors(): List<Descriptors.Descriptor> {
    return MSG.getDescriptor().messageTypes
  }

  override fun findMessageDescriptor(message: String): Descriptors.Descriptor? {
    return MSG.getDescriptor().messageTypes.find { it.fullName == message }
  }

  override fun getServiceDescriptors(): List<Descriptors.ServiceDescriptor> {
    return MSG.getDescriptor().services
  }

  override fun findServiceDescriptor(service: String): Descriptors.ServiceDescriptor? {
    return MSG.getDescriptor().services.find { it.fullName == service }
  }
}
