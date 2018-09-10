package kat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kat.kafka.Brokers
import kat.kafka.Producer
import java.io.DataInputStream

class Produce() : CliktCommand() {
  val topic by argument("topic")
  val brokers by option(envvar = "KAFKA_BROKERS").default("localhost:9092")
  val encoding by option()

  private val MAX_MESSAGE_SIZE: Int = 65536 // 64 kb should be enough

  override fun run() {
//    val producer = Producer(*Brokers.from(brokers))
//
//    val byteArray: ByteArray(MAX_MESSAGE_SIZE)
//    while(true) {
//      val input = DataInputStream(System.`in`)
//      val nextMessageSize = input.readInt()
//      input.readFully()
//    }
  }
}
