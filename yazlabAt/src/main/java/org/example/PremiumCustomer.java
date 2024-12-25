package org.example;


public class PremiumCustomer extends Customer {

    public PremiumCustomer(int customerID, String customerName, double budget) {
        super(customerID, customerName, budget, CustomerType.PREMIUM);
    }

    // Yeni constructor
    public PremiumCustomer(int customerID, String customerName, double budget, double totalSpent) {
        super(customerID, customerName, budget, CustomerType.PREMIUM, totalSpent);
    }

    @Override
    protected void processOrder() {
        try {
            orderProcessingSemaphore.acquire();
            try {
                Logger.log(LogType.INFO, "Premium customer " + customerID + " processing order");

                Thread.sleep(2000);

                priorityScore = calculatePriorityScore();

            } finally {
                orderProcessingSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Premium customer order processing interrupted: " + customerID);
        }
    }
}