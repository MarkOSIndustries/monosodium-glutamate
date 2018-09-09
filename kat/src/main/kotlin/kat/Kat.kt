package kat

import com.github.ajalt.clikt.core.CliktCommand

class Kat : CliktCommand(name = "kat", help = "Kafka command line tool") {
  override fun run() = Unit
}
