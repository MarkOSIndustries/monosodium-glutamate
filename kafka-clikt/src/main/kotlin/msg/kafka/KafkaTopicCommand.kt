package msg.kafka

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument

abstract class KafkaTopicCommand : KafkaCommand() {
  override val printHelpOnEmptyArgs: Boolean get() = true

  protected val topic by argument(
    help = "the name of the topic",
    completionCandidates = CompletionCandidates.Custom.fromStdout("""cat ${'$'}COMPLETIONS_KAFKA_TOPICS""")
  )
}
