package com.sentinel.agent;

import com.sentinel.grpc.Alert;
import com.sentinel.grpc.SentinelServiceGrpc;
import com.sentinel.grpc.SystemStats;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

public class SystemMonitorAgent {

    public static void main(String[] args) throws InterruptedException {
        // 1. Create a gRPC channel to localhost:9090 without TLS
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        // 2. Create an asynchronous stub
        SentinelServiceGrpc.SentinelServiceStub stub = SentinelServiceGrpc.newStub(channel);

        // 3. Keep application running
        CountDownLatch finishLatch = new CountDownLatch(1);

        // 4. Set up the response observer to receive Alerts from the server
        StreamObserver<Alert> responseObserver = new StreamObserver<Alert>() {
            @Override
            public void onNext(Alert alert) {
                System.out.println("!! ALERT RECEIVED !! [" + alert.getSeverity() + "] " + alert.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Stream error from server: " + t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Server closed the stream.");
                finishLatch.countDown();
            }
        };

        // 5. Call streamStats to get the requestObserver to send out data
        StreamObserver<SystemStats> requestObserver = stub.streamStats(responseObserver);

        // 6. Access system metrics MXBeans
        // Casting java.lang.management.OperatingSystemMXBean to com.sun.management.OperatingSystemMXBean for advanced metrics
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // 7. Schedule periodic telemetry extraction
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                // Get CPU load (returns a double between 0.0 and 1.0)
                double cpuLoad = osBean.getCpuLoad();
                double cpuPercent = Math.max(0, cpuLoad) * 100.0;

                // Grab memory using Java 14+ methods on com.sun.management.OperatingSystemMXBean
                long memoryTotalMb = osBean.getTotalMemorySize() / (1024 * 1024);
                long memoryUsedMb = (osBean.getTotalMemorySize() - osBean.getFreeMemorySize()) / (1024 * 1024);
                
                int activeThreads = threadBean.getThreadCount();
                long timestamp = System.currentTimeMillis();

                // 8. Build protobuf message
                SystemStats stats = SystemStats.newBuilder()
                        .setCpuPercent(cpuPercent)
                        .setMemoryTotalMb(memoryTotalMb)
                        .setMemoryUsedMb(memoryUsedMb)
                        .setActiveThreads(activeThreads)
                        .setTimestamp(timestamp)
                        .build();

                // 9. Send to Server via gRPC Stream
                requestObserver.onNext(stats);
                System.out.printf("Sent Stats -> CPU: %.2f%%, Mem Used: %d MB, Threads: %d%n",
                        cpuPercent, memoryUsedMb, activeThreads);

            } catch (Exception e) {
                System.err.println("Metric extraction failed: " + e.getMessage());
                requestObserver.onError(e);
                finishLatch.countDown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        System.out.println("SystemMonitorAgent started. Pushing telemetry to localhost:9090...");
        
        // Wait until connection closes
        finishLatch.await();

        // Graceful shutdown after disconnecting
        executor.shutdown();
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
