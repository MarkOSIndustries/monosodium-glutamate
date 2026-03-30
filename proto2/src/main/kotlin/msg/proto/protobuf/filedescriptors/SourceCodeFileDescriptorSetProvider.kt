package msg.proto.protobuf.filedescriptors

import com.google.protobuf.DescriptorProtos
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import com.squareup.wire.schema.internal.SchemaEncoder
import okio.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.pathString

class SourceCodeFileDescriptorSetProvider(
  private val protobufHome: Path,
) : FileDescriptorSetProvider {
  private fun loadSchema(protobufHome: Path): Schema {
    val schemaLoader = SchemaLoader(FileSystem.SYSTEM)

    val locations = getLocations()

    schemaLoader.loadExhaustively = true
    schemaLoader.initRoots(locations, listOf(Location.Companion.get(protobufHome.pathString)))
    return schemaLoader.loadSchema()
  }

  private fun getLocations(parentDir: Path = protobufHome): List<Location> =
    Files
      .list(parentDir)
      .flatMap { path ->
        when {
          path.isDirectory() -> getLocations(path.absolute()).stream()
          path.isReadable() && path.pathString.endsWith(".proto") ->
            Stream.of(
              Location.get(
                protobufHome.pathString,
                protobufHome.relativize(path).pathString,
              ),
            )

          else -> Stream.empty()
        }
      }.toList()

  override fun getFileDescriptorSet(): DescriptorProtos.FileDescriptorSet {
    val schema = loadSchema(protobufHome)
    val protoFileEncoder = SchemaEncoder(schema)
    val fileDescriptorProtos =
      schema.protoFiles.map {
        DescriptorProtos.FileDescriptorProto.parseFrom(
          protoFileEncoder.encode(schema.protoFile(it.location.path)!!).asByteBuffer(),
        )
      }
    return DescriptorProtos.FileDescriptorSet
      .newBuilder()
      .addAllFile(fileDescriptorProtos)
      .build()
  }
}
