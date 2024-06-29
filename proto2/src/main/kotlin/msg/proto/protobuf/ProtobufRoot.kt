package msg.proto.protobuf

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
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
import kotlin.streams.toList

class ProtobufRoot(private val protobufHome: Path) {
  private val schema = loadSchema(protobufHome)
  private val protoFileEncoder = SchemaEncoder(schema)
  private val fileDescriptorCache = schema.protoFiles.associate { it.location.path to LazyFileDescriptor(schema, protoFileEncoder, ::getFileDescriptor, it.location.path) }

  // TODO Wire will give us a LinkedHashMap for free, keeping as a backup
  //    val adapterThing = schemas.fold(null as ProtoAdapter<Any>?) { found, schema ->
  //      found ?: schema.protoAdapter(messageName, true)
  //    }
  //    val message = adapterThing.decode(bytes)
  //    println("OH MY ${message.javaClass.name}")
  //    println("OH MY $message")

  class LazyFileDescriptor(private val schema: Schema, private val protoFileEncoder: SchemaEncoder, private val getFileDescriptor: (String) -> Descriptors.FileDescriptor, path: String) {
    val fileDescriptor: Descriptors.FileDescriptor by lazy {
      val fileDescriptorProto = DescriptorProtos.FileDescriptorProto.parseFrom(protoFileEncoder.encode(schema.protoFile(path)!!).asByteBuffer())
      Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, fileDescriptorProto.dependencyList.map { getFileDescriptor(it) }.toTypedArray())
    }
  }

  fun getMessageDescriptors(): List<Descriptors.Descriptor> {
    return schema.protoFiles.flatMap { getFileDescriptor(it.location.path).messageTypes }
  }

  fun findMessageDescriptor(message: String): Descriptors.Descriptor? {
    val separatorIndex = message.lastIndexOf('.')
    val messageNamespace = message.substring(0 until separatorIndex)
    val messageSimpleName = message.substring(separatorIndex + 1)

    return findFileDescriptor { protoFile -> protoFile.types.any { it.type.enclosingTypeOrPackage == messageNamespace && it.type.simpleName == messageSimpleName } }?.let { fileDescriptor ->
      return fileDescriptor.messageTypes.find { it.fullName == message }
    }
  }

  fun getServiceDescriptors(): List<Descriptors.ServiceDescriptor> {
    return schema.protoFiles.flatMap { getFileDescriptor(it.location.path).services }
  }

  fun findServiceDescriptor(service: String): Descriptors.ServiceDescriptor? {
    val separatorIndex = service.lastIndexOf('.')
    val serviceNamespace = service.substring(0 until separatorIndex)
    val serviceSimpleName = service.substring(separatorIndex + 1)

    return findFileDescriptor { protoFile -> protoFile.services.any { it.type.enclosingTypeOrPackage == serviceNamespace && it.type.simpleName == serviceSimpleName } }?.let { fileDescriptor ->
      return fileDescriptor.services.find { it.fullName == service }
    }
  }

  private fun findFileDescriptor(predicate: (ProtoFile) -> Boolean): Descriptors.FileDescriptor? {
    return schema.protoFiles.find { predicate(it) }?.let { protoFile ->
      return getFileDescriptor(protoFile.location.path)
    }
  }

  private fun getFileDescriptor(path: String): Descriptors.FileDescriptor {
    return fileDescriptorCache[path]!!.fileDescriptor
  }

  private fun loadSchema(protobufHome: Path): Schema {
    val schemaLoader = SchemaLoader(FileSystem.SYSTEM)

    val locations = getLocations()

    schemaLoader.loadExhaustively = true
    schemaLoader.initRoots(locations, listOf(Location.Companion.get(protobufHome.pathString)))
    return schemaLoader.loadSchema()
  }

  private fun getLocations(parentDir: Path = protobufHome): List<Location> {
    return Files.list(parentDir).flatMap { path ->
      when {
        path.isDirectory() -> getLocations(path.absolute()).stream()
        path.isReadable() && path.pathString.endsWith(".proto") -> Stream.of(
          Location.get(
            protobufHome.pathString,
            protobufHome.relativize(path).pathString
          )
        )

        else -> Stream.empty()
      }
    }.toList()
  }
}
