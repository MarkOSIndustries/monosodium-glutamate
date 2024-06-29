package msg.proto

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import msg.encodings.delimiters.Delimiters
import msg.proto.encodings.ProtobufEncodings

abstract class ProtobufDataCommand : ProtobufCommand() {
  override val printHelpOnEmptyArgs: Boolean get() = true

  protected val messageName by argument(
    help = "protobuf message to use (fully qualified)",
    completionCandidates = CompletionCandidates.Custom.fromStdout("""cat ${'$'}COMPLETIONS_PROTO_MESSAGES""")
  )

  protected val inputEncoding by argument("input", help = "the stdin format for messages. hex,base64,json are delimited strings. Others are length-prefixed binary.").choice(
    ProtobufEncodings.byName
  )
  protected val outputEncoding by argument("output", help = "the stdout format for messages. hex,base64,json are delimited strings. Others are length-prefixed binary.").choice(
    ProtobufEncodings.byName
  )
  protected val inputBinaryPrefix by option("--input-prefix", help = "the prefix type to use for length prefixed binary encodings. Has no effect on line delimited string encodings").choice(*Delimiters.lengthPrefixedBinary.keys.toTypedArray()).default("varint")
  protected val outputBinaryPrefix by option("--output-prefix", help = "the prefix type to use for length prefixed binary encodings. Has no effect on line delimited string encodings").choice(*Delimiters.lengthPrefixedBinary.keys.toTypedArray()).default("varint")
}
