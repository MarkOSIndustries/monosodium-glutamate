package msg.schemas;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.64.0)",
    comments = "Source: msg.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class QueryStoreGrpc {

  private QueryStoreGrpc() {}

  public static final java.lang.String SERVICE_NAME = "msg.QueryStore";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.GetRequest,
      msg.schemas.MSG.GetResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "get",
      requestType = msg.schemas.MSG.GetRequest.class,
      responseType = msg.schemas.MSG.GetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.GetRequest,
      msg.schemas.MSG.GetResponse> getGetMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.GetRequest, msg.schemas.MSG.GetResponse> getGetMethod;
    if ((getGetMethod = QueryStoreGrpc.getGetMethod) == null) {
      synchronized (QueryStoreGrpc.class) {
        if ((getGetMethod = QueryStoreGrpc.getGetMethod) == null) {
          QueryStoreGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.GetRequest, msg.schemas.MSG.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.GetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.GetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryStoreMethodDescriptorSupplier("get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.GetKeyCountsRequest,
      msg.schemas.MSG.GetKeyCountsResponse> getGetKeyCountsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getKeyCounts",
      requestType = msg.schemas.MSG.GetKeyCountsRequest.class,
      responseType = msg.schemas.MSG.GetKeyCountsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.GetKeyCountsRequest,
      msg.schemas.MSG.GetKeyCountsResponse> getGetKeyCountsMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.GetKeyCountsRequest, msg.schemas.MSG.GetKeyCountsResponse> getGetKeyCountsMethod;
    if ((getGetKeyCountsMethod = QueryStoreGrpc.getGetKeyCountsMethod) == null) {
      synchronized (QueryStoreGrpc.class) {
        if ((getGetKeyCountsMethod = QueryStoreGrpc.getGetKeyCountsMethod) == null) {
          QueryStoreGrpc.getGetKeyCountsMethod = getGetKeyCountsMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.GetKeyCountsRequest, msg.schemas.MSG.GetKeyCountsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getKeyCounts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.GetKeyCountsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.GetKeyCountsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryStoreMethodDescriptorSupplier("getKeyCounts"))
              .build();
        }
      }
    }
    return getGetKeyCountsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.PutRequest,
      msg.schemas.MSG.PutResponse> getPutMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "put",
      requestType = msg.schemas.MSG.PutRequest.class,
      responseType = msg.schemas.MSG.PutResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.PutRequest,
      msg.schemas.MSG.PutResponse> getPutMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.PutRequest, msg.schemas.MSG.PutResponse> getPutMethod;
    if ((getPutMethod = QueryStoreGrpc.getPutMethod) == null) {
      synchronized (QueryStoreGrpc.class) {
        if ((getPutMethod = QueryStoreGrpc.getPutMethod) == null) {
          QueryStoreGrpc.getPutMethod = getPutMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.PutRequest, msg.schemas.MSG.PutResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "put"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.PutRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.PutResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryStoreMethodDescriptorSupplier("put"))
              .build();
        }
      }
    }
    return getPutMethod;
  }

  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.ScanRequest,
      msg.schemas.MSG.GetResponse> getScanMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "scan",
      requestType = msg.schemas.MSG.ScanRequest.class,
      responseType = msg.schemas.MSG.GetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.ScanRequest,
      msg.schemas.MSG.GetResponse> getScanMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.ScanRequest, msg.schemas.MSG.GetResponse> getScanMethod;
    if ((getScanMethod = QueryStoreGrpc.getScanMethod) == null) {
      synchronized (QueryStoreGrpc.class) {
        if ((getScanMethod = QueryStoreGrpc.getScanMethod) == null) {
          QueryStoreGrpc.getScanMethod = getScanMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.ScanRequest, msg.schemas.MSG.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "scan"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.ScanRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.GetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryStoreMethodDescriptorSupplier("scan"))
              .build();
        }
      }
    }
    return getScanMethod;
  }

  private static volatile io.grpc.MethodDescriptor<msg.schemas.MSG.DeleteRequest,
      msg.schemas.MSG.DeleteResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "delete",
      requestType = msg.schemas.MSG.DeleteRequest.class,
      responseType = msg.schemas.MSG.DeleteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<msg.schemas.MSG.DeleteRequest,
      msg.schemas.MSG.DeleteResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<msg.schemas.MSG.DeleteRequest, msg.schemas.MSG.DeleteResponse> getDeleteMethod;
    if ((getDeleteMethod = QueryStoreGrpc.getDeleteMethod) == null) {
      synchronized (QueryStoreGrpc.class) {
        if ((getDeleteMethod = QueryStoreGrpc.getDeleteMethod) == null) {
          QueryStoreGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<msg.schemas.MSG.DeleteRequest, msg.schemas.MSG.DeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.DeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  msg.schemas.MSG.DeleteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryStoreMethodDescriptorSupplier("delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QueryStoreStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryStoreStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryStoreStub>() {
        @java.lang.Override
        public QueryStoreStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryStoreStub(channel, callOptions);
        }
      };
    return QueryStoreStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QueryStoreBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryStoreBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryStoreBlockingStub>() {
        @java.lang.Override
        public QueryStoreBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryStoreBlockingStub(channel, callOptions);
        }
      };
    return QueryStoreBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QueryStoreFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryStoreFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryStoreFutureStub>() {
        @java.lang.Override
        public QueryStoreFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryStoreFutureStub(channel, callOptions);
        }
      };
    return QueryStoreFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void get(msg.schemas.MSG.GetRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     */
    default void getKeyCounts(msg.schemas.MSG.GetKeyCountsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetKeyCountsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetKeyCountsMethod(), responseObserver);
    }

    /**
     */
    default void put(msg.schemas.MSG.PutRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.PutResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPutMethod(), responseObserver);
    }

    /**
     */
    default void scan(msg.schemas.MSG.ScanRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getScanMethod(), responseObserver);
    }

    /**
     */
    default void delete(msg.schemas.MSG.DeleteRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.DeleteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service QueryStore.
   */
  public static abstract class QueryStoreImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QueryStoreGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service QueryStore.
   */
  public static final class QueryStoreStub
      extends io.grpc.stub.AbstractAsyncStub<QueryStoreStub> {
    private QueryStoreStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryStoreStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryStoreStub(channel, callOptions);
    }

    /**
     */
    public void get(msg.schemas.MSG.GetRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getKeyCounts(msg.schemas.MSG.GetKeyCountsRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetKeyCountsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetKeyCountsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void put(msg.schemas.MSG.PutRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.PutResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void scan(msg.schemas.MSG.ScanRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getScanMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void delete(msg.schemas.MSG.DeleteRequest request,
        io.grpc.stub.StreamObserver<msg.schemas.MSG.DeleteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service QueryStore.
   */
  public static final class QueryStoreBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QueryStoreBlockingStub> {
    private QueryStoreBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryStoreBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryStoreBlockingStub(channel, callOptions);
    }

    /**
     */
    public msg.schemas.MSG.GetResponse get(msg.schemas.MSG.GetRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.GetKeyCountsResponse> getKeyCounts(
        msg.schemas.MSG.GetKeyCountsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetKeyCountsMethod(), getCallOptions(), request);
    }

    /**
     */
    public msg.schemas.MSG.PutResponse put(msg.schemas.MSG.PutRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPutMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<msg.schemas.MSG.GetResponse> scan(
        msg.schemas.MSG.ScanRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getScanMethod(), getCallOptions(), request);
    }

    /**
     */
    public msg.schemas.MSG.DeleteResponse delete(msg.schemas.MSG.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service QueryStore.
   */
  public static final class QueryStoreFutureStub
      extends io.grpc.stub.AbstractFutureStub<QueryStoreFutureStub> {
    private QueryStoreFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryStoreFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryStoreFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<msg.schemas.MSG.GetResponse> get(
        msg.schemas.MSG.GetRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<msg.schemas.MSG.PutResponse> put(
        msg.schemas.MSG.PutRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<msg.schemas.MSG.DeleteResponse> delete(
        msg.schemas.MSG.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET = 0;
  private static final int METHODID_GET_KEY_COUNTS = 1;
  private static final int METHODID_PUT = 2;
  private static final int METHODID_SCAN = 3;
  private static final int METHODID_DELETE = 4;

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
        case METHODID_GET:
          serviceImpl.get((msg.schemas.MSG.GetRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse>) responseObserver);
          break;
        case METHODID_GET_KEY_COUNTS:
          serviceImpl.getKeyCounts((msg.schemas.MSG.GetKeyCountsRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.GetKeyCountsResponse>) responseObserver);
          break;
        case METHODID_PUT:
          serviceImpl.put((msg.schemas.MSG.PutRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.PutResponse>) responseObserver);
          break;
        case METHODID_SCAN:
          serviceImpl.scan((msg.schemas.MSG.ScanRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.GetResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((msg.schemas.MSG.DeleteRequest) request,
              (io.grpc.stub.StreamObserver<msg.schemas.MSG.DeleteResponse>) responseObserver);
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
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              msg.schemas.MSG.GetRequest,
              msg.schemas.MSG.GetResponse>(
                service, METHODID_GET)))
        .addMethod(
          getGetKeyCountsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              msg.schemas.MSG.GetKeyCountsRequest,
              msg.schemas.MSG.GetKeyCountsResponse>(
                service, METHODID_GET_KEY_COUNTS)))
        .addMethod(
          getPutMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              msg.schemas.MSG.PutRequest,
              msg.schemas.MSG.PutResponse>(
                service, METHODID_PUT)))
        .addMethod(
          getScanMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              msg.schemas.MSG.ScanRequest,
              msg.schemas.MSG.GetResponse>(
                service, METHODID_SCAN)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              msg.schemas.MSG.DeleteRequest,
              msg.schemas.MSG.DeleteResponse>(
                service, METHODID_DELETE)))
        .build();
  }

  private static abstract class QueryStoreBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QueryStoreBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return msg.schemas.MSG.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QueryStore");
    }
  }

  private static final class QueryStoreFileDescriptorSupplier
      extends QueryStoreBaseDescriptorSupplier {
    QueryStoreFileDescriptorSupplier() {}
  }

  private static final class QueryStoreMethodDescriptorSupplier
      extends QueryStoreBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QueryStoreMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (QueryStoreGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QueryStoreFileDescriptorSupplier())
              .addMethod(getGetMethod())
              .addMethod(getGetKeyCountsMethod())
              .addMethod(getPutMethod())
              .addMethod(getScanMethod())
              .addMethod(getDeleteMethod())
              .build();
        }
      }
    }
    return result;
  }
}
