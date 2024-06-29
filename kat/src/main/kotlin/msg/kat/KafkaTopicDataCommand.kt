package msg.kat

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.StringEncoding
import msg.encodings.Transport
import msg.encodings.delimiters.Delimiters
import msg.kafka.KafkaTopicCommand
import msg.kat.encodings.KafkaEncodings

abstract class KafkaTopicDataCommand : KafkaTopicCommand() {
  protected val encoding by argument("encoding", "the stdin/stdout format for records (and in some cases keys). HEX,Base64 are line delimited ASCII. Others are length-prefixed binary.").choice(KafkaEncodings.byName)
  private val prefix by option("--prefix", help = "the prefix type to use for length prefixed binary encodings. Has no effect on line delimited ASCII encodings").choice(*Delimiters.lengthPrefixedBinary.keys.toTypedArray()).default("varint")

  protected fun delimiter(): Transport<ByteArray> {
    return if (encoding is StringEncoding.OfBinary) {
      val asciiEncoding = encoding as StringEncoding.OfBinary
      Delimiters.makeStringNewlineDelimiter(asciiEncoding)
    } else {
      Delimiters.lengthPrefixedBinary[prefix]!!
    }
  }
}
