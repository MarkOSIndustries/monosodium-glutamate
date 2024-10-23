package msg.proto.protobuf

import com.google.protobuf.Descriptors
import msg.schemas.MSG

class MSGProtobufRoot : ProtobufRoot {
  override fun getMessageDescriptors(): List<Descriptors.Descriptor> = MSG.getDescriptor().messageTypes

  override fun findMessageDescriptor(message: String): Descriptors.Descriptor? =
    MSG.getDescriptor().messageTypes.find {
      it.fullName == message
    }

  override fun getServiceDescriptors(): List<Descriptors.ServiceDescriptor> = MSG.getDescriptor().services

  override fun findServiceDescriptor(service: String): Descriptors.ServiceDescriptor? =
    MSG.getDescriptor().services.find {
      it.fullName == service
    }
}
