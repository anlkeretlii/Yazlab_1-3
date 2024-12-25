package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger {
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile Logger instance;
    private static Thread loggerThread;
    private final DatabaseManager dbManager;

    private Logger() {
        this.dbManager = DatabaseManager.getInstance();
        startLoggingThread();
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    private void startLoggingThread() {
        loggerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    LogEntry entry = logQueue.take();

                    ColorLogger.ColorLogEntry colorEntry = ColorLogger.createLog(
                            entry.getType(),
                            entry.getMessage()
                    );

                    if (MainGUI.getInstance() != null) {
                        MainGUI.getInstance().updateLogPanel(colorEntry);
                    }

                    Log dbLog = new Log(
                            DatabaseManager.getInstance().generateNextLogId(),
                            entry.getCustomerId(),
                            entry.getOrderId(),
                            entry.getType(),
                            entry.getMessage()
                    );
                    dbManager.saveLog(dbLog);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Loglama hatası: " + e.getMessage());
                }
            }
        });
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    public static void log(LogType type, String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), type, null, null, message);
        logQueue.offer(entry);
    }

    public static void log(LogType type, int customerId, String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), type, customerId, null, message);
        logQueue.offer(entry);
    }

    public static void log(LogType type, OrderManager.Order order, String message) {
        LogEntry entry = new LogEntry(
                LocalDateTime.now(),
                type,
                order.getCustomerID(),
                order.getOrderId(),
                message
        );
        logQueue.offer(entry);
    }

    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final LogType type;
        private final Integer customerId;
        private final Integer orderId;
        private final String message;

        public LogEntry(LocalDateTime timestamp, LogType type, Integer customerId, Integer orderId, String message) {
            this.timestamp = timestamp;
            this.type = type;
            this.customerId = customerId;
            this.orderId = orderId;
            this.message = message;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public LogType getType() { return type; }
        public Integer getCustomerId() { return customerId; }
        public Integer getOrderId() { return orderId; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%s] %s", timestamp.format(formatter), type));

            if (customerId != null) {
                sb.append(String.format(" - Müşteri[%d]", customerId));
            }
            if (orderId != null) {
                sb.append(String.format(" - Sipariş[%d]", orderId));
            }

            sb.append(": ").append(message);
            return sb.toString();
        }
    }

    public void shutdown() {
        try {
            loggerThread.interrupt();
            loggerThread.join(1000);
        } catch (InterruptedException e) {
            System.err.println("Logger kapatma hatası: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}