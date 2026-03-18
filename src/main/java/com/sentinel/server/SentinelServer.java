package com.sentinel.server;

import com.sentinel.grpc.Alert;
import com.sentinel.grpc.SentinelServiceGrpc;
import com.sentinel.grpc.SystemStats;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class SentinelServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 1. Setup the gRPC server to listen on port 9090
        Server server = ServerBuilder.forPort(9090)
                .addService(new SentinelServiceImpl())
                .build();

        System.out.println("Starting SentinelServer on port 9090...");
        server.start();
        System.out.println("SentinelServer is up and listening for telemetry!");

        // 2. Keep the server running
        server.awaitTermination();
    }

    // 3. Service Implementation extending the generated base class
    static class SentinelServiceImpl extends SentinelServiceGrpc.SentinelServiceImplBase {

        @Override
        public StreamObserver<SystemStats> streamStats(StreamObserver<Alert> responseObserver) {
            
            // Return an observer that processes incoming SystemStats messages stream
            return new StreamObserver<SystemStats>() {

                @Override
                public void onNext(SystemStats stats) {
                    // Print received telemetry nicely formatted
                    System.out.printf(
                            "[Telemetry Rx] CPU: %.2f%% | RAM: %d / %d MB | Active Threads: %d%n",
                            stats.getCpuPercent(),
                            stats.getMemoryUsedMb(),
                            stats.getMemoryTotalMb(),
                            stats.getActiveThreads()
                    );

                    // Execute Simple Threat Detection Rule
                    if (stats.getCpuPercent() > 90.0) {
                        System.out.println("   => Threat Detected! Sending CRITICAL Alert to Agent.");
                        
                        Alert alert = Alert.newBuilder()
                                .setSeverity("CRITICAL")
                                .setMessage("CPU Spike Detected!")
                                .setTimestamp(System.currentTimeMillis())
                                .build();
                                
                        responseObserver.onNext(alert);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Stream error from agent: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Agent closed its data stream.");
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
