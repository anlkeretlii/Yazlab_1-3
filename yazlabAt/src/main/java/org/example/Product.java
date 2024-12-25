package org.example;
import java.util.concurrent.atomic.AtomicInteger;

public class Product {
    private int productID;
    private String productName;
    private AtomicInteger stock;  // Thread-safe stock yönetimi için
    private double price;

    public Product(int productID, String productName, int stock, double price) {
        this.productID = productID;
        this.productName = productName;
        this.stock = new AtomicInteger(stock);
        this.price = price;
    }

    public boolean decreaseStock(int amount) {
        while (true) {
            int currentStock = stock.get();
            if (currentStock < amount) {
                return false;
            }
            if (stock.compareAndSet(currentStock, currentStock - amount)) {
                return true;
            }
        }
    }

    public void increaseStock(int amount) {
        stock.addAndGet(amount);
    }

    // Getters
    public int getProductID() { return productID; }
    public String getProductName() { return productName; }
    public int getStock() { return stock.get(); }
    public double getPrice() { return price; }

    // Setters
    public void setPrice(double price) { this.price = price; }
}