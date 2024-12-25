package org.example;

import java.util.concurrent.Semaphore;
import java.time.Instant;

public abstract class Customer extends Thread {
    protected int customerID;
    protected String customerName;
    protected double budget;
    protected double totalSpent;
    protected CustomerType type;
    protected Instant orderStartTime;
    protected volatile double priorityScore;


    // Semaforlar
    protected static final Semaphore orderProcessingSemaphore = new Semaphore(1, true);
    protected static final Semaphore stockManagementSemaphore = new Semaphore(1, true);

    public enum CustomerType {
        PREMIUM(15.0),
        STANDARD(10.0);

        private final double basePriority;

        CustomerType(double basePriority) {
            this.basePriority = basePriority;
        }

        public double getBasePriority() {
            return basePriority;
        }
    }

    public Customer(int customerID, String customerName, double budget, CustomerType type) {
        this.customerID = customerID;
        this.customerName = customerName;
        this.budget = budget;
        this.type = type;
        this.totalSpent = 0.0;
        this.setName("Customer-" + customerID);
    }

    // 2. construct varolan müşteri için
    public Customer(int customerID, String customerName, double budget, CustomerType type, double totalSpent) {
        this.customerID = customerID;
        this.customerName = customerName;
        this.budget = budget;
        this.type = type;
        this.totalSpent = totalSpent;
        this.setName("Customer-" + customerID);
    }

    @Override
    public void run() {

        AdminPanel.getAdminLock().acquireUninterruptibly();
        AdminPanel.getAdminLock().release();
        try {
            while (!Thread.interrupted()) {
                orderStartTime = Instant.now();
                processOrder();
                Thread.sleep(1000); // Siparişler arası bekleme
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Customer thread interrupted: " + customerID);
        }
    }

    protected abstract void processOrder();

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public double calculatePriorityScore() {
        if (orderStartTime == null) {
            return type.getBasePriority();
        }
        // Bekleme hesaplama
        long waitingTimeSeconds = Instant.now().getEpochSecond() - orderStartTime.getEpochSecond();
        double waitingScoreMultiplier = 0.5;
        return type.getBasePriority() + (waitingTimeSeconds * waitingScoreMultiplier);
    }
    public void startOrder() {
        this.orderStartTime = Instant.now();
    }
    public void completeOrder() {
        this.orderStartTime = null;
    }



    // Getters ve Setters
    public int getCustomerID() { return customerID; }
    public String getCustomerName() { return customerName; }
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
    public double getTotalSpent() { return totalSpent; }
    public CustomerType getType() { return type; }
    public void setType(CustomerType type) { this.type = type; }
    public double getPriorityScore() { return priorityScore; }
}