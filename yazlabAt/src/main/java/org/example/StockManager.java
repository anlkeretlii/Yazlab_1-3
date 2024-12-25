package org.example;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;

public class StockManager {
    private static volatile StockManager instance;
    private final Map<Integer, Product> products;
    private final Semaphore stockUpdateSemaphore;

    private StockManager() {
        this.products = new ConcurrentHashMap<>();
        this.stockUpdateSemaphore = new Semaphore(1);
        initializeProducts();
    }

    public static StockManager getInstance() {
        if (instance == null) {
            synchronized (StockManager.class) {
                if (instance == null) {
                    instance = new StockManager();
                }
            }
        }
        return instance;
    }

    private void initializeProducts() {
        // Başlangıç ürünlerini ekle
        addProduct(new Product(1, "Product1", 500, 100.0));
        addProduct(new Product(2, "Product2", 10, 50.0));
        addProduct(new Product(3, "Product3", 200, 45.0));
        addProduct(new Product(4, "Product4", 75, 75.0));
        addProduct(new Product(5, "Product5", 0, 500.0));
    }

    public void addProduct(Product product) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                // Önce var olan aynı isimli ürünü kontrol et
                Optional<Product> existingProduct = products.values().stream()
                        .filter(p -> p.getProductName().equals(product.getProductName()))
                        .findFirst();

                if (existingProduct.isPresent()) {
                    // Var olan ürünün stokunu güncelle
                    Product existing = existingProduct.get();
                    existing.increaseStock(product.getStock());
                    DatabaseManager.getInstance().updateProductStock(existing.getProductID(), existing.getStock());
                    Logger.log(LogType.INFO,
                            String.format("Product stock updated - ID: %d, Name: %s, New Stock: %d",
                                    existing.getProductID(), existing.getProductName(), existing.getStock()));
                } else {
                    // Yeni ürün ekle
                    products.put(product.getProductID(), product);
                    DatabaseManager.getInstance().saveProduct(product);
                    Logger.log(LogType.INFO,
                            String.format("New product added - ID: %d, Name: %s, Stock: %d",
                                    product.getProductID(), product.getProductName(), product.getStock()));
                }
                if (MainGUI.getInstance() != null) {
                    MainGUI.getInstance().updateProductTable();
                }
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error adding/updating product: " + e.getMessage());
        }
    }

    public boolean checkStock(int productId, int quantity) {
        Product product = products.get(productId);
        return product != null && product.getStock() >= quantity;
    }
    public synchronized void updateProductStock(int productId, int newStock) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                Product product = products.get(productId);
                if (product != null) {
                    while (product.getStock() > 0) {
                        product.decreaseStock(1);
                    }
                    product.increaseStock(newStock);

                    DatabaseManager.getInstance().updateProductStock(productId, newStock);

                    Logger.log(LogType.INFO,
                            String.format("Stock updated - ProductID: %d, New Stock: %d",
                                    productId, newStock));

                    if (MainGUI.getInstance() != null) {
                        MainGUI.getInstance().updateProductTable();
                    }
                }
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error updating stock: " + e.getMessage());
        }
    }

    public synchronized boolean decreaseStock(int productId, int quantity) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                Product product = products.get(productId);
                if (product != null && product.decreaseStock(quantity)) {
                    DatabaseManager.getInstance().updateProductStock(productId, product.getStock());
                    if (product.getStock() < 10) {
                        Logger.log(LogType.WARNING,
                                String.format("Low stock alert - ProductID: %d, Current Stock: %d",
                                        productId, product.getStock()));
                    }
                    return true;
                }
                return false;
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error updating stock: " + e.getMessage());
            return false;
        }
    }

   /* public synchronized void increaseStock(int productId, int quantity) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                Product product = products.get(productId);
                if (product != null) {
                    product.increaseStock(quantity);
                    // Veritabanını güncelle
                    DatabaseManager.getInstance().updateProductStock(productId, product.getStock());

                    Logger.log(LogType.INFO,
                            String.format("Stock increased - ProductID: %d, Added: %d, New Stock: %d",
                                    productId, quantity, product.getStock()));
                }
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error increasing stock: " + e.getMessage());
        }
    }*/

    public Product getProduct(int productId) {
        return products.get(productId);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public void removeProduct(int productId) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                Product removedProduct = products.remove(productId);
                if (removedProduct != null) {
                    DatabaseManager.getInstance().deleteProduct(productId);
                    Logger.log(LogType.INFO, "Product removed - ID: " + productId);
                }
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error removing product: " + e.getMessage());
        }
    }

    public void updateProductPrice(int productId, double newPrice) {
        try {
            stockUpdateSemaphore.acquire();
            try {
                Product product = products.get(productId);
                if (product != null) {
                    product.setPrice(newPrice);
                    DatabaseManager.getInstance().updateProductPrice(productId, newPrice);

                    Logger.log(LogType.INFO,
                            String.format("Price updated - ProductID: %d, New Price: %.2f",
                                    productId, newPrice));
                }
            } finally {
                stockUpdateSemaphore.release();
            }
        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Error updating product price: " + e.getMessage());
        }
    }
}
