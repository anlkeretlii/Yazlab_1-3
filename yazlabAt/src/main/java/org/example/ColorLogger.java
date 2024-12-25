package org.example;


import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ColorLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static class ColorLogEntry {
        private final String message;
        private final Color color;
        private final LogType type;
        private final LocalDateTime timestamp;

        public ColorLogEntry(String message, Color color, LogType type) {
            this.message = message;
            this.color = color;
            this.type = type;
            this.timestamp = LocalDateTime.now();
        }
        public String getMessage() { return message; }
        public Color getColor() { return color; }
        public LogType getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s",
                    timestamp.format(formatter),
                    type,
                    message);
        }
    }
    public static ColorLogEntry createLog(LogType type, String message) {
        return new ColorLogEntry(
                translateLogMessage(message),
                getColorForLogType(type),
                type
        );
    }
    private static Color getColorForLogType(LogType type) {
        return switch (type) {
            case INFO -> Color.GREEN;
            case WARNING -> Color.ORANGE;
            case ERROR -> Color.RED;
            default -> Color.BLACK;
        };
    }

    private static String translateLogMessage(String message) {
        message = message.replace("Customer", "Müşteri")
                .replace("Product", "Ürün")
                .replace("Order", "Sipariş")
                .replace("Stock", "Stok")
                .replace("Error", "Hata")
                .replace("Warning", "Uyarı")
                .replace("Info", "Bilgi")
                .replace("added", "eklendi")
                .replace("updated", "güncellendi")
                .replace("removed", "kaldırıldı")
                .replace("processing", "işleniyor")
                .replace("completed", "tamamlandı")
                .replace("failed", "başarısız")
                .replace("Low stock", "Düşük stok")
                .replace("items remaining", "adet kaldı")
                .replace("New", "Yeni")
                .replace("Type", "Tip")
                .replace("ID", "NO")
                .replace("Price", "Fiyat")
                .replace("Budget", "Bütçe")
                .replace("PREMIUM", "ÖNCELİKLİ")
                .replace("STANDARD", "STANDART");

        return message;
    }
}
