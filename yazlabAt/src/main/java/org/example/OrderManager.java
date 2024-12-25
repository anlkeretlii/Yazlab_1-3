package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import org.bson.Document;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.util.stream.Collectors;


public class OrderManager {
    private static volatile OrderManager instance;
    private final Map<Integer, Order> activeOrders;
    private final StockManager stockManager;
    private final DatabaseManager dbManager;
    private final AtomicInteger orderIdGenerator = new AtomicInteger(1);
    private final AtomicInteger logIdCounter = new AtomicInteger(1);
    private final PriorityBlockingQueue<Order> orderQueue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean isRunning = true;
    private final ScheduledExecutorService priorityScheduler = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService tableUpdateScheduler = Executors.newScheduledThreadPool(1);
    private final Map<Integer, Double> orderPriorityScores = new ConcurrentHashMap<>();






    private OrderManager() {
        this.orderQueue = new PriorityBlockingQueue<>(11, (o1, o2) -> {
            double score1 = calculateOrderPriority(o1);
            double score2 = calculateOrderPriority(o2);
            return Double.compare(score2, score1); // Yüksek skordan düşüğe sırala
        });
        this.activeOrders = new ConcurrentHashMap<>();
        this.stockManager = StockManager.getInstance();
        this.dbManager = DatabaseManager.getInstance();

        startPeriodicUpdates();

    }
    private void startPeriodicUpdates() {
        priorityScheduler.scheduleAtFixedRate(() -> {
            try {
                // Aktif siparişlerden bekleyen siparişleri al
                List<Order> pendingOrders = activeOrders.values().stream()
                        .filter(order -> order.getStatus() == OrderStatus.PENDING)
                        .collect(Collectors.toList());

                // Her sipariş için güncel öncelik skorunu hesapla ve güncelle
                for (Order order : pendingOrders) {
                    double currentPriority = calculateOrderPriority(order);
                    orderPriorityScores.put(order.getOrderId(), currentPriority);
                }

                // Siparişleri öncelik skorlarına göre sırala
                pendingOrders.sort((o1, o2) -> {
                    double score1 = orderPriorityScores.getOrDefault(o1.getOrderId(), 0.0);
                    double score2 = orderPriorityScores.getOrDefault(o2.getOrderId(), 0.0);
                    return Double.compare(score2, score1); // Büyükten küçüğe sıralama
                });

                // GUI güncellemesi
                Platform.runLater(() -> {
                    if (MainGUI.getInstance() != null) {
                        TableView<Order> table = MainGUI.getInstance().getPendingOrdersTable();

                        // Mevcut seçimleri koru
                        ObservableList<Order> selectedOrders = table.getSelectionModel().getSelectedItems();
                        List<Integer> selectedOrderIds = selectedOrders.stream()
                                .map(Order::getOrderId)
                                .collect(Collectors.toList());

                        // Tabloyu güncelle
                        table.setItems(FXCollections.observableArrayList(pendingOrders));

                        // Seçimleri geri yükle
                        for (Order order : pendingOrders) {
                            if (selectedOrderIds.contains(order.getOrderId())) {
                                table.getSelectionModel().select(order);
                            }
                        }

                        // Öncelik kolonunu güncelle
                        table.refresh();
                    }
                });

            } catch (Exception e) {
                Logger.log(LogType.ERROR, "Öncelik hesaplama hatası: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    private void updatePriorityScores() {
        // Tüm aktif siparişlerin öncelik skorlarını güncelle
        for (Order order : activeOrders.values()) {
            if (order.getStatus() == OrderStatus.PENDING) {
                double priority = calculateOrderPriority(order);
                orderPriorityScores.put(order.getOrderId(), priority);
            }
        }
    }

    private void updateOrderQueue() {
        // Kuyruğu yeniden sırala ve GUI'yi güncelle
        List<Order> currentOrders = new ArrayList<>(orderQueue);
        orderQueue.clear();
        orderQueue.addAll(currentOrders);

        Platform.runLater(() -> {
            if (MainGUI.getInstance() != null) {
                MainGUI.getInstance().updatePendingOrdersTable();
            }
        });
    }
    public double getOrderPriority(Order order) {
        return orderPriorityScores.getOrDefault(order.getOrderId(), calculateOrderPriority(order));
    }

    public static OrderManager getInstance() {
        if (instance == null) {
            synchronized (OrderManager.class) {
                if (instance == null) {
                    instance = new OrderManager();
                }
            }
        }
        return instance;
    }

    double calculateOrderPriority(Order order) {
        Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
        if (customer == null) return 0.0;

        // Temel öncelik puanı
        double baseScore = customer.getType() == Customer.CustomerType.PREMIUM ? 15.0 : 10.0;

        // Bekleme süresi (saniye)
        long waitingTimeSeconds = Duration.between(order.getOrderDate(), LocalDateTime.now()).getSeconds();

        // Öncelik skoru = TemelÖncelikSkoru + (BeklemeSüresi × 0.5)
        return baseScore + (waitingTimeSeconds * 0.5);
    }


    public synchronized Order placeOrder(Customer customer, Product product, int quantity, boolean isManual) {
        try {
            customer.startOrder();
            // Stok kontrolü
            if (!stockManager.checkStock(product.getProductID(), quantity)) {
                Logger.log(LogType.ERROR, customer.getCustomerID(),
                        String.format("Yetersiz stok - Ürün: %s, İstenen: %d, Mevcut: %d",
                                product.getProductName(), quantity, product.getStock()));
                return null;
            }

            // Bütçe kontrolü
            double totalCost = product.getPrice() * quantity;
            if (customer.getBudget() < totalCost) {
                Logger.log(LogType.ERROR, customer.getCustomerID(),
                        String.format("Yetersiz bütçe - Gereken: %.2f TL, Mevcut: %.2f TL",
                                totalCost, customer.getBudget()));
                return null;
            }

            // Bütçeyi düş
            customer.setBudget(customer.getBudget() - totalCost);
            dbManager.updateCustomer(customer);

            // Yeni sipariş oluştur
            int orderId = orderIdGenerator.getAndIncrement();
            Order order = new Order(
                    orderId,
                    customer.getCustomerID(),
                    product.getProductID(),
                    quantity,
                    totalCost,
                    isManual
            );
            orderQueue.offer(order);

            // Siparişi kaydet
            activeOrders.put(orderId, order);
            dbManager.saveOrder(order);

            // GUI güncelle
            Platform.runLater(() -> {
                if (MainGUI.getInstance() != null) {
                    MainGUI.getInstance().updatePendingOrdersTable();
                    MainGUI.getInstance().updateCustomerTable(); // Bütçe güncellemesi için
                }
            });

            Logger.log(LogType.INFO,
                    String.format("Sipariş kuyruğa eklendi - Müşteri: %d, Öncelik Skoru: %.2f",
                            customer.getCustomerID(),
                            customer.calculatePriorityScore()));

            return order;
        } catch (Exception e) {
            customer.completeOrder();
            Logger.log(LogType.ERROR, "Sipariş oluşturma hatası: " + e.getMessage());
            return null;
        }
    }
    public synchronized Order placeOrder(Customer customer, Product product, int quantity) {
        return placeOrder(customer, product, quantity, false);
    }


    public List<OrderWithPriority> getPendingOrdersWithPriority() {
        List<OrderWithPriority> priorityList = new ArrayList<>();

        for (Order order : getPendingOrders()) {
            double priority = calculateOrderPriority(order);
            priorityList.add(new OrderWithPriority(order, priority));
        }

        // Öncelik skoruna göre sırala
        priorityList.sort((o1, o2) -> Double.compare(o2.getPriority(), o1.getPriority()));
        return priorityList;
    }

    public static class OrderWithPriority {
        private final Order order;
        private final double priority;

        public OrderWithPriority(Order order, double priority) {
            this.order = order;
            this.priority = priority;
        }

        public Order getOrder() { return order; }
        public double getPriority() { return priority; }
    }


    public boolean hasPremiumCustomersWaiting() {
        return orderQueue.stream()
                .map(order -> DatabaseManager.getInstance().getCustomer(order.getCustomerID()))
                .anyMatch(customer -> customer != null && customer.getType() == Customer.CustomerType.PREMIUM);
    }

    public void processNextOrder() {
        Order order = orderQueue.poll();
        if (order != null) {
            Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
            if (customer != null) {
                processOrder(order, customer);
            }
        }
    }
    private Order findOrderForCustomer(int customerID) {
        return activeOrders.values().stream()
                .filter(order -> order.getCustomerID() == customerID)
                .findFirst()
                .orElse(null);
    }
    public synchronized void processOrder(Order order, Customer customer) {
        try {
            order.setStatus(OrderStatus.PROCESSING);
            dbManager.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);

            // Her işlemde güncel müşteri bilgilerini veritabanından al
            Customer updatedCustomer = dbManager.getCustomer(customer.getCustomerID());
            if (updatedCustomer == null) {
                Logger.log(LogType.ERROR, "Müşteri bulunamadı: " + customer.getCustomerID());
                return;
            }

            // Stok güncelleme
            if (!stockManager.decreaseStock(order.getProductID(), order.getQuantity())) {
                order.setStatus(OrderStatus.FAILED);
                dbManager.updateOrderStatus(order.getOrderId(), OrderStatus.FAILED);
                Logger.log(LogType.ERROR, order.getCustomerID(),
                        "Stok güncelleme başarısız - Sipariş ID: " + order.getOrderId());
                return;
            }

            // Siparişi tamamla ve totalSpent'i güncelle - synchronized block içinde
            double newTotalSpent;
            synchronized (updatedCustomer) {
                double currentTotalSpent = updatedCustomer.getTotalSpent();
                newTotalSpent = currentTotalSpent + order.getTotalPrice();
                updatedCustomer.setTotalSpent(newTotalSpent);
                dbManager.updateCustomer(updatedCustomer);
            }

            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletionTime(LocalDateTime.now());
            dbManager.updateOrder(order);

            // Aktif siparişlerden kaldır
            activeOrders.remove(order.getOrderId());
            orderPriorityScores.remove(order.getOrderId());


            // GUI güncelle
            Platform.runLater(() -> {
                if (MainGUI.getInstance() != null) {
                    MainGUI.getInstance().updatePendingOrdersTable();
                    MainGUI.getInstance().updateCustomerTable();
                    MainGUI.getInstance().updateProductTable();
                }
            });

            Logger.log(LogType.INFO, order.getCustomerID(),
                    String.format("Sipariş tamamlandı - ID: %d, Toplam: %.2f TL, Yeni Toplam Harcama: %.2f TL",
                            order.getOrderId(), order.getTotalPrice(), newTotalSpent));

        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
            Logger.log(LogType.ERROR, order.getCustomerID(), "Sipariş işleme hatası: " + e.getMessage());
        }
    }
    public List<Order> getPendingOrders() {
        List<Order> pendingOrders = new ArrayList<>();
        try {
            for (Order order : activeOrders.values()) {
                if (order.getStatus() == OrderStatus.PENDING) {
                    pendingOrders.add(order);
                }
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error fetching pending orders: " + e.getMessage());
        }
        return pendingOrders;
    }



    public void shutdown() {
        isRunning = false;
        priorityScheduler.shutdown();
        tableUpdateScheduler.shutdown();
        try {
            if (!priorityScheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                priorityScheduler.shutdownNow();
            }
            if (!tableUpdateScheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                tableUpdateScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            priorityScheduler.shutdownNow();
            tableUpdateScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }




   /* public Order getOrderFromDocument(Document doc) {
        try {
            return new Order(
                    doc.getInteger("_id"),
                    doc.getInteger("customerId"),
                    doc.getInteger("productId"),
                    doc.getInteger("quantity"),
                    doc.getDouble("totalPrice")
            );
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error creating order from document: " + e.getMessage());
            return null;
        }
    }*/


    public void cancelOrder(int orderId) {
        Order order = activeOrders.get(orderId);
        if (order != null && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            activeOrders.remove(orderId);
            dbManager.updateOrderStatus(orderId, OrderStatus.CANCELLED);

            // Siparişin parasını iade et
            Customer customer = dbManager.getCustomer(order.getCustomerID());
            if (customer != null) {
                customer.setBudget(customer.getBudget() + order.getTotalPrice());
                dbManager.updateCustomer(customer);
            }

            Logger.getInstance().log(LogType.INFO, "Order cancelled - OrderID: " + orderId);
        }
    }




    public class Order {
        private final int orderId;
        private final int customerID;
        private final int productID;
        private final int quantity;
        private final double totalPrice;
        private final LocalDateTime orderDate;
        private LocalDateTime completionTime;
        private OrderStatus status;
        private final boolean isManualOrder;


        public Order(int orderId, int customerID, int productID, int quantity, double totalPrice, boolean isManualOrder) {
            this.orderId = orderId;
            this.customerID = customerID;
            this.productID = productID;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
            this.orderDate = LocalDateTime.now();
            this.status = OrderStatus.PENDING;
            this.completionTime = null;
            this.isManualOrder = isManualOrder;
        }



        // Getters
        public int getOrderId() { return orderId; }
        public int getCustomerID() { return customerID; }
        public int getProductID() { return productID; }
        public int getQuantity() { return quantity; }
        public double getTotalPrice() { return totalPrice; }
        public LocalDateTime getOrderDate() { return orderDate; }
        public OrderStatus getStatus() { return status; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public boolean isManualOrder() { return isManualOrder; }

        // Setters
        public void setStatus(OrderStatus status) { this.status = status; }
        public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }

        @Override
        public String toString() {
            return String.format("Order{id=%d, customer=%d, product=%d, quantity=%d, total=%.2f, status=%s}",
                    orderId, customerID, productID, quantity, totalPrice, status);
        }
    }

    enum OrderStatus {
        PENDING,    // Sipariş alındı, işlem bekliyor
        PROCESSING, // Sipariş işleniyor
        COMPLETED,  // Sipariş tamamlandı
        CANCELLED,  // Sipariş iptal edildi
        FAILED      // Sipariş başarısız oldu
    }
}
