package com.sentinel.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.61.0)",
    comments = "Source: monitor.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SentinelServiceGrpc {

  private SentinelServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "monitor.SentinelService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.sentinel.grpc.SystemStats,
      com.sentinel.grpc.Alert> getStreamStatsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamStats",
      requestType = com.sentinel.grpc.SystemStats.class,
      responseType = com.sentinel.grpc.Alert.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.sentinel.grpc.SystemStats,
      com.sentinel.grpc.Alert> getStreamStatsMethod() {
    io.grpc.MethodDescriptor<com.sentinel.grpc.SystemStats, com.sentinel.grpc.Alert> getStreamStatsMethod;
    if ((getStreamStatsMethod = SentinelServiceGrpc.getStreamStatsMethod) == null) {
      synchronized (SentinelServiceGrpc.class) {
        if ((getStreamStatsMethod = SentinelServiceGrpc.getStreamStatsMethod) == null) {
          SentinelServiceGrpc.getStreamStatsMethod = getStreamStatsMethod =
              io.grpc.MethodDescriptor.<com.sentinel.grpc.SystemStats, com.sentinel.grpc.Alert>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamStats"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.sentinel.grpc.SystemStats.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.sentinel.grpc.Alert.getDefaultInstance()))
              .setSchemaDescriptor(new SentinelServiceMethodDescriptorSupplier("StreamStats"))
              .build();
        }
      }
    }
    return getStreamStatsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.sentinel.grpc.MonitorRequest,
      com.sentinel.grpc.SystemStats> getSubscribeToDashboardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubscribeToDashboard",
      requestType = com.sentinel.grpc.MonitorRequest.class,
      responseType = com.sentinel.grpc.SystemStats.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.sentinel.grpc.MonitorRequest,
      com.sentinel.grpc.SystemStats> getSubscribeToDashboardMethod() {
    io.grpc.MethodDescriptor<com.sentinel.grpc.MonitorRequest, com.sentinel.grpc.SystemStats> getSubscribeToDashboardMethod;
    if ((getSubscribeToDashboardMethod = SentinelServiceGrpc.getSubscribeToDashboardMethod) == null) {
      synchronized (SentinelServiceGrpc.class) {
        if ((getSubscribeToDashboardMethod = SentinelServiceGrpc.getSubscribeToDashboardMethod) == null) {
          SentinelServiceGrpc.getSubscribeToDashboardMethod = getSubscribeToDashboardMethod =
              io.grpc.MethodDescriptor.<com.sentinel.grpc.MonitorRequest, com.sentinel.grpc.SystemStats>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubscribeToDashboard"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.sentinel.grpc.MonitorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.sentinel.grpc.SystemStats.getDefaultInstance()))
              .setSchemaDescriptor(new SentinelServiceMethodDescriptorSupplier("SubscribeToDashboard"))
              .build();
        }
      }
    }
    return getSubscribeToDashboardMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SentinelServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SentinelServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SentinelServiceStub>() {
        @java.lang.Override
        public SentinelServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SentinelServiceStub(channel, callOptions);
        }
      };
    return SentinelServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SentinelServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SentinelServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SentinelServiceBlockingStub>() {
        @java.lang.Override
        public SentinelServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SentinelServiceBlockingStub(channel, callOptions);
        }
      };
    return SentinelServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SentinelServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SentinelServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SentinelServiceFutureStub>() {
        @java.lang.Override
        public SentinelServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SentinelServiceFutureStub(channel, callOptions);
        }
      };
    return SentinelServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Member 1 (Agent) calls this to stream data TO Member 2 (Server)
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.sentinel.grpc.SystemStats> streamStats(
        io.grpc.stub.StreamObserver<com.sentinel.grpc.Alert> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getStreamStatsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Member 4 (UI) calls this to receive live data FROM Member 2 (Server)
     * </pre>
     */
    default void subscribeToDashboard(com.sentinel.grpc.MonitorRequest request,
        io.grpc.stub.StreamObserver<com.sentinel.grpc.SystemStats> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubscribeToDashboardMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SentinelService.
   */
  public static abstract class SentinelServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SentinelServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SentinelService.
   */
  public static final class SentinelServiceStub
      extends io.grpc.stub.AbstractAsyncStub<SentinelServiceStub> {
    private SentinelServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SentinelServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SentinelServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Member 1 (Agent) calls this to stream data TO Member 2 (Server)
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.sentinel.grpc.SystemStats> streamStats(
        io.grpc.stub.StreamObserver<com.sentinel.grpc.Alert> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getStreamStatsMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Member 4 (UI) calls this to receive live data FROM Member 2 (Server)
     * </pre>
     */
    public void subscribeToDashboard(com.sentinel.grpc.MonitorRequest request,
        io.grpc.stub.StreamObserver<com.sentinel.grpc.SystemStats> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSubscribeToDashboardMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SentinelService.
   */
  public static final class SentinelServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SentinelServiceBlockingStub> {
    private SentinelServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SentinelServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SentinelServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Member 4 (UI) calls this to receive live data FROM Member 2 (Server)
     * </pre>
     */
    public java.util.Iterator<com.sentinel.grpc.SystemStats> subscribeToDashboard(
        com.sentinel.grpc.MonitorRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSubscribeToDashboardMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SentinelService.
   */
  public static final class SentinelServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<SentinelServiceFutureStub> {
    private SentinelServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SentinelServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SentinelServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SUBSCRIBE_TO_DASHBOARD = 0;
  private static final int METHODID_STREAM_STATS = 1;

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
        case METHODID_SUBSCRIBE_TO_DASHBOARD:
          serviceImpl.subscribeToDashboard((com.sentinel.grpc.MonitorRequest) request,
              (io.grpc.stub.StreamObserver<com.sentinel.grpc.SystemStats>) responseObserver);
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
        case METHODID_STREAM_STATS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamStats(
              (io.grpc.stub.StreamObserver<com.sentinel.grpc.Alert>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getStreamStatsMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              com.sentinel.grpc.SystemStats,
              com.sentinel.grpc.Alert>(
                service, METHODID_STREAM_STATS)))
        .addMethod(
          getSubscribeToDashboardMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.sentinel.grpc.MonitorRequest,
              com.sentinel.grpc.SystemStats>(
                service, METHODID_SUBSCRIBE_TO_DASHBOARD)))
        .build();
  }

  private static abstract class SentinelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SentinelServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.sentinel.grpc.MonitorProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SentinelService");
    }
  }

  private static final class SentinelServiceFileDescriptorSupplier
      extends SentinelServiceBaseDescriptorSupplier {
    SentinelServiceFileDescriptorSupplier() {}
  }

  private static final class SentinelServiceMethodDescriptorSupplier
      extends SentinelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    SentinelServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (SentinelServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SentinelServiceFileDescriptorSupplier())
              .addMethod(getStreamStatsMethod())
              .addMethod(getSubscribeToDashboardMethod())
              .build();
        }
      }
    }
    return result;
  }
}
