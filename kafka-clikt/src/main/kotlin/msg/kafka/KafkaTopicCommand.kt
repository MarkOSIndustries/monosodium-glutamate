package msg.kafka

import com.github.ajalt.clikt.parameters.arguments.argument

abstract class KafkaTopicCommand(help: String) : KafkaCommand(help) {
  protected val topic by argument(help = "the name of the topic")
}
