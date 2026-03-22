package com.sentinel.server;

import com.google.gson.JsonObject;
import com.sentinel.grpc.Alert;
import com.sentinel.grpc.MonitorRequest;
import com.sentinel.grpc.SentinelServiceGrpc;
import com.sentinel.grpc.SystemStats;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SentinelServer {

    private static final Set<StreamObserver<SystemStats>> dashboardClients = ConcurrentHashMap.newKeySet();
    private static WebSocketServer webSocketServer;

    public static void main(String[] args) throws IOException, InterruptedException {

        // 1. Setup the WebSocketServer on port 8081
        webSocketServer = new WebSocketServer(new InetSocketAddress(8081)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                System.out.println("Web UI connected: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                System.out.println("Web UI disconnected.");
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                System.err.println("WS Error: " + ex.getMessage());
            }

            @Override
            public void onStart() {
                System.out.println("WebSocket Server started successfully on port 8081!");
            }
        };

        System.out.println("Starting WebSocket Server on port 8081...");
        webSocketServer.start();

        // 2. Setup the gRPC server on port 9090
        Server grpcServer = ServerBuilder.forPort(9090)
                .addService(new SentinelServiceImpl())
                .build();

        System.out.println("Starting gRPC SentinelServer on port 9090...");
        grpcServer.start();
        System.out.println("gRPC SentinelServer is up and listening for telemetry!");

        grpcServer.awaitTermination();
    }

    static class SentinelServiceImpl extends SentinelServiceGrpc.SentinelServiceImplBase {

        @Override
        public void subscribeToDashboard(MonitorRequest request, StreamObserver<SystemStats> responseObserver) {
            dashboardClients.add(responseObserver);
        }

        @Override
        public StreamObserver<SystemStats> streamStats(StreamObserver<Alert> responseObserver) {
            return new StreamObserver<SystemStats>() {
                @Override
                public void onNext(SystemStats stats) {
                    System.out.printf("[Telemetry Rx] CPU: %.2f%% | RAM: %d / %d MB | Threads: %d%n",
                            stats.getCpuPercent(), stats.getMemoryUsedMb(), stats.getMemoryTotalMb(),
                            stats.getActiveThreads());

                    // --- WebSocket Broadcasting ---
                    JsonObject telemetryJson = new JsonObject();
                    telemetryJson.addProperty("type", "telemetry");
                    telemetryJson.addProperty("cpu", stats.getCpuPercent());
                    telemetryJson.addProperty("ram", stats.getMemoryUsedMb());
                    telemetryJson.addProperty("threads", stats.getActiveThreads());

                    if (webSocketServer != null) {
                        webSocketServer.broadcast(telemetryJson.toString());
                    }

                    // --- Threat Detection Rule ---
                    if (stats.getCpuPercent() > 90.0) {
                        System.out.println("   => Threat Detected! Sending CRITICAL Alert.");

                        Alert alert = Alert.newBuilder()
                                .setSeverity("CRITICAL")
                                .setMessage("CPU Spike Detected!")
                                .setTimestamp(System.currentTimeMillis())
                                .build();
                        responseObserver.onNext(alert);

                        JsonObject alertJson = new JsonObject();
                        alertJson.addProperty("type", "alert");
                        alertJson.addProperty("severity", "CRITICAL");
                        alertJson.addProperty("message",
                                "CPU Spike! Usage at " + String.format("%.1f", stats.getCpuPercent()) + "%");

                        if (webSocketServer != null) {
                            webSocketServer.broadcast(alertJson.toString());
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}