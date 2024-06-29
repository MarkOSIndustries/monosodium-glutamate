package msg.proto.grpc

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import io.grpc.MethodDescriptor
import io.grpc.protobuf.ProtoUtils

class GrpcMethod(serviceDescriptor: Descriptors.ServiceDescriptor, methodDescriptor: Descriptors.MethodDescriptor) {
  private val inputTypeMarshaller = ProtoUtils.marshaller(DynamicMessage.newBuilder(methodDescriptor.inputType).buildPartial() as Message)
  private val outputTypeMarshaller = ProtoUtils.marshaller(DynamicMessage.newBuilder(methodDescriptor.outputType).buildPartial())
  val methodDescriptor = MethodDescriptor.newBuilder<Message, DynamicMessage>()
    .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceDescriptor.fullName, methodDescriptor.name))
    .setRequestMarshaller(inputTypeMarshaller)
    .setResponseMarshaller(outputTypeMarshaller)
    .setType(
      when {
        (methodDescriptor.isServerStreaming && methodDescriptor.isClientStreaming) -> MethodDescriptor.MethodType.BIDI_STREAMING
        (methodDescriptor.isServerStreaming) -> MethodDescriptor.MethodType.SERVER_STREAMING
        (methodDescriptor.isClientStreaming) -> MethodDescriptor.MethodType.CLIENT_STREAMING
        else -> MethodDescriptor.MethodType.UNARY
      }
    )
    .build()
}
