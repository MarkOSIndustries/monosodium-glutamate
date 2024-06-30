package msg.proto

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.google.protobuf.Descriptors

abstract class ProtobufDataCommand : ProtobufCommand() {
  override val printHelpOnEmptyArgs: Boolean get() = true

  private val messageName by argument(
    name = "message",
    help = "protobuf message to use (fully qualified)",
    completionCandidates = CompletionCandidates.Custom.fromStdout("""cat ${'$'}COMPLETIONS_PROTO_MESSAGES""")
  )

  protected fun getMessageDescriptor(): Descriptors.Descriptor {
    return protobufRoots.findMessageDescriptor(messageName) ?: run {
      System.err.println("Schema $messageName not found. Try >proto schemas")
      throw ProgramResult(1)
    }
  }
}
