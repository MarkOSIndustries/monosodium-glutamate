package msg.qs

import com.github.ajalt.clikt.core.Context
import msg.clikt.ProfileAwareCommand

class Qs(args: Array<String>) : ProfileAwareCommand(args = args, name = "qs") {
  override fun help(context: Context) = "Query store command line tool"
  override fun run() = Unit
}
