package msg.kat

import com.github.ajalt.clikt.core.Context
import msg.kafka.KafkaCommand

class Debug : KafkaCommand() {
  override fun help(context: Context) = """
  Debug output
  """.trimIndent()

  override val hiddenFromHelp = true

  override fun run() {
    System.err.println(debugString())
  }
}
