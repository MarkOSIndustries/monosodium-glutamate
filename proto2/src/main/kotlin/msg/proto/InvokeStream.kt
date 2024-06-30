package msg.proto

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import io.grpc.CallOptions
import io.grpc.StatusRuntimeException
import io.grpc.stub.ClientCalls
import msg.proto.encodings.MessageTransport
import msg.proto.grpc.GrpcHosts
import msg.proto.grpc.GrpcMethod
import msg.proto.grpc.GrpcResponseWriter
import java.io.IOException

class InvokeStream : GrpcMethodDataCommand() {
  override fun help(context: Context) = """
    Invoke a GRPC method using client streaming

    Reads requests from stdin and makes a single GRPC call to stream them, writing responses to stdout
  """.trimIndent()

  override fun run() {
    val serviceDescriptor = getServiceDescriptor()
    val methodDescriptor = getMethodDescriptor(serviceDescriptor)

    val grpcMethod = GrpcMethod(serviceDescriptor, methodDescriptor)
    val grpcHosts = GrpcHosts(hostsAndPorts, defaultPort)

    val clientCall = grpcHosts.managedChannel.newCall(
      grpcMethod.methodDescriptor,
      CallOptions.DEFAULT.withDeadlineAfter(deadline, deadlineUnits)
    )

    val reader = MessageTransport(methodDescriptor.inputType).reader(inputEncoding(protobufRoots), inputBinaryPrefix, System.`in`)
    val writer = MessageTransport(methodDescriptor.outputType).writer(outputEncoding(protobufRoots), outputBinaryPrefix, System.out)
    val grpcResponseWriter = GrpcResponseWriter(writer)
    val requestObserver = ClientCalls.asyncBidiStreamingCall(clientCall, grpcResponseWriter)
    try {
      while (reader.hasNext()) {
        requestObserver.onNext(reader.next())
      }
      requestObserver.onCompleted()
      grpcResponseWriter.awaitStreamCompletion()
    } catch (t: IOException) {
      // Ignore, this will be either:
      // - we just terminated between hasNext and next()
      // - the output stream was closed
    } catch (ex: StatusRuntimeException) {
      System.err.println("${ex.message} - ${ex.status}")
      throw ProgramResult(ex.status.code.value())
    } catch (ex: Exception) {
      System.err.println("${ex.message}")
      try { requestObserver.onError(ex) } catch (_: Exception) { }
      throw ProgramResult(2)
    }
  }
}
