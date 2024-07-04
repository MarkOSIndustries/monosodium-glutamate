package msg.proto

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.google.protobuf.Descriptors
import java.util.concurrent.TimeUnit

abstract class GrpcMethodDataCommand : ProtobufCommand() {
  override val printHelpOnEmptyArgs: Boolean get() = true
  override val invokeWithoutSubcommand: Boolean get() = true

  private val serviceName by argument(
    name = "service",
    help = "the GRPC service to use (fully qualified)",
    completionCandidates = CompletionCandidates.Custom.fromStdout("""cat ${'$'}COMPLETIONS_PROTO_SERVICES""")
  )
  private val methodName by argument(
    name = "method",
    help = "the GRPC service method to use"
  )

  protected fun getServiceDescriptor(): Descriptors.ServiceDescriptor {
    return protobufRoots.findServiceDescriptor(serviceName) ?: run {
      System.err.println("Service $serviceName not found. Try >proto services")
      throw ProgramResult(1)
    }
  }
  protected fun getMethodDescriptor(serviceDescriptor: Descriptors.ServiceDescriptor): Descriptors.MethodDescriptor {
    return serviceDescriptor.findMethodByName(methodName) ?: run {
      System.err.println("Service $serviceName has no method $methodName. Try >proto services --methods $serviceName")
      throw ProgramResult(1)
    }
  }

  protected val inputEncoding by inputEncodingArgument()
  protected val outputEncoding by outputEncodingArgument()
  protected val inputBinaryPrefix by inputBinaryPrefixOption()
  protected val outputBinaryPrefix by outputBinaryPrefixOption()

  protected val hostsAndPorts by option("--host", "-h", help = "the host to connect to - can specify multiple times for round robin requests", metavar = "host:port").multiple(default = listOf("localhost:8082"))
  protected val defaultPort by option("--port", "-p", help = "the port to connect on (if not specified in host)").int().default(8082)
  protected val deadline by option("--deadline", "-d", help = "the deadline for GRPC requests (default 60). In seconds unless changed with --deadline-units").long().default(60L)
  protected val deadlineUnits by option("--deadline-units").choice(
    mapOf(
      "s" to TimeUnit.SECONDS,
      "m" to TimeUnit.MINUTES,
      "h" to TimeUnit.HOURS,
    )
  ).default(TimeUnit.SECONDS)
}
