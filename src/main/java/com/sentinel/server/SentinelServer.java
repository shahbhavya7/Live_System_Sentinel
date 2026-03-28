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
                if ("POST".equals(exchange.getRequestMethod())) {
                    try {
                        // 1. Read the JSON package
                        InputStream is = exchange.getRequestBody();
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject payload = JsonParser.parseString(body).getAsJsonObject();

                        String agentId = payload.has("agent_id") ? payload.get("agent_id").getAsString() : "Unknown-Node";

                        // 2. BACKWARDS COMPATIBILITY ARMOR: 
                        // If an older agent connects, inject 0.0 for the missing fields so it doesn't crash!
                        if (!payload.has("disk_percent") || payload.get("disk_percent").isJsonNull()) payload.addProperty("disk_percent", 0.0);
                        if (!payload.has("network_rx_mb") || payload.get("network_rx_mb").isJsonNull()) payload.addProperty("network_rx_mb", 0.0);
                        if (!payload.has("network_tx_mb") || payload.get("network_tx_mb").isJsonNull()) payload.addProperty("network_tx_mb", 0.0);
                        if (!payload.has("uptime_hours") || payload.get("uptime_hours").isJsonNull()) payload.addProperty("uptime_hours", 0.0);

                        // 3. Print the successful extraction
                        System.out.printf("[REST Webhook: %s] CPU: %s%% | RAM: %sMB | Disk: %s%% | Rx: %sMB | Tx: %sMB | Up: %sh%n", 
                                agentId, payload.get("cpu"), payload.get("ram"), payload.get("disk_percent"), 
                                payload.get("network_rx_mb"), payload.get("network_tx_mb"), payload.get("uptime_hours"));

                        // 4. Wire it to the Web Dashboard
                        payload.addProperty("type", "telemetry"); 
                        if (webSocketServer != null) {
                            webSocketServer.broadcast(payload.toString());
                        }

                        // --- Execute SIEM Threat Detection (HTTP Pipeline) ---
                        int threatScore = 0;
                        StringBuilder reasons = new StringBuilder();

                        double cpu = payload.has("cpu") && !payload.get("cpu").isJsonNull() ? payload.get("cpu").getAsDouble() : 0.0;
                        int threads = payload.has("threads") && !payload.get("threads").isJsonNull() ? payload.get("threads").getAsInt() : 0;
                        double disk = payload.get("disk_percent").getAsDouble();
                        double rx = payload.get("network_rx_mb").getAsDouble();
                        double tx = payload.get("network_tx_mb").getAsDouble();

                        if (cpu > 95.0) { threatScore += 50; reasons.append("CPU Critical; "); }
                        else if (cpu > 80.0) { threatScore += 20; reasons.append("CPU High; "); }

                        // 2. Analyze Threads (Calibrated for Windows OS)
                        if (threads > 8000) { threatScore += 20; reasons.append("Thread anomaly. "); }
                        if (threads > 12000) { threatScore += 30; reasons.append("Possible Fork Bomb. "); }

                        if (disk > 90.0) { threatScore += 50; reasons.append("Storage Near Crash; "); }

                        if (rx > 500.0 || tx > 500.0) { threatScore += 30; reasons.append("Network Surge; "); }

                        if (threatScore >= 50) {
                            String severity = threatScore >= 75 ? "CRITICAL" : "WARNING";
                            String msg = String.format("Score: %d | Reasons: %s", threatScore, reasons.toString().trim());

                            System.out.println("[SIEM TRIGGER] Agent: " + agentId + " | Level: " + severity + " | " + msg);

                            JsonObject alertJson = new JsonObject();
                            alertJson.addProperty("type", "alert");
                            alertJson.addProperty("agent_id", agentId);
                            alertJson.addProperty("severity", severity);
                            alertJson.addProperty("message", msg);

                            if (webSocketServer != null) {
                                webSocketServer.broadcast(alertJson.toString());
                            }
                        }

                        // 5. Send "Thumbs Up" to the device
                        String response = "Data Accepted by Sentinel";
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();

                    } catch (Exception e) {
                        System.err.println("Webhook parsing pipeline crash: " + e.getMessage());
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
                    
                    if (lastTime != null && (currentTime - lastTime < 500)) return; 
                    
                    lastSeen.put(agentId, currentTime);

                    // Print received telemetry elegantly partitioned
                    System.out.printf(
                            "[gRPC Rx: %s] | CPU: %05.2f%% | RAM: %4d / %4d MB | Threads: %3d%n",
                            agentId, stats.getCpuPercent(), stats.getMemoryUsedMb(), stats.getMemoryTotalMb(), stats.getActiveThreads()
                    );

                    // --- WebSocket Broadcasting (Web UI) ---
                    JsonObject telemetryJson = new JsonObject();
                    telemetryJson.addProperty("type", "telemetry");
                    telemetryJson.addProperty("agent_id", agentId);
                    telemetryJson.addProperty("cpu", stats.getCpuPercent());
                    telemetryJson.addProperty("ram", stats.getMemoryUsedMb());
                    telemetryJson.addProperty("threads", stats.getActiveThreads());
                    telemetryJson.addProperty("disk_percent", stats.getDiskUsedPercent());
                    telemetryJson.addProperty("network_rx_mb", stats.getNetworkRxMb());
                    telemetryJson.addProperty("network_tx_mb", stats.getNetworkTxMb());
                    telemetryJson.addProperty("uptime_hours", stats.getUptimeHours());

                    if (webSocketServer != null) {
                        webSocketServer.broadcast(telemetryJson.toString());
                    }

                    // --- Execute Deep Threat Detection Rule: Weighted Scoring Algorithm ---
                    int threatScore = 0;
                    StringBuilder reasons = new StringBuilder();

                    // CPU Heuristic
                    if (stats.getCpuPercent() > 95.0) {
                        threatScore += 40;
                        reasons.append("CPU Critical; ");
                    } else if (stats.getCpuPercent() > 80.0) {
                        threatScore += 20;
                        reasons.append("CPU High; ");
                    }

                    // RAM Heuristic
                    double ramPercent = 0.0;
                    if (stats.getMemoryTotalMb() > 0) {
                        ramPercent = ((double) stats.getMemoryUsedMb() / stats.getMemoryTotalMb()) * 100.0;
                    }
                    if (ramPercent > 95.0) {
                        threatScore += 40;
                        reasons.append("RAM Critical; ");
                    } else if (ramPercent > 85.0) {
                        threatScore += 20;
                        reasons.append("RAM High; ");
                    }

                    // Storage Heuristic
                    if (stats.getDiskUsedPercent() > 90.0) {
                        threatScore += 50;
                        reasons.append("Disk Near Crash; ");
                    }

                    // Network Surge (DDoS / Exfiltration) Heuristic
                    if (stats.getNetworkRxMb() > 500 || stats.getNetworkTxMb() > 500) {
                        threatScore += 30;
                        reasons.append("Network Surge; ");
                    }

                    // Thread Fork Bomb Heuristic
                    if (stats.getActiveThreads() > 1500) {
                        threatScore += 30;
                        reasons.append("Possible Fork Bomb; ");
                    }

                    // Dispatch AI Severity Event
                    if (threatScore >= 50) {
                        String severity = threatScore >= 75 ? "CRITICAL" : "WARNING";
                        String finalMessage = String.format("Score: %d | Reasons: %s", threatScore, reasons.toString().trim());

                        System.out.println("   => Threat Detected! Flagging " + agentId + " [Level: " + severity + "]");
                        
                        // Push Alert linearly back into gRPC loop for Java Agents
                        Alert alert = Alert.newBuilder()
                                .setSeverity(severity)
                                .setMessage(finalMessage)
                                .setTimestamp(currentTime)
                                .build();
                                
                        responseObserver.onNext(alert);

                        // Global WebSockets Dispatch Event
                        JsonObject alertJson = new JsonObject();
                        alertJson.addProperty("type", "alert");
                        alertJson.addProperty("agent_id", agentId);
                        alertJson.addProperty("severity", severity);
                        alertJson.addProperty("message", finalMessage);
                        
                        if (webSocketServer != null) {
                            webSocketServer.broadcast(alertJson.toString());
                        }
                    }

                    // Flush valid secure arrays downward over standard JavaFX connections
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