package msg.pqt.clikt

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import msg.clikt.protobuf.ProtobufDataCommand

abstract class ProtobufWriteFileCommand(
  name: String? = null,
) : ProtobufDataCommand(name) {
  protected val file by option(
    "--path",
    "-p",
    help = "the path to the parquet file",
  ).file(canBeDir = false).required()
}
