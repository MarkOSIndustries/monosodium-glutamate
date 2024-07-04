package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import msg.proto.terminal.Colours

class Services : ProtobufCommand() {
  override fun help(context: Context) = """
  Query services

  Gets the names and methods of all known services and prints to stdout
  """.trimIndent()

  private val query by argument(help = "Filter the service list by fully qualified name (case-insensitive, supports * wildcards)").default("*")
  private val not by option("--not", "-n", help = "Invert the filter to be exclusive rather than inclusive").flag(default = false)
  private val fields by option("--methods", "-m", help = "Include a description of the services' methods in the output").flag(default = false)

  override fun run() {
    val pattern = ".*${query.split("*").map { Regex.escape(it) }.joinToString(".*")}.*"
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)

    val terminal = Terminal()

    protobufRoots.getAllServiceDescriptors()
      .filter { not xor regex.matches(it.fullName) }
      .sortedBy { it.fullName }
      .forEach { serviceDescriptor ->
        terminal.println(Colours.service(serviceDescriptor.fullName))
        if (fields) {
          for (method in serviceDescriptor.methods) {
            val clientStreaming = if (method.isClientStreaming) "stream of " else ""
            val serverStreaming = if (method.isServerStreaming) "stream of " else ""
            terminal.println("  ${Colours.method(method.name)}: ${Colours.metadata(clientStreaming)}${Colours.type(method.inputType.fullName)} => ${Colours.metadata(serverStreaming)}${Colours.type(method.outputType.fullName)}")
          }
        }
      }
  }
}
