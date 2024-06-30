package msg.proto

import com.github.ajalt.clikt.core.Context

class Debug : ProtobufCommand() {
  override fun help(context: Context) = """
  Debug output
  """.trimIndent()

  override val hiddenFromHelp = true

  override fun run() {
    System.err.println(debugString())
  }
}
