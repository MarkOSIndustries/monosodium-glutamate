package msg.kgb

import com.github.ajalt.clikt.core.CliktCommand

class Kgb : CliktCommand(name = "kgb", help = "Kafka GRPC Bridge") {
  override fun run() = Unit
}
