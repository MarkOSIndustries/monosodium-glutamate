package msg.proto.protobuf

import com.google.protobuf.Descriptors
import com.google.protobuf.TypeRegistry
import java.nio.file.Path

class ProtobufRoots(protobufPaths: Collection<Path>) {
  val protobufRoots = protobufPaths.map {
    try {
      FileSystemProtobufRoot(it)
    } catch (ex: Exception) {
      System.err.println("Couldn't load protobuf root $it - ${ex.message}")
      null
    }
  }.filterNotNull() + MSGProtobufRoot()

  val typeRegistry: TypeRegistry by lazy {
    val typeRegistry = TypeRegistry.newBuilder()
    for (protobufRoot in protobufRoots) {
      for (messageDescriptor in protobufRoot.getMessageDescriptors()) {
        typeRegistry.add(messageDescriptor)
      }
    }
    typeRegistry.build()
  }

  fun findMessageDescriptor(messageName: String): Descriptors.Descriptor? {
    return protobufRoots.fold(null as Descriptors.Descriptor?) { found, protobufRoot ->
      found ?: protobufRoot.findMessageDescriptor(messageName)
    }
  }

  fun getAllMessageDescriptors(): List<Descriptors.Descriptor> {
    return protobufRoots.flatMap { it.getMessageDescriptors() }
  }

  fun findServiceDescriptor(serviceName: String): Descriptors.ServiceDescriptor? {
    return protobufRoots.fold(null as Descriptors.ServiceDescriptor?) { found, protobufRoot ->
      found ?: protobufRoot.findServiceDescriptor(serviceName)
    }
  }

  fun getAllServiceDescriptors(): List<Descriptors.ServiceDescriptor> {
    return protobufRoots.flatMap { it.getServiceDescriptors() }
  }
}
