package org.example;


import java.time.LocalDateTime;

public class Log {
    private int logId;
    private Integer customerID;  // Nullable
    private Integer orderID;     // Nullable
    private LocalDateTime logDate;
    private LogType logType;
    private String logDetails;

    public Log(int logId, Integer customerID, Integer orderID, LogType logType, String logDetails) {
        this.logId = logId;
        this.customerID = customerID;
        this.orderID = orderID;
        this.logDate = LocalDateTime.now();
        this.logType = logType;
        this.logDetails = logDetails;
    }

    // Getters
    public int getLogId() { return logId; }
    public Integer getCustomerID() { return customerID; }
    public Integer getOrderID() { return orderID; }
    public LocalDateTime getLogDate() { return logDate; }
    public LogType getLogType() { return logType; }
    public String getLogDetails() { return logDetails; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", logDate, logType, logDetails);
    }
}

enum LogType {
    INFO("Bilgilendirme"),
    WARNING("Uyarı"),
    ERROR("Hata");

    private final String description;

    LogType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
