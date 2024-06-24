package msg.kgb

import com.github.ajalt.clikt.core.Context
import msg.clikt.ProfileAwareCommand

class Kgb(args: Array<String>) : ProfileAwareCommand(args = args, name = "kgb") {
  override fun help(context: Context) = "Kafka GRPC Bridge"
  override fun run() = Unit
}
