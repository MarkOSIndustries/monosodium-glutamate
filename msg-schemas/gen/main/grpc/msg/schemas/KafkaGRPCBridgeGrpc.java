package msg.schemas;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.64.0)",
    comments = "Source: msg.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class KafkaGRPCBridgeGrpc {

  private KafkaGRPCBridgeGrpc() {}

  public static final java.lang.String SERVICE_NAME = "msg.KafkaGRPCBridge";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest,
      msg.schemas.MSG.TypedKafkaRecord> getConsumeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "consume",
      requestType = msg.schemas.MSG.ConsumeRequest.class,
      responseType = msg.schemas.MSG.TypedKafkaRecord.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest,
      msg.schemas.MSG.TypedKafkaRecord> getConsumeMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.ConsumeRequest, msg.schemas.MSG.TypedKafkaRecord> getConsumeMethod;
    if ((getConsumeMethod = KafkaGRPCBridgeGrpc.getConsumeMethod) == null) {
      synchronized (KafkaGRPCBridgeGrpc.class) {
        if ((getConsumeMethod = KafkaGRPCBridgeGrpc.getConsumeMethod) == null) {
          KafkaGRPCBridgeGrpc.getConsumeMethod = getConsumeMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.ConsumeRequest, msg.schemas.MSG.TypedKafkaRecord>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "consume"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.ConsumeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.TypedKafkaRecord.getDefaultInstance()))
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
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "offsets"))
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
    io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeStub>() {
        @java.lang.Override
        public KafkaGRPCBridgeStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KafkaGRPCBridgeStub(channel, callOptions);
        }
      };
    return KafkaGRPCBridgeStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static KafkaGRPCBridgeBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeBlockingStub>() {
        @java.lang.Override
        public KafkaGRPCBridgeBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KafkaGRPCBridgeBlockingStub(channel, callOptions);
        }
      };
    return KafkaGRPCBridgeBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static KafkaGRPCBridgeFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<KafkaGRPCBridgeFutureStub>() {
        @java.lang.Override
        public KafkaGRPCBridgeFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new KafkaGRPCBridgeFutureStub(channel, callOptions);
        }
      };
    return KafkaGRPCBridgeFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void consume(msg.schemas.MSG.ConsumeRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.TypedKafkaRecord> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getConsumeMethod(), responseObserver);
    }

    /**
     */
    default void offsets(msg.schemas.MSG.OffsetsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOffsetsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service KafkaGRPCBridge.
   */
  public static abstract class KafkaGRPCBridgeImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return KafkaGRPCBridgeGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service KafkaGRPCBridge.
   */
  public static final class KafkaGRPCBridgeStub
      extends io.grpc.stub.AbstractAsyncStub<KafkaGRPCBridgeStub> {
    private KafkaGRPCBridgeStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new KafkaGRPCBridgeStub(channel, callOptions);
    }

    /**
     */
    public void consume(msg.schemas.MSG.ConsumeRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.TypedKafkaRecord> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getConsumeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void offsets(msg.schemas.MSG.OffsetsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.OffsetsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getOffsetsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service KafkaGRPCBridge.
   */
  public static final class KafkaGRPCBridgeBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<KafkaGRPCBridgeBlockingStub> {
    private KafkaGRPCBridgeBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new KafkaGRPCBridgeBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.TypedKafkaRecord> consume(
        msg.schemas.MSG.ConsumeRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getConsumeMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.OffsetsResponse> offsets(
        msg.schemas.MSG.OffsetsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getOffsetsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service KafkaGRPCBridge.
   */
  public static final class KafkaGRPCBridgeFutureStub
      extends io.grpc.stub.AbstractFutureStub<KafkaGRPCBridgeFutureStub> {
    private KafkaGRPCBridgeFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected KafkaGRPCBridgeFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CONSUME:
          serviceImpl.consume((msg.schemas.MSG.ConsumeRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.TypedKafkaRecord>) responseObserver);
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

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getConsumeMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              msg.schemas.MSG.ConsumeRequest,
              msg.schemas.MSG.TypedKafkaRecord>(
                service, METHODID_CONSUME)))
        .addMethod(
          getOffsetsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              msg.schemas.MSG.OffsetsRequest,
              msg.schemas.MSG.OffsetsResponse>(
                service, METHODID_OFFSETS)))
        .build();
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
    private final java.lang.String methodName;

    KafkaGRPCBridgeMethodDescriptorSupplier(java.lang.String methodName) {
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
