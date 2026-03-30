package msg.proto.protobuf.filedescriptors

import com.google.protobuf.DescriptorProtos
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class DiskCachingFileDescriptorSetProvider(
  private val cacheFilePath: Path,
  private val cacheMaxAgeMs: ULong,
  private val newValueProvider: FileDescriptorSetProvider,
) : FileDescriptorSetProvider {
  override fun getFileDescriptorSet(): DescriptorProtos.FileDescriptorSet {
    val fileDescriptorSet =
      if (!Files.exists(cacheFilePath) ||
        cacheMaxAgeMs < (System.currentTimeMillis() - Files.getLastModifiedTime(cacheFilePath).toMillis()).toULong()
      ) {
        val compiledFileDescriptorSet = newValueProvider.getFileDescriptorSet()
        compiledFileDescriptorSet.writeTo(FileOutputStream(File(cacheFilePath.absolutePathString())))
        compiledFileDescriptorSet
      } else {
        DescriptorProtos.FileDescriptorSet.parseFrom(
          FileInputStream(File(cacheFilePath.absolutePathString())),
        )
      }

    return fileDescriptorSet
  }
}
