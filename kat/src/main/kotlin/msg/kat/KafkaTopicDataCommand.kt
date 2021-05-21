package msg.kat

import com.github.ajalt.clikt.completion.ExperimentalCompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.choice
import msg.kafka.KafkaTopicCommand
import msg.kat.encodings.Encodings

@ExperimentalCompletionCandidates
abstract class KafkaTopicDataCommand(help: String) : KafkaTopicCommand(help) {
  protected val encoding by argument("encoding", "the stdin/stdout format for records (and in some cases keys). HEX,Base64 are line delimited ASCII. Others are length-prefixed binary.").choice(Encodings.byName)
}
