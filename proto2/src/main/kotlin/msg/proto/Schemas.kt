package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE
import msg.proto.colours.Colours

class Schemas : ProtobufCommand() {
  override fun help(context: Context) = """
  Query schemas

  Gets the names and fields of all known schemas and prints to stdout
  """.trimIndent()

  private val query by argument(help = "Filter the schema list by fully qualified name (case-insensitive, supports * wildcards)").default("*")
  private val not by option("--not", "-n", help = "Invert the filter to be exclusive rather than inclusive").flag(default = false)
  private val fields by option("--fields", "-f", help = "Include a description of the schemas' fields in the output").flag(default = false)

  override fun run() {
    val pattern = ".*${query.split("*").map { Regex.escape(it) }.joinToString(".*")}.*"
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)

    val terminal = Terminal()

    protobufRoots.getAllMessageDescriptors()
      .filter { not xor regex.matches(it.fullName) }
      .sortedBy { it.fullName }
      .distinctBy { it.fullName } // handle multiple sources importing common protobufs, like google ones
      .forEach { messageDescriptor ->
        terminal.println(Colours.message(messageDescriptor.fullName))
        if (fields) {
          for (field in messageDescriptor.fields) {
            val type = if (field.type == MESSAGE) field.messageType.fullName else field.type.name.lowercase()
            terminal.println("  ${Colours.field(field.name)}: ${Colours.type(type)}")
          }
        }
      }
  }
}
