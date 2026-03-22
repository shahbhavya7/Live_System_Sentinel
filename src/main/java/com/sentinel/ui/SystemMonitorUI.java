package com.sentinel.ui;

import com.sentinel.grpc.MonitorRequest;
import com.sentinel.grpc.SentinelServiceGrpc;
import com.sentinel.grpc.SystemStats;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class SystemMonitorUI extends Application {

    private ManagedChannel channel;
    private int timeCounter = 0; // Simple integer counter for the X-axis time representation

    @Override
    public void start(Stage primaryStage) {
        // 1. Setup JavaFX Chart Axes
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (Ticks)");
        // By disabling forceZeroInRange, the X-axis glides nicely over time
        xAxis.setForceZeroInRange(false);

        // Limit the Y-axis to strictly visually represent percentage bounds 0 - 100
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("CPU Usage (%)");

        // Create the line chart, disable animations to avoid staggered redraws as points append quickly
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Live System CPU Monitor");
        lineChart.setAnimated(false); 

        // 2. Setup Data Container and inject into Chart
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("CPU Over Time");
        lineChart.getData().add(series);

        // Lock Scene constraints
        Scene scene = new Scene(lineChart, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("System Sentinel Stream UI");
        primaryStage.show();

        // 3. Initialize gRPC asynchronous Managed Channel to the local telemetry Server
        channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        SentinelServiceGrpc.SentinelServiceStub stub = SentinelServiceGrpc.newStub(channel);

        // 4. Connect to Dashboard Stream bridging directly back from Server Thread
        MonitorRequest request = MonitorRequest.newBuilder()
                .setAgentId("UI-1")
                .build();

        stub.subscribeToDashboard(request, new StreamObserver<SystemStats>() {
            @Override
            public void onNext(SystemStats stats) {
                
                // 5. Must offload internal chart data mutations straight to the JavaFX Main Event Dispatch Thread
                Platform.runLater(() -> {
                    // Feed the new X/Y struct into Series
                    series.getData().add(new XYChart.Data<>(timeCounter++, stats.getCpuPercent()));

                    // Guarantee scrolling buffer limit mapping prevents infinite memory leaks
                    if (series.getData().size() > 60) {
                        series.getData().remove(0);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Dashboard socket dropped: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Connection halted cleanly by Server.");
            }
        });
    }

    @Override
    public void stop() throws Exception {
        // Securely rip cord connection if X box is closed
        if (channel != null && !channel.isShutdown()) {
            System.out.println("Stopping UI stream. Exiting gracefully.");
            channel.shutdownNow();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
