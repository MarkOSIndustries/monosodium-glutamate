package msg.proto

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.ulong
import msg.proto.protobuf.ProtobufRoots
import kotlin.io.path.Path

abstract class ProtobufCommand : CliktCommand() {
  protected val protobufPaths by option(
    "--protobufs",
    help = "paths which contain protobuf schemas (comma separated, env PROTO_HOME to override)",
    envvar = "PROTO_HOME",
  ).path(canBeDir = true, canBeFile = false)
    .split(",")
    .default(emptyList())
  protected val descriptorCachePath by option(
    "--cache",
    help = "the path where proto will cache message descriptors",
    envvar = "PROTO_CACHE",
  ).path(canBeDir = true, canBeFile = false, mustExist = true).default(Path("/tmp"))
  protected val descriptorCacheMaxAgeMs by option(
    "--cache-max-ms",
    help = "The maximum allowed age of the proto descriptor cache files",
    envvar = "PROTO_CACHE_MAX_MS",
  ).ulong().default(0uL)
  protected val protobufRoots by lazy {
    ProtobufRoots(protobufPaths, descriptorCachePath, descriptorCacheMaxAgeMs)
  }

  fun debugString(): String =
    """
    protobufPaths $protobufPaths
    """.trimIndent()
}
