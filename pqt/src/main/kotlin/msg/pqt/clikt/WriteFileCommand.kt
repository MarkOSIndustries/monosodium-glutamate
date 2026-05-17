package msg.pqt.clikt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

abstract class WriteFileCommand(
  name: String? = null,
) : CliktCommand(name) {
  protected val file by option(
    "--path",
    "-p",
    help = "the path to the parquet file",
  ).file(canBeDir = false, mustBeWritable = true).required()
}
