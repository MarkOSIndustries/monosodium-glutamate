package msg.kat

import com.github.ajalt.clikt.core.Context
import msg.clikt.ProfileAwareCommand

class Kat(args: Array<String>) : ProfileAwareCommand(args = args, name = "kat") {
  override fun help(context: Context) = "Kafka command line tool"
  override fun run() {}
}
