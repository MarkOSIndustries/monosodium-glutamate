package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.ASCIIEncoding
import msg.encodings.delimiters.Delimiter
import msg.encodings.delimiters.Delimiters
import msg.kafka.KafkaTopicCommand
import msg.kat.encodings.Encodings

abstract class KafkaTopicDataCommand : KafkaTopicCommand() {
  protected val encoding by argument("encoding", "the stdin/stdout format for records (and in some cases keys). HEX,Base64 are line delimited ASCII. Others are length-prefixed binary.").choice(Encodings.byName)
  private val prefix by option("--prefix", help = "the prefix type to use for length prefixed binary encodings. Has no effect on line delimited ASCII encodings").choice(*Delimiters.lengthPrefixedBinary.keys.toTypedArray()).default("varint")

  protected fun delimiter(): Delimiter {
    return if (encoding is ASCIIEncoding) {
      val asciiEncoding = encoding as ASCIIEncoding
      Delimiters.ascii["newline"]!!(asciiEncoding)
    } else {
      Delimiters.lengthPrefixedBinary[prefix]!!
    }
  }
}
