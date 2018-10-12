package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import msg.kafka.KafkaCommand
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.time.Duration

class Topics : KafkaCommand("Query topics\nRetrieves the names of all topics and prints to stdout") {
  private val query by argument(help = "Filter the topic list (case-insensitive, supports * wildcards)").default("*")
  private val not by option("--not", "-n", help = "Invert the filter to be exclusive rather than inclusive").flag(default = false)

  override fun run() {
    val pattern = ".*${query.split("*").map { Regex.escape(it) }.joinToString(".*")}.*"
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)

    val consumer = newConsumer(ByteArrayDeserializer::class, ByteArrayDeserializer::class)
    consumer.listTopics(Duration.ofMinutes(1))
      .filterKeys { if(not) !regex.matches(it) else regex.matches(it) }
      .toSortedMap()
      .forEach { println(it.key) }
  }
}
