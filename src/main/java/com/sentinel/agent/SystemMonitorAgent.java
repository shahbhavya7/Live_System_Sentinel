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
        // 1. Dynamic Startup Arguments
        String serverIp = args.length > 0 ? args[0] : "localhost";
        String agentId  = args.length > 1 ? args[1] : "Agent-1";

        System.out.println("==================================================");
        System.out.println("  Live System Sentinel | Agent Node Booting...  ");
        System.out.println("  Agent ID:       " + agentId);
        System.out.println("  Target Server:  " + serverIp + ":9090");
        System.out.println("==================================================\n");

        // Cache system metric bindings
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // 2. High Availability Auto-Reconnect Infinite Loop
        while (true) {
            System.out.println("Connecting to network bridge...");
            
            ManagedChannel channel = null;
            ScheduledExecutorService executor = null;
            CountDownLatch finishLatch = new CountDownLatch(1);

            try {
                // Build communication matrix specifically for Target IP
                channel = ManagedChannelBuilder.forAddress(serverIp, 9090)
                        .usePlaintext()
                        .build();

                SentinelServiceGrpc.SentinelServiceStub stub = SentinelServiceGrpc.newStub(channel);

                // Receive stream configuring automated callback protocols
                StreamObserver<Alert> responseObserver = new StreamObserver<Alert>() {
                    @Override
                    public void onNext(Alert alert) {
                        System.out.println("\n[!] SECURITY THREAT TRIPPED [!]");
                        System.out.println("    Rank: [" + alert.getSeverity() + "]");
                        System.out.println("    Info: " + alert.getMessage() + "\n");
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("Connection matrix dropped: " + t.getMessage());
                        finishLatch.countDown(); // Snap latch to trigger reconnect
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Server requested clean termination.");
                        finishLatch.countDown();
                    }
                };

                // Initialize the bi-directional stream pipeline
                StreamObserver<SystemStats> requestObserver = stub.streamStats(responseObserver);

                // Spin up thread firing exactly every 1000ms
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(() -> {
                    try {
                        double cpuLoad = osBean.getCpuLoad();
                        double cpuPercent = Math.max(0, cpuLoad) * 100.0;
                        
                        long memoryTotalMb = osBean.getTotalMemorySize() / (1024 * 1024);
                        long memoryUsedMb = (osBean.getTotalMemorySize() - osBean.getFreeMemorySize()) / (1024 * 1024);
                        int activeThreads = threadBean.getThreadCount();
                        long timestamp = System.currentTimeMillis();

                        // Bind exact telemetry onto Protobuf architecture
                        SystemStats stats = SystemStats.newBuilder()
                                .setAgentId(agentId) // Using dynamically injected ID
                                .setCpuPercent(cpuPercent)
                                .setMemoryTotalMb(memoryTotalMb)
                                .setMemoryUsedMb(memoryUsedMb)
                                .setActiveThreads(activeThreads)
                                .setTimestamp(timestamp)
                                .build();

                        // Pipeline blast to server
                        requestObserver.onNext(stats);
                        System.out.printf("[%s] CPU: %05.2f%% | RAM: %4d MB | Thr: %3d%n",
                                agentId, cpuPercent, memoryUsedMb, activeThreads);

                    } catch (Exception e) {
                        System.err.println("Hardware metrics collection failed: " + e.getMessage());
                        requestObserver.onError(e);  // Trigger pipeline collapse natively
                        finishLatch.countDown();     // Escalate into reconnect scope
                    }
                }, 0, 1, TimeUnit.SECONDS);

                System.out.println("Link Established! Broadcasting telemetry stream natively...");

                // Suspend the main thread indefinitely until `finishLatch` clicks
                finishLatch.await();

            } catch (Exception e) {
                System.err.println("Fatal execution crash tracked: " + e.getMessage());
            } finally {
                // Garbage Collection and memory wipe before restarting pipeline
                if (executor != null) {
                    executor.shutdownNow();
                }
                if (channel != null && !channel.isShutdown()) {
                    channel.shutdownNow();
                }
                
                System.out.println("\n--- Pipeline Disconnected ---");
                System.out.println("Restoring heartbeat in 5 seconds...\n");
                
                // 3. Automated 5 second backup pacing
                Thread.sleep(5000);
            }
        }
    }
}
