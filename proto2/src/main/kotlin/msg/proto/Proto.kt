package msg.proto

import com.github.ajalt.clikt.core.Context
import msg.clikt.ManPages.manPageGenerationOption
import msg.clikt.ProfileAwareCommand

class Proto(
  args: Array<String>,
) : ProfileAwareCommand(args = args, name = "proto") {
  init {
    manPageGenerationOption()
  }

  override fun help(context: Context) = "Protobuf command line tool"

  override fun run() {}
}
