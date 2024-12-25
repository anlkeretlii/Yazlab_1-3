package org.example;

public class StandardCustomer extends Customer {
    public StandardCustomer(int customerID, String customerName, double budget) {
        super(customerID, customerName, budget, CustomerType.STANDARD);
    }

    // Yeni constructor ekle
    public StandardCustomer(int customerID, String customerName, double budget, double totalSpent) {
        super(customerID, customerName, budget, CustomerType.STANDARD, totalSpent);
    }

    @Override
    protected void processOrder() {
        try {
            while (hasPremiumCustomersInQueue()) {
                Thread.sleep(500);
            }

            orderProcessingSemaphore.acquire();
            try {
                Logger.log(LogType.INFO, "Standard customer " + customerID + " processing order");
                Thread.sleep(2000);
                priorityScore = calculatePriorityScore();

            } finally {
                orderProcessingSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Standard customer order processing interrupted: " + customerID);
        }
    }

    private boolean hasPremiumCustomersInQueue() {
        return OrderManager.getInstance().hasPremiumCustomersWaiting();
    }
}
