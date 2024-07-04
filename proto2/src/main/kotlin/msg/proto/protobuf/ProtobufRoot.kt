package msg.proto.protobuf

import com.google.protobuf.Descriptors

interface ProtobufRoot {
  fun getMessageDescriptors(): List<Descriptors.Descriptor>
  fun findMessageDescriptor(message: String): Descriptors.Descriptor?

  fun getServiceDescriptors(): List<Descriptors.ServiceDescriptor>
  fun findServiceDescriptor(service: String): Descriptors.ServiceDescriptor?
}
