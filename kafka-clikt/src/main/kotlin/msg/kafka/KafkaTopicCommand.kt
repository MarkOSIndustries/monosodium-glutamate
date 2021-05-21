package msg.kafka

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument

@ExperimentalCompletionCandidates
abstract class KafkaTopicCommand(help: String) : KafkaCommand(help) {
  protected val topic by argument(
    help = "the name of the topic",
    completionCandidates = CompletionCandidates.Custom.fromStdout("""cat ${'$'}COMPLETIONS_KAFKA_TOPICS""")
  )
}
