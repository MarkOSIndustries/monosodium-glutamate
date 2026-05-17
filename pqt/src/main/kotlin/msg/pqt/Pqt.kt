package msg.pqt

import com.github.ajalt.clikt.core.Context
import msg.clikt.ProfileAwareCommand

class Pqt(
  args: Array<String>,
) : ProfileAwareCommand(args = args, name = "pqt") {
  override fun help(context: Context) = "Parquet command line tool"

  override fun run() = Unit
}
