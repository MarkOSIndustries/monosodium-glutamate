package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import io.grpc.CallOptions
import io.grpc.StatusRuntimeException
import io.grpc.stub.ClientCalls
import msg.proto.encodings.MessageTransport
import msg.proto.grpc.GrpcHosts
import msg.proto.grpc.GrpcMethod
import msg.proto.grpc.GrpcResponseWriter
import java.io.EOFException
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

class Invoke : GrpcMethodDataCommand() {
  private val inFlightLimit by option("--inflight", "-l", help = "the maximum number of requests simultaneously awaiting response").int().default(Int.MAX_VALUE).validate {
    require(it > 0) {
      """
        $it
        At least one request must be allowed in-flight simultaneously
      """.trimIndent()
    }
  }

  override fun help(context: Context) = """
    Invoke a GRPC method

    Reads requests from stdin and makes a new GRPC call for each request, writing responses to stdout
  """.trimIndent()

  override fun run() {
    val serviceDescriptor = getServiceDescriptor()
    val methodDescriptor = getMethodDescriptor(serviceDescriptor)

    val grpcMethod = GrpcMethod(serviceDescriptor, methodDescriptor)
    val grpcHosts = GrpcHosts(hostsAndPorts, defaultPort)

    val exitCode = AtomicInteger(0)
    val inFlightSemaphore = Semaphore(inFlightLimit, true)
    try {
      val reader = MessageTransport(methodDescriptor.inputType).reader(inputEncoding(protobufRoots), inputBinaryPrefix, System.`in`)
      val writer = MessageTransport(methodDescriptor.outputType).writer(outputEncoding(protobufRoots), outputBinaryPrefix, System.out)
      while (reader.hasNext() && exitCode.get() == 0) {
        val request = reader.next()

        inFlightSemaphore.acquire()
        val clientCall = grpcHosts.managedChannel.newCall(
          grpcMethod.methodDescriptor,
          CallOptions.DEFAULT.withDeadlineAfter(deadline, deadlineUnits)
        )
        val grpcResponseWriter = GrpcResponseWriter(writer)
        ClientCalls.asyncServerStreamingCall(clientCall, request, grpcResponseWriter)
        grpcResponseWriter.onStreamCompletion { maybeException ->
          inFlightSemaphore.release()
          when (maybeException) {
            is StatusRuntimeException -> {
              System.err.println("${maybeException.message} - ${maybeException.status}")
              exitCode.set(maybeException.status.code.value())
            }
            is Exception -> {
              System.err.println("${maybeException.message}")
              exitCode.set(2)
            }
          }
        }
      }
    } catch (t: EOFException) {
      // Ignore, we just terminated between hasNext and next()
    }
    inFlightSemaphore.acquire(inFlightLimit)
  }
}
