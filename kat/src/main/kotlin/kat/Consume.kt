package kat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kat.kafka.Brokers
import kat.kafka.EarliestOffsetSpec
import kat.kafka.EphemeralConsumer
import java.nio.ByteBuffer

class Consume() : CliktCommand(help = "Consume records from Kafka") {
  val topic by argument("topic")
  val brokers by option(envvar = "KAFKA_BROKERS").default("localhost:9092")
  val encoding by option()

  override fun run() {
    val ephemeralConsumer = EphemeralConsumer(*Brokers.from(brokers))

    val sizeBufferArray = ByteArray(4)
    val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

    ephemeralConsumer.consumeFrom(topic, EarliestOffsetSpec()) { record ->
      // TODO: move swithcing to a lambda return func
      when(encoding) {
        "hex" -> {
          record.value().map { String.format("%02X", it) }.forEach { System.out.print(it) }
          println()
        }
        else -> {
          sizeBuffer.putInt(0, record.value().size)
          System.out.write(sizeBufferArray)
          System.out.write(record.value())
        }
      }

      true
    }
  }
}
