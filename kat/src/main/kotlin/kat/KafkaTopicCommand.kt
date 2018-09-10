package kat

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

abstract class KafkaTopicCommand(help:String) : CliktCommand(help) {
  val brokers by option(help = "comma separated list of broker addresses",envvar = "KAFKA_BROKERS").default("localhost:9092")
  val topic by argument("topic", "the name of the topic")
}
