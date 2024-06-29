package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import io.grpc.CallOptions
import io.grpc.StatusRuntimeException
import io.grpc.stub.ClientCalls
import msg.proto.encodings.MessageTransport
import msg.proto.grpc.GrpcHosts
import msg.proto.grpc.GrpcMethod
import java.io.EOFException
import java.util.concurrent.TimeUnit

class Invoke : GrpcMethodDataCommand() {
  private val inFlightLimit by option("--inflight", "-l", help = "the maximum number of requests simultaneously awaiting response").long().default(Long.MAX_VALUE)
  private val hostAndPort by option("--host", "-h", help = "the host to connect to - can specify multiple times for round robin requests", metavar = "host:port").default("localhost") // .multiple(default = listOf("localhost:8082"))
  private val defaultPort by option("--port", "-p", help = "the port to connect on (if not specified in host)").int().default(8082)
  private val deadline by option("--deadline", "-d", help = "the deadline for GRPC requests. In seconds unless changed with --deadline-units").long().default(60L)
  private val deadlineUnits by option("--deadline-units").choice(TimeUnit.entries.associateBy { it.name.lowercase() }).default(TimeUnit.SECONDS)

  override fun help(context: Context) = """
    Invoke a GRPC method

    Reads requests from stdin and makes a new GRPC call for each request, writing responses to stdout
  """.trimIndent()

  override fun run() {
    val serviceDescriptor = protobufRoots.findServiceDescriptor(serviceName)
    if (serviceDescriptor == null) {
      System.err.println("Service $serviceName not found. Try >proto services")
      throw ProgramResult(1)
    }

    val methodDescriptor = serviceDescriptor.findMethodByName(methodName)
    if (methodDescriptor == null) {
      System.err.println("Service $serviceName has no method $methodName. Try >proto services $serviceName")
      throw ProgramResult(1)
    }

    val grpcMethod = GrpcMethod(serviceDescriptor, methodDescriptor)
    val grpcHosts = GrpcHosts(hostAndPort, defaultPort)

    try {
      val reader = MessageTransport(methodDescriptor.inputType).reader(inputEncoding(protobufRoots), inputBinaryPrefix, System.`in`)
      val writer = MessageTransport(methodDescriptor.outputType).writer(outputEncoding(protobufRoots), outputBinaryPrefix, System.out)
      while (reader.hasNext()) {
        val request = reader.next()

        try {
          val responses = ClientCalls.blockingServerStreamingCall(
            grpcHosts.managedChannel,
            grpcMethod.methodDescriptor,
            CallOptions.DEFAULT.withDeadlineAfter(deadline, deadlineUnits),
            request
          )
          while (responses.hasNext()) {
            writer(responses.next())
          }
        } catch (e: StatusRuntimeException) {
          System.err.println("${e.message} - ${e.status}")
          throw ProgramResult(e.status.code.value())
        }
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }
  }
}
