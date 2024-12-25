package org.example;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import javafx.application.Platform;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseManager {
    private static volatile DatabaseManager instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private final AtomicInteger customerIdCounter;
    private final AtomicInteger productIdCounter;
    private static final String DATABASE_NAME = "karpuzdomates";
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private final AtomicInteger orderIdCounter;
    private final AtomicInteger logIdCounter;


    private DatabaseManager() {
        try {
            // MongoDB bağlantı ayarları
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(CONNECTION_STRING))
                    .serverApi(ServerApi.builder()
                            .version(ServerApiVersion.V1)
                            .build())
                    .build();

            mongoClient = MongoClients.create(settings);

            mongoClient.getDatabase(DATABASE_NAME).drop();

            database = mongoClient.getDatabase(DATABASE_NAME);

            createCollections();

            // Sayaçları başlat
            customerIdCounter = new AtomicInteger(1);
            productIdCounter = new AtomicInteger(1);
            orderIdCounter = new AtomicInteger(1);
            logIdCounter = new AtomicInteger(1);

            Logger.log(LogType.INFO, "Veritabanı başarıyla başlatıldı");

        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Veritabanı başlatma hatası: " + e.getMessage());
            throw new RuntimeException("Veritabanı başlatılamadı", e);
        }
    }
    private void createCollections() {
        try {
            database.createCollection("customers");
            database.getCollection("customers").createIndex(
                    new Document("customerName", 1)  // customerName üzerine index
            );
            database.createCollection("products");
            database.getCollection("products").createIndex(
                    new Document("productName", 1)  // productName üzerine index
            );
            database.createCollection("orders");
            database.getCollection("orders").createIndex(
                    new Document("orderDate", 1)  // orderDate üzerine index
            );

            database.createCollection("logs");
            database.getCollection("logs").createIndex(
                    new Document("logDate", 1)  // logDate üzerine index
            );

            Logger.log(LogType.INFO, "Koleksiyonlar başarıyla oluşturuldu");
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Koleksiyon oluşturma hatası: " + e.getMessage());
            throw new RuntimeException("Koleksiyonlar oluşturulamadı", e);
        }
    }

    public synchronized int generateNextProductId() {
        try {
            MongoCollection<Document> products = database.getCollection("products");
            Document maxIdDoc = products.find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            int nextId = (maxIdDoc != null) ? maxIdDoc.getInteger("_id") + 1 : 1;
            return nextId;
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error generating product ID: " + e.getMessage());
            return productIdCounter.getAndIncrement(); // Fallback
        }
    }

    public int generateNextCustomerId() {
        return customerIdCounter.getAndIncrement();
    }
    /*private void initializeCollections() {
        // Koleksiyonları oluştur (eğer yoksa)
        List<String> collectionNames = new ArrayList<>();
        database.listCollectionNames().into(collectionNames);

        if (!collectionNames.contains("customers")) {
            database.createCollection("customers");
        }
        if (!collectionNames.contains("products")) {
            database.createCollection("products");
        }
        if (!collectionNames.contains("orders")) {
            database.createCollection("orders");
        }
    }
    private int getLastProductId() {
        try {
            Document lastProduct = database.getCollection("products")
                    .find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            return lastProduct != null ? lastProduct.getInteger("_id") : 0;
        } catch (Exception e) {
            return 0;
        }
    }*/


    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public Customer getCustomer(int customerId) {
        try {
            Document doc = database.getCollection("customers").find(eq("_id", customerId)).first();
            if (doc != null) {
                Customer.CustomerType type = Customer.CustomerType.valueOf(doc.getString("type"));
                double budget = doc.getDouble("budget");
                double totalSpent = doc.getDouble("totalSpent");
                String name = doc.getString("name");

                Customer customer;
                if (type == Customer.CustomerType.PREMIUM) {
                    customer = new PremiumCustomer(customerId, name, budget);
                } else {
                    customer = new StandardCustomer(customerId, name, budget);
                }
                customer.setTotalSpent(totalSpent);
                return customer;
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Müşteri getirme hatası: " + e.getMessage());
        }
        return null;
    }
    /*public Customer getCustomerById(int customerId) {
        try {
            Document doc = database.getCollection("customers").find(eq("_id", customerId)).first();
            if (doc != null) {
                Customer.CustomerType type = Customer.CustomerType.valueOf(doc.getString("type"));
                if (type == Customer.CustomerType.PREMIUM) {
                    return new PremiumCustomer(
                            doc.getInteger("_id"),
                            doc.getString("name"),
                            doc.getDouble("budget")
                    );
                } else {
                    return new StandardCustomer(
                            doc.getInteger("_id"),
                            doc.getString("name"),
                            doc.getDouble("budget")
                    );
                }
            }
            return null;
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error getting customer by ID: " + e.getMessage());
            return null;
        }
    }*/

    /*private int getLastCustomerId() {
        try {
            Document lastCustomer = database.getCollection("customers")
                    .find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            return lastCustomer != null ? lastCustomer.getInteger("_id") : 0;
        } catch (Exception e) {
            return 0;
        }
    }*/
    public void saveLog(Log log) {
        try {
            Document doc = new Document()
                    .append("_id", log.getLogId())
                    .append("customerID", log.getCustomerID())
                    .append("orderID", log.getOrderID())
                    .append("logDate", log.getLogDate().toString())
                    .append("logType", log.getLogType().toString())
                    .append("logDetails", log.getLogDetails());

            database.getCollection("logs").insertOne(doc);
            System.out.println("Log saved successfully: " + log);
        } catch (Exception e) {
            System.err.println("Error saving log: " + e.getMessage());
        }
    }


    public void clearLogs() {
        try {
            database.getCollection("logs").deleteMany(new Document());
            Document maxIdDoc = database.getCollection("logs")
                    .find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            int nextId = (maxIdDoc != null) ? maxIdDoc.getInteger("_id") + 1 : 1;
            logIdCounter.set(nextId);

            System.out.println("All logs cleared from database");
        } catch (Exception e) {
            System.err.println("Error clearing logs: " + e.getMessage());
            throw new RuntimeException("Failed to clear logs", e);
        }
    }

    public void clearAllCustomers() {
        try {
            database.getCollection("customers").deleteMany(new Document());
            customerIdCounter.set(1);
            System.out.println("All customers cleared from database");
        } catch (Exception e) {
            System.err.println("Error clearing customers: " + e.getMessage());
            throw new RuntimeException("Failed to clear customers", e);
        }
    }

    public void clearAllProducts() {
        try {
            database.getCollection("products").deleteMany(new Document());
            Document maxIdDoc = database.getCollection("products")
                    .find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            int nextId = (maxIdDoc != null) ? maxIdDoc.getInteger("_id") + 1 : 1;
            productIdCounter.set(nextId);

            System.out.println("All products cleared from database");
        } catch (Exception e) {
            System.err.println("Error clearing products: " + e.getMessage());
            throw new RuntimeException("Failed to clear products", e);
        }
    }

    public void saveCustomer(Customer customer) {
        try {
            Document doc = new Document()
                    .append("_id", customer.getCustomerID())
                    .append("name", customer.getCustomerName())
                    .append("budget", customer.getBudget())
                    .append("type", customer.getType().toString())
                    .append("totalSpent", customer.getTotalSpent());

            database.getCollection("customers").insertOne(doc);
            System.out.println("Customer saved successfully: " + customer.getCustomerName());
        } catch (Exception e) {
            System.err.println("Error saving customer: " + e.getMessage());
            throw new RuntimeException("Error saving customer", e);
        }
    }

    public synchronized void updateCustomer(Customer customer) {
        try {
            MongoCollection<Document> customers = database.getCollection("customers");

            Document existingCustomer = customers.find(eq("_id", customer.getCustomerID())).first();
            if (existingCustomer == null) {
                Logger.log(LogType.ERROR, "Güncellenecek müşteri bulunamadı: " + customer.getCustomerID());
                return;
            }

            double existingTotalSpent = existingCustomer.getDouble("totalSpent");
            double newTotalSpent = Math.max(existingTotalSpent, customer.getTotalSpent());

            Document updates = new Document()
                    .append("name", customer.getCustomerName())
                    .append("budget", customer.getBudget())
                    .append("type", customer.getType().toString())
                    .append("totalSpent", newTotalSpent);

            Document updateOperation = new Document("$set", updates);

            UpdateResult result = customers.updateOne(eq("_id", customer.getCustomerID()), updateOperation);

            if (result.getModifiedCount() == 0) {
                Logger.log(LogType.WARNING, "Müşteri güncellenemedi: " + customer.getCustomerID());
            } else {
                Logger.log(LogType.INFO, String.format("Müşteri güncellendi - ID: %d, Bütçe: %.2f, TotalSpent: %.2f",
                        customer.getCustomerID(), customer.getBudget(), newTotalSpent));
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Müşteri güncelleme hatası: " + e.getMessage());
        }
    }

    public void saveProduct(Product product) {
        try {
            MongoCollection<Document> products = database.getCollection("products");
            Document doc = new Document("_id", product.getProductID())
                    .append("name", product.getProductName())
                    .append("stock", product.getStock())
                    .append("price", product.getPrice());

            products.insertOne(doc);
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error saving product: " + e.getMessage());
        }
    }

    public void updateProductStock(int productId, int newStock) {
        try {
            MongoCollection<Document> products = database.getCollection("products");
            Bson updates = Updates.set("stock", newStock);

            UpdateResult result = products.updateOne(eq("_id", productId), updates);
            if (result.getModifiedCount() == 0) {
                Logger.log(LogType.WARNING, "Product not found for stock update: " + productId);
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error updating product stock: " + e.getMessage());
        }
    }

    public void updateProductPrice(int productId, double newPrice) {
        try {
            MongoCollection<Document> products = database.getCollection("products");
            Bson updates = Updates.set("price", newPrice);

            UpdateResult result = products.updateOne(eq("_id", productId), updates);
            if (result.getModifiedCount() == 0) {
                Logger.log(LogType.WARNING, "Product not found for price update: " + productId);
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error updating product price: " + e.getMessage());
        }
    }


    public synchronized int generateNextLogId() {
        try {
            MongoCollection<Document> logs = database.getCollection("logs");
            Document maxIdDoc = logs.find()
                    .sort(new Document("_id", -1))
                    .limit(1)
                    .first();

            // Eğer kayıt varsa en yüksek ID + 1, yoksa 1 döndür
            int nextId = (maxIdDoc != null) ? maxIdDoc.getInteger("_id") + 1 : 1;

            // ID'nin benzersiz
            while (logs.find(eq("_id", nextId)).first() != null) {
                nextId++;
            }

            return nextId;
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error generating log ID: " + e.getMessage());
            return logIdCounter.getAndIncrement(); // Fallback mekanizması
        }
    }

    public void saveOrder(OrderManager.Order order) {
        try {
            MongoCollection<Document> orders = database.getCollection("orders");
            Document doc = new Document("_id", order.getOrderId())
                    .append("customerId", order.getCustomerID())
                    .append("productId", order.getProductID())
                    .append("quantity", order.getQuantity())
                    .append("totalPrice", order.getTotalPrice())
                    .append("status", order.getStatus().toString())
                    .append("orderTime", order.getOrderDate().toString());
            if (order.getCompletionTime() != null) {
                doc.append("completionTime", order.getCompletionTime().toString());
            }
            orders.insertOne(doc);
            ColorLogger.ColorLogEntry logEntry = ColorLogger.createLog(
                    LogType.INFO,
                    String.format("Sipariş kaydedildi - ID: %d, Müşteri: %d, Ürün: %d, Miktar: %d",
                            order.getOrderId(),
                            order.getCustomerID(),
                            order.getProductID(),
                            order.getQuantity())
            );

            if (MainGUI.getInstance() != null) {
                Platform.runLater(() -> {
                    MainGUI.getInstance().updateLogPanel(String.valueOf(logEntry));
                });
            }
        } catch (Exception e) {
            // Hata durumunda kırmızı renkli log
            ColorLogger.ColorLogEntry errorLog = ColorLogger.createLog(
                    LogType.ERROR,
                    "Sipariş kaydetme hatası: " + e.getMessage()
            );
            if (MainGUI.getInstance() != null) {
                Platform.runLater(() -> {
                    MainGUI.getInstance().updateLogPanel(String.valueOf(errorLog));
                });
            }
        }
    }

    public void updateOrder(OrderManager.Order order) {
        try {
            Document query = new Document("_id", order.getOrderId());
            Document updates = new Document("$set", new Document()
                    .append("status", order.getStatus().toString())
                    .append("completionTime", order.getCompletionTime() != null ?
                            order.getCompletionTime().toString() : null));

            database.getCollection("orders").updateOne(query, updates);
            System.out.println("Order updated successfully: " + order);
        } catch (Exception e) {
            System.err.println("Error updating order: " + e.getMessage());
            throw new RuntimeException("Error updating order", e);
        }
    }
    public void updateOrderStatus(int orderId, OrderManager.OrderStatus newStatus) {
        try {
            Document query = new Document("_id", orderId);
            Document update = new Document("$set", new Document()
                    .append("status", newStatus.toString())
                    .append("completionTime", newStatus == OrderManager.OrderStatus.COMPLETED ?
                            java.time.LocalDateTime.now().toString() : null));

            database.getCollection("orders").updateOne(query, update);
            System.out.println("Order status updated: " + orderId + " -> " + newStatus);
        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
            throw new RuntimeException("Error updating order status", e);
        }
    }

    public void deleteProduct(int productId) {
        try {
            MongoCollection<Document> products = database.getCollection("products");
            products.deleteOne(eq("_id", productId));
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error deleting product: " + e.getMessage());
        }
    }
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        try {
            MongoCollection<Document> collection = database.getCollection("customers");
            FindIterable<Document> documents = collection.find();

            for (Document doc : documents) {
                Customer customer;
                Customer.CustomerType type = Customer.CustomerType.valueOf(doc.getString("type"));
                double totalSpent = doc.getDouble("totalSpent");

                if (type == Customer.CustomerType.PREMIUM) {
                    customer = new PremiumCustomer(
                            doc.getInteger("_id"),
                            doc.getString("name"),
                            doc.getDouble("budget"),
                            totalSpent
                    );
                } else {
                    customer = new StandardCustomer(
                            doc.getInteger("_id"),
                            doc.getString("name"),
                            doc.getDouble("budget"),
                            totalSpent
                    );
                }
                customers.add(customer);
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error getting customers: " + e.getMessage());
        }
        return customers;
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            database.getCollection("products")
                    .find()
                    .forEach(doc -> {
                        Product product = new Product(
                                doc.getInteger("_id"),
                                doc.getString("name"),
                                doc.getInteger("stock"),
                                doc.getDouble("price")
                        );
                        products.add(product);
                    });
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error getting products: " + e.getMessage());
        }
        return products;
    }
    // Log işlemleri
    public void saveLog(Logger.LogEntry logEntry) {
        try {
            MongoCollection<Document> logs = database.getCollection("logs");
            Document doc = new Document()
                    .append("timestamp", logEntry.getTimestamp().toString())
                    .append("type", logEntry.getType().toString())
                    .append("message", logEntry.getMessage());

            logs.insertOne(doc);
        } catch (Exception e) {
            System.err.println("Error saving log: " + e.getMessage());
        }
    }

    // Kapatma işlemi
    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}