package msg.proto.protobuf

import com.google.protobuf.Descriptors
import com.google.protobuf.TypeRegistry
import java.nio.file.Path

class ProtobufRoots(
  protobufPaths: Collection<Path>,
) {
  val protobufRoots =
    protobufPaths.mapNotNull {
      try {
        FileSystemProtobufRoot(it)
      } catch (ex: Exception) {
        System.err.println("Couldn't load protobuf root $it - ${ex.message}")
        null
      }
    } + MSGProtobufRoot()

  val typeRegistry: TypeRegistry by lazy {
    val typeRegistry = TypeRegistry.newBuilder()
    for (protobufRoot in protobufRoots) {
      for (messageDescriptor in protobufRoot.getMessageDescriptors()) {
        typeRegistry.add(messageDescriptor)
      }
    }
    typeRegistry.build()
  }

  fun findMessageDescriptor(messageName: String): Descriptors.Descriptor? =
    protobufRoots.fold(null as Descriptors.Descriptor?) { found, protobufRoot ->
      found ?: protobufRoot.findMessageDescriptor(messageName)
    }

  fun getAllMessageDescriptors(): List<Descriptors.Descriptor> = protobufRoots.flatMap { it.getMessageDescriptors() }

  fun findServiceDescriptor(serviceName: String): Descriptors.ServiceDescriptor? =
    protobufRoots.fold(null as Descriptors.ServiceDescriptor?) { found, protobufRoot ->
      found ?: protobufRoot.findServiceDescriptor(serviceName)
    }

  fun getAllServiceDescriptors(): List<Descriptors.ServiceDescriptor> = protobufRoots.flatMap { it.getServiceDescriptors() }
}
