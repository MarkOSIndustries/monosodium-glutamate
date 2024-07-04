package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.delimiters.Delimiters
import msg.kafka.KafkaTopicCommand
import msg.kat.encodings.KafkaEncodings

abstract class KafkaTopicDataCommand : KafkaTopicCommand() {
  protected val encoding by argument("encoding", "the stdin/stdout format for records (and in some cases keys). HEX,Base64 are line delimited ASCII encodings. Strings treats records as UTF8 strings with line delimiters. binary,msg.KafkaRecord,msg.TypedKafkaRecord are length-prefixed binary.").choice(KafkaEncodings.byName)
  protected val prefix by option("--prefix", help = "the prefix type to use for length prefixed binary encodings. Has no effect on line delimited ASCII encodings").choice(Delimiters.lengthPrefixedBinary).default(Delimiters.lengthPrefixedBinary["varint"]!!)
  protected val progress by option("--progress", help = "show a progress bar").flag()
}
