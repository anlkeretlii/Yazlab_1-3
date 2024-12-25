package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class StockVisualizationPanel extends VBox {
    private BarChart<String, Number> barChart;
    private XYChart.Series<String, Number> series;
    private Timeline updateTimeline;
    private Button threadControlButton;
    private Label alertLabel;
    private volatile boolean isRunning = true;

    public StockVisualizationPanel() {
        setPadding(new Insets(10));
        setSpacing(10);

        Label title = new Label("Stok Durumu");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        setupBarChart();

        alertLabel = new Label();
        alertLabel.setWrapText(true);
        alertLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        threadControlButton = new Button("Thread'leri Durdur");
        threadControlButton.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white;");
        threadControlButton.setOnAction(e -> toggleThreads());

        getChildren().addAll(title, barChart, alertLabel, threadControlButton);
        startAutoUpdate();
    }

    private void setupBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Ürünler");
        yAxis.setLabel("Stok Miktarı");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(false);
        barChart.setPrefHeight(300);
        barChart.setLegendVisible(false);

        series = new XYChart.Series<>();
        barChart.getData().add(series);
    }

    private void startAutoUpdate() {
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateStockData()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    private void updateStockData() {
        if (!isRunning) return;

        Platform.runLater(() -> {
            series.getData().clear();
            StringBuilder alertText = new StringBuilder();

            List<Product> products = StockManager.getInstance().getAllProducts();
            for (Product product : products) {
                XYChart.Data<String, Number> data = new XYChart.Data<>(product.getProductName(), product.getStock());
                series.getData().add(data);

                // Stok düşükse kırmızı, değilse yeşil renk ayarlaması
                Node bar = data.getNode();
                if (bar != null) {
                    if (product.getStock() < 10) {
                        bar.setStyle("-fx-bar-fill: #ff4d4f;");
                        alertText.append(product.getProductName())
                                .append(": ")
                                .append(product.getStock())
                                .append(" adet kaldı\n");
                    } else {
                        bar.setStyle("-fx-bar-fill: #4caf50;");
                    }
                }
            }
            // Uyarı metnini güncelle
            if (alertText.length() > 0) {
                alertLabel.setText("⚠️ Düşük Stok Uyarısı!\n" + alertText.toString());
                alertLabel.setVisible(true);
            } else {
                alertLabel.setVisible(false);
            }
        });
    }

    private void toggleThreads() {
        isRunning = !isRunning;
        if (isRunning) {
            threadControlButton.setText("Thread'leri Durdur");
            threadControlButton.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white;");
            ThreadManager.getInstance().resumeAll();
        } else {
            threadControlButton.setText("Thread'leri Başlat");
            threadControlButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
            ThreadManager.getInstance().pauseAll();
        }
    }

    public void shutdown() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
    }
}