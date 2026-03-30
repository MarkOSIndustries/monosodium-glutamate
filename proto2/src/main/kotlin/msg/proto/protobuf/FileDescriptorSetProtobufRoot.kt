package msg.proto.protobuf

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors

class FileDescriptorSetProtobufRoot(
  fileDescriptorSet: DescriptorProtos.FileDescriptorSet,
) : ProtobufRoot {
  private val fileDescriptorCache =
    fileDescriptorSet.fileList.associate {
      it.name to
        LazyFileDescriptor(it, ::getFileDescriptor)
    }

  class LazyFileDescriptor(
    fileDescriptorProto: DescriptorProtos.FileDescriptorProto,
    private val getFileDescriptor: (String) -> Descriptors.FileDescriptor,
  ) {
    val fileDescriptor: Descriptors.FileDescriptor by lazy {
      Descriptors.FileDescriptor.buildFrom(
        fileDescriptorProto,
        fileDescriptorProto.dependencyList.map { getFileDescriptor(it) }.toTypedArray(),
      )
    }
  }

  private fun getFileDescriptor(path: String): Descriptors.FileDescriptor = fileDescriptorCache[path]!!.fileDescriptor

  override fun getMessageDescriptors(): List<Descriptors.Descriptor> = fileDescriptorCache.values.flatMap { it.fileDescriptor.messageTypes }

  override fun findMessageDescriptor(message: String): Descriptors.Descriptor? = getMessageDescriptors().find { it.fullName == message }

  override fun getServiceDescriptors(): List<Descriptors.ServiceDescriptor> =
    fileDescriptorCache.values.flatMap {
      it.fileDescriptor.services
    }

  override fun findServiceDescriptor(service: String): Descriptors.ServiceDescriptor? =
    getServiceDescriptors().find {
      it.fullName == service
    }
}
