package kat

import com.github.ajalt.clikt.core.subcommands
import kat.kafka.Broker
import kat.kafka.EarliestOffsetSpec
import kat.kafka.EphemeralConsumer
import java.lang.String.format
import java.nio.ByteBuffer

fun main(args: Array<String>) = Kat().subcommands(Consume(), Produce()).main(args)
