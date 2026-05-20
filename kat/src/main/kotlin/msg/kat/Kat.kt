package msg.kat

import com.github.ajalt.clikt.core.Context
import msg.clikt.ManPages.manPageGenerationOption
import msg.clikt.ProfileAwareCommand

class Kat(
  args: Array<String>,
) : ProfileAwareCommand(args = args, name = "kat") {
  init {
    manPageGenerationOption()
  }

  override fun help(context: Context) = "Kafka command line tool"

  override fun run() {}
}
