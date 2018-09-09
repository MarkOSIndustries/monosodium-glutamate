package kat

import kat.kafka.Broker
import kat.kafka.EarliestOffsetSpec
import kat.kafka.EphemeralConsumer
import java.lang.String.format
import java.nio.ByteBuffer

fun main(args: Array<String>) {
  if (args.size == 0) {
    throw RuntimeException("Please provide a schema name")
  }

  val topic = args[0]

  val hex = args.size > 1

  val ephemeralConsumer = EphemeralConsumer(Broker("localhost", 9092))

  val sizeBufferArray = ByteArray(4)
  val sizeBuffer:ByteBuffer = ByteBuffer.wrap(sizeBufferArray)

  ephemeralConsumer.consumeFrom(topic, EarliestOffsetSpec()) { record ->
    if(hex) {
      record.value().map { format("%02X", it) }.forEach { System.out.print(it) }
      println()
    } else {
      sizeBuffer.putInt(0, record.value().size)
      System.out.write(sizeBufferArray)
      System.out.write(record.value())
    }

    true
  }
}
