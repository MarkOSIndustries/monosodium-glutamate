package msg.pqt

import com.github.ajalt.clikt.core.Context
import msg.clikt.ManPages.manPageGenerationOption
import msg.clikt.ProfileAwareCommand

class Pqt(
  args: Array<String>,
) : ProfileAwareCommand(args = args, name = "pqt") {
  init {
    manPageGenerationOption()
  }

  override fun help(context: Context) = "Parquet command line tool"

  override fun run() = Unit
}
