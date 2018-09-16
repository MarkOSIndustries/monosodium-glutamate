package msg.schemas;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.14.0)",
    comments = "Source: msg.proto")
public final class KafkaGRPCBridgeGrpc {

  private KafkaGRPCBridgeGrpc() {}

  public static final String SERVICE_NAME = "msg.KafkaGRPCBridge";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest,
      msg.schemas.MSG.KafkaRecord> getConsumeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "consume",
      requestType = msg.schemas.MSG.ConsumeRequest.class,
      responseType = msg.schemas.MSG.KafkaRecord.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest,
      msg.schemas.MSG.KafkaRecord> getConsumeMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest, msg.schemas.MSG.KafkaRecord> getConsumeMethod;
    if ((getConsumeMethod = KafkaGRPCBridgeGrpc.getConsumeMethod) == null) {
      synchronized (KafkaGRPCBridgeGrpc.class) {
        if ((getConsumeMethod = KafkaGRPCBridgeGrpc.getConsumeMethod) == null) {
          KafkaGRPCBridgeGrpc.getConsumeMethod = getConsumeMethod = 
              io.grpc.MethodDescriptor.<msg.schemas.MSG.ConsumeRequest, msg.schemas.MSG.KafkaRecord>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "msg.KafkaGRPCBridge", "consume"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.ConsumeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.KafkaRecord.getDefaultInstance()))
                  .setSchemaDescriptor(new KafkaGRPCBridgeMethodDescriptorSupplier("consume"))
                  .build();
          }
        }
     }
     return getConsumeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.OffsetsRequest,
      msg.schemas.MSG.OffsetsResponse> getOffsetsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "offsets",
      requestType = msg.schemas.MSG.OffsetsRequest.class,
      responseType = msg.schemas.MSG.OffsetsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.OffsetsRequest,
      msg.schemas.MSG.OffsetsResponse> getOffsetsMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.OffsetsRequest, msg.schemas.MSG.OffsetsResponse> getOffsetsMethod;
    if ((getOffsetsMethod = KafkaGRPCBridgeGrpc.getOffsetsMethod) == null) {
      synchronized (KafkaGRPCBridgeGrpc.class) {
        if ((getOffsetsMethod = KafkaGRPCBridgeGrpc.getOffsetsMethod) == null) {
          KafkaGRPCBridgeGrpc.getOffsetsMethod = getOffsetsMethod = 
              io.grpc.MethodDescriptor.<msg.schemas.MSG.OffsetsRequest, msg.schemas.MSG.OffsetsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "msg.KafkaGRPCBridge", "offsets"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.OffsetsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.OffsetsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new KafkaGRPCBridgeMethodDescriptorSupplier("offsets"))
                  .build();
          }
        }
     }
     return getOffsetsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static KafkaGRPCBridgeStub newStub(io.grpc.Channel channel) {
    return new KafkaGRPCBridgeStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static KafkaGRPCBridgeBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new KafkaGRPCBridgeBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static KafkaGRPCBridgeFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new KafkaGRPCBridgeFutureStub(channel);
  }

  /**
   */
  public static abstract class KafkaGRPCBridgeImplBase implements io.grpc.BindableService {

    /**
     */
    public void consume(msg.schemas.MSG.ConsumeRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.KafkaRecord> responseObserver) {
      asyncUnimplementedUnaryCall(getConsumeMethod(), responseObserver);
    }

    /**
     */
    public void offsets(msg.schemas.MSG.OffsetsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getOffsetsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getConsumeMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                msg.schemas.MSG.ConsumeRequest,
                msg.schemas.MSG.KafkaRecord>(
                  this, METHODID_CONSUME)))
          .addMethod(
            getOffsetsMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                msg.schemas.MSG.OffsetsRequest,
                msg.schemas.MSG.OffsetsResponse>(
                  this, METHODID_OFFSETS)))
          .build();
    }
  }

  /**
   */
  public static final class KafkaGRPCBridgeStub extends io.grpc.stub.AbstractStub<KafkaGRPCBridgeStub> {
    private KafkaGRPCBridgeStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KafkaGRPCBridgeStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KafkaGRPCBridgeStub(channel, callOptions);
    }

    /**
     */
    public void consume(msg.schemas.MSG.ConsumeRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.KafkaRecord> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getConsumeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void offsets(msg.schemas.MSG.OffsetsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getOffsetsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class KafkaGRPCBridgeBlockingStub extends io.grpc.stub.AbstractStub<KafkaGRPCBridgeBlockingStub> {
    private KafkaGRPCBridgeBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KafkaGRPCBridgeBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KafkaGRPCBridgeBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.KafkaRecord> consume(
        msg.schemas.MSG.ConsumeRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getConsumeMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.OffsetsResponse> offsets(
        msg.schemas.MSG.OffsetsRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getOffsetsMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class KafkaGRPCBridgeFutureStub extends io.grpc.stub.AbstractStub<KafkaGRPCBridgeFutureStub> {
    private KafkaGRPCBridgeFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private KafkaGRPCBridgeFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new KafkaGRPCBridgeFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_CONSUME = 0;
  private static final int METHODID_OFFSETS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final KafkaGRPCBridgeImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(KafkaGRPCBridgeImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CONSUME:
          serviceImpl.consume((msg.schemas.MSG.ConsumeRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.KafkaRecord>) responseObserver);
          break;
        case METHODID_OFFSETS:
          serviceImpl.offsets((msg.schemas.MSG.OffsetsRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class KafkaGRPCBridgeBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    KafkaGRPCBridgeBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return msg.schemas.MSG.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("KafkaGRPCBridge");
    }
  }

  private static final class KafkaGRPCBridgeFileDescriptorSupplier
      extends KafkaGRPCBridgeBaseDescriptorSupplier {
    KafkaGRPCBridgeFileDescriptorSupplier() {}
  }

  private static final class KafkaGRPCBridgeMethodDescriptorSupplier
      extends KafkaGRPCBridgeBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    KafkaGRPCBridgeMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (KafkaGRPCBridgeGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new KafkaGRPCBridgeFileDescriptorSupplier())
              .addMethod(getConsumeMethod())
              .addMethod(getOffsetsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
