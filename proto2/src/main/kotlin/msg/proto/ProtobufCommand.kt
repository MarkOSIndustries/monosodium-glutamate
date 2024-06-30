package msg.proto

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.path
import msg.proto.protobuf.ProtobufRoots

abstract class ProtobufCommand : CliktCommand() {
  protected val protobufPaths by option("--protobufs", help = "paths which contain protobuf schemas (comma separated, env PROTO_HOME to override)", envvar = "PROTO_HOME")
    .path(canBeDir = true, canBeFile = false)
    .split(",")
    .default(emptyList())
  protected val protobufRoots by lazy {
    ProtobufRoots(protobufPaths)
  }

  fun debugString(): String {
    return """
      protobufPaths $protobufPaths
    """.trimIndent()
  }
}
