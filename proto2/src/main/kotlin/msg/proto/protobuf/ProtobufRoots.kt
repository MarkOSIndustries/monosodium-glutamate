package msg.proto.protobuf

import com.google.protobuf.Descriptors
import com.google.protobuf.TypeRegistry
import msg.proto.protobuf.filedescriptors.DiskCachingFileDescriptorSetProvider
import msg.proto.protobuf.filedescriptors.FileDescriptorSetProvider
import msg.proto.protobuf.filedescriptors.SourceCodeFileDescriptorSetProvider
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolutePathString

class ProtobufRoots(
  protobufPaths: Collection<Path>,
  descriptorCachePath: Path,
  descriptorCacheMaxAgeMs: ULong,
) {
  val protobufRoots =
    protobufPaths.mapNotNull {
      try {
        val cacheFilePath = descriptorCachePath.resolve(getDescriptorCacheFileName(it.absolutePathString()))
        var fileDescriptorSetProvider: FileDescriptorSetProvider = SourceCodeFileDescriptorSetProvider(it)
        if (descriptorCacheMaxAgeMs > 0uL) {
          fileDescriptorSetProvider =
            DiskCachingFileDescriptorSetProvider(cacheFilePath, descriptorCacheMaxAgeMs, fileDescriptorSetProvider)
        }
        FileDescriptorSetProtobufRoot(fileDescriptorSetProvider.getFileDescriptorSet())
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

  private fun getDescriptorCacheFileName(protobufPath: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(protobufPath.toByteArray())
    return digest.toHexString()
  }
}
