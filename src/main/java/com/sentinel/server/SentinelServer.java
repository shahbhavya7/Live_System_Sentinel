package com.sentinel.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sentinel.grpc.Alert;
import com.sentinel.grpc.MonitorRequest;
import com.sentinel.grpc.SentinelServiceGrpc;
import com.sentinel.grpc.SystemStats;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SentinelServer {

    // Thread-safe set of connected UI dashboards (JavaFX)
    private static final Set<StreamObserver<SystemStats>> dashboardClients = ConcurrentHashMap.newKeySet();
    
    // Static WebSocketServer for broadcasting Web UI telemetry
    private static WebSocketServer webSocketServer;

    // Rate Limiter cache holding last seen timestamps per isolated Agent Network ID
    private static final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        
        // 1. Setup the WebSocketServer to listen on port 8081 (for Web Browser clients)
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
                // Empty override
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

        // 2. Setup REST API Server on port 8080 for HTTP Webhooks
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/api/stats", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        // Read and Parse incoming JSON Payload natively via standard charsets
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
                        
                        String agentId = jsonObject.has("agent_id") ? jsonObject.get("agent_id").getAsString() : "Unknown";
                        double cpu = jsonObject.has("cpu") ? jsonObject.get("cpu").getAsDouble() : 0.0;
                        long ram = jsonObject.has("ram") ? jsonObject.get("ram").getAsLong() : 0;
                        int threads = jsonObject.has("threads") ? jsonObject.get("threads").getAsInt() : 0;

                        // Print webhook receipt onto standard deployment terminal logs
                        System.out.printf("[Webhook Rx] Agent %s | CPU: %.2f%% | RAM: %d MB | Thr: %d%n",
                                agentId, cpu, ram, threads);

                        // Enforce specific UI routing constraint 
                        jsonObject.addProperty("type", "telemetry");

                        // Bridge entirely separate network paradigms by pushing HTTP directly to Websockets
                        if (webSocketServer != null) {
                            webSocketServer.broadcast(jsonObject.toString());
                        }

                        // Flush standard 200 HTTP acknowledgment
                        String responseText = "Data Accepted";
                        exchange.sendResponseHeaders(200, responseText.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(responseText.getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        System.err.println("Webhook parsing pipeline crash: " + e.getMessage());
                        String errorText = "Internal Server Error";
                        exchange.sendResponseHeaders(500, errorText.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(errorText.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                } else {
                    String methodNotAllowed = "Method Not Allowed";
                    exchange.sendResponseHeaders(405, methodNotAllowed.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(methodNotAllowed.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        });
        
        System.out.println("Starting REST HTTP Server on port 8080...");
        httpServer.setExecutor(null);
        httpServer.start();

        // 3. Setup the gRPC server to listen on port 9090
        Server grpcServer = ServerBuilder.forPort(9090)
                .addService(new SentinelServiceImpl())
                .build();

        System.out.println("Starting gRPC SentinelServer on port 9090...");
        grpcServer.start();
        System.out.println("gRPC SentinelServer is up and listening for telemetry!");

        // 4. Keep the server running
        grpcServer.awaitTermination();
    }

    // 5. Service Implementation extending the generated base class
    static class SentinelServiceImpl extends SentinelServiceGrpc.SentinelServiceImplBase {

        @Override
        public void subscribeToDashboard(MonitorRequest request, StreamObserver<SystemStats> responseObserver) {
            dashboardClients.add(responseObserver);
        }

        @Override
        public StreamObserver<SystemStats> streamStats(StreamObserver<Alert> responseObserver) {
            
            // Return an observer that processes incoming SystemStats messages stream
            return new StreamObserver<SystemStats>() {

                @Override
                public void onNext(SystemStats stats) {
                    
                    String agentId = stats.getAgentId();
                    
                    // --- Hardware Rate Limiter Firewall (500ms max speed) ---
                    Long lastTime = lastSeen.get(agentId);
                    long currentTime = System.currentTimeMillis();
                    
                    // Securely verify timestamp maps, explicitly drop ghost/flooding packets 
                    // attempting to DDoS buffer arrays faster than half a second interval limit.
                    if (lastTime != null && (currentTime - lastTime < 500)) {
                        return; 
                    }
                    
                    // Overwrite state array unlocking 1 processing pipeline logic
                    lastSeen.put(agentId, currentTime);

                    // Print received telemetry elegantly partitioned
                    System.out.printf(
                            "[Agent: %s] | CPU: %05.2f%% | RAM: %4d / %4d MB | Threads: %3d%n",
                            agentId,
                            stats.getCpuPercent(),
                            stats.getMemoryUsedMb(),
                            stats.getMemoryTotalMb(),
                            stats.getActiveThreads()
                    );

                    // --- WebSocket Broadcasting (Web UI) ---
                    JsonObject telemetryJson = new JsonObject();
                    telemetryJson.addProperty("type", "telemetry");
                    telemetryJson.addProperty("agent_id", agentId);
                    telemetryJson.addProperty("cpu", stats.getCpuPercent());
                    telemetryJson.addProperty("ram", stats.getMemoryUsedMb());
                    telemetryJson.addProperty("threads", stats.getActiveThreads());

                    if (webSocketServer != null) {
                        webSocketServer.broadcast(telemetryJson.toString());
                    }

                    // --- Execute Deep Threat Detection Rule ---
                    if (stats.getCpuPercent() > 90.0) {
                        System.out.println("   => Threat Detected! Flagging " + agentId + ".");
                        
                        // Drop an Alert inside the strict Agent-Mapped gRPC Thread Pipeline
                        Alert alert = Alert.newBuilder()
                                .setSeverity("CRITICAL")
                                .setMessage("CPU Spike Detected on " + agentId + "!")
                                .setTimestamp(currentTime)
                                .build();
                                
                        responseObserver.onNext(alert);

                        // Broadcast Alert independently across Web UI Sockets Global Broadcaster
                        JsonObject alertJson = new JsonObject();
                        alertJson.addProperty("type", "alert");
                        alertJson.addProperty("agent_id", agentId);
                        alertJson.addProperty("severity", "CRITICAL");
                        alertJson.addProperty("message", "CPU Spike! Core usage breached " + String.format("%.1f", stats.getCpuPercent()) + "%");
                        
                        if (webSocketServer != null) {
                            webSocketServer.broadcast(alertJson.toString());
                        }
                    }

                    // Flush valid secure array structs back over standard JavaFX connections
                    for (StreamObserver<SystemStats> client : dashboardClients) {
                        try {
                            client.onNext(stats);
                        } catch (Exception e) {
                            dashboardClients.remove(client);
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