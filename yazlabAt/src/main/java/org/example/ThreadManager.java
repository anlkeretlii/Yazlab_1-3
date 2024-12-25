package org.example;


import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.Random;

public class ThreadManager {
    private static volatile ThreadManager instance;
    private final ExecutorService customerThreadPool;
    private final Random random = new Random();
    private final List<CustomerOrderTask> customerTasks;
    private volatile boolean isRunning;
    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();
    private final List<Thread> activeThreads = new ArrayList<>();

    private static final int MIN_ORDER_DELAY = 10;
    private static final int MAX_ORDER_DELAY = 15;

    private ThreadManager() {
        this.customerThreadPool = Executors.newFixedThreadPool(10);
        this.customerTasks = new ArrayList<>();
        this.isRunning = true;
    }

    public static ThreadManager getInstance() {
        if (instance == null) {
            synchronized (ThreadManager.class) {
                if (instance == null) {
                    instance = new ThreadManager();
                }
            }
        }
        return instance;
    }


    private class CustomerOrderTask implements Runnable {
        private final Customer customer;
        private final Random random = new Random();

        public CustomerOrderTask(Customer customer) {
            this.customer = customer;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    checkPaused();
                    if (AdminPanel.getAdminLock().tryAcquire()) {
                        try {
                            placeRandomOrder();
                        } finally {
                            AdminPanel.getAdminLock().release();
                        }

                        int waitTime = MIN_ORDER_DELAY + random.nextInt(MAX_ORDER_DELAY - MIN_ORDER_DELAY + 1);
                        Thread.sleep(waitTime * 1000);
                    } else {

                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        private void placeRandomOrder() {
            try {
                List<Product> availableProducts = StockManager.getInstance().getAllProducts();
                if (!availableProducts.isEmpty()) {
                    int numberOfDifferentProducts = random.nextInt(3) + 1;

                    List<Product> availableProductsCopy = new ArrayList<>(availableProducts);

                    for (int i = 0; i < numberOfDifferentProducts && !availableProductsCopy.isEmpty(); i++) {
                        int randomIndex = random.nextInt(availableProductsCopy.size());
                        Product selectedProduct = availableProductsCopy.get(randomIndex);
                        int quantity = random.nextInt(5) + 1;
                        OrderManager.getInstance().placeOrder(customer, selectedProduct, quantity);

                        Logger.log(LogType.INFO, customer.getCustomerID(),
                                String.format("Müşteri %d, %s ürününden %d adet sipariş verdi",
                                        customer.getCustomerID(), selectedProduct.getProductName(), quantity));

                        availableProductsCopy.remove(randomIndex);
                    }
                }
            } catch (Exception e) {
                Logger.log(LogType.ERROR, customer.getCustomerID(),
                        "Sipariş verme hatası: " + e.getMessage());
            }
        }
    }

    public synchronized void pauseAll() {
        isPaused = true;
        Logger.log(LogType.INFO, "Tüm thread'ler duraklatıldı");
    }

    public synchronized void resumeAll() {
        synchronized (pauseLock) {
            isPaused = false;
            pauseLock.notifyAll();
            Logger.log(LogType.INFO, "Tüm thread'ler devam ediyor");
        }
    }
    private void checkPaused() {
        while (isPaused) {
            synchronized (pauseLock) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

  /* otomatik stok arttırma artık gerek yok test amaçlı...
   private void checkAndUpdateStocks() {
        try {
            StockManager stockManager = StockManager.getInstance();
            List<Product> products = stockManager.getAllProducts();

            for (Product product : products) {
                if (product.getStock() < 10) {
                    Logger.log(LogType.WARNING,
                            String.format("Kritik stok uyarısı - %s: %d adet kaldı",
                                    product.getProductName(), product.getStock()));

                    stockManager.increaseStock(product.getProductID(), 50);
                }
            }
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Stok kontrol hatası: " + e.getMessage());
        }
    }*/


    public void addCustomer(Customer customer) {
        CustomerOrderTask task = new CustomerOrderTask(customer);
        customerTasks.add(task);
        customerThreadPool.submit(task);

        Logger.log(LogType.INFO,
                String.format("Müşteri thread'i başlatıldı - ID: %d, İsim: %s, Tip: %s",
                        customer.getCustomerID(),
                        customer.getCustomerName(),
                        customer.getType()));
    }

    public void stopAll() {
        isRunning = false;
        resumeAll(); // Duraklatılmış thread'leri uyandır
        customerThreadPool.shutdown();
        try {
            if (!customerThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                customerThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            customerThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

