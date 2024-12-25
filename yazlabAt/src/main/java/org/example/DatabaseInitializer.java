package org.example;


import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class DatabaseInitializer {
    private static final Random random = new Random();

    public static void initializeDatabase() {
        DatabaseManager dbManager = DatabaseManager.getInstance();

        clearAllData(dbManager);

        int customerCount = random.nextInt(6) + 5;

        List<Customer> customers = createRandomCustomers(customerCount);

        List<Product> products = createInitialProducts();

        saveToDatabase(dbManager, customers, products);
    }

    private static void clearAllData(DatabaseManager dbManager) {
        dbManager.clearAllCustomers();
        dbManager.clearAllProducts();
        dbManager.clearLogs();
        Logger.getInstance().log(LogType.INFO, "Veritabanı temizlendi ve sıfırdan başlatıldı");
    }

    private static List<Customer> createRandomCustomers(int totalCount) {
        List<Customer> customers = new ArrayList<>();

        int premiumCount = Math.max(2, random.nextInt(totalCount / 2));
        int standardCount = totalCount - premiumCount;

        for (int i = 0; i < premiumCount; i++) {
            int id = DatabaseManager.getInstance().generateNextCustomerId();
            String name = generateRandomName();
            int budget = random.nextInt(2501) + 500; // 500-3000 TL arası tam sayı
            customers.add(new PremiumCustomer(id, name, budget));
        }

        for (int i = 0; i < standardCount; i++) {
            int id = DatabaseManager.getInstance().generateNextCustomerId();
            String name = generateRandomName();
            int budget = random.nextInt(2501) + 500; // 500-3000 TL arası tam sayı
            customers.add(new StandardCustomer(id, name, budget));
        }

        return customers;
    }

    private static List<Product> createInitialProducts() {
        List<Product> products = new ArrayList<>();
        DatabaseManager dbManager = DatabaseManager.getInstance();

        // PDF'deki sabit ürünler
        products.add(new Product(dbManager.generateNextProductId(), "Product1", 500, 100));
        products.add(new Product(dbManager.generateNextProductId(), "Product2", 10, 50));
        products.add(new Product(dbManager.generateNextProductId(), "Product3", 200, 45));
        products.add(new Product(dbManager.generateNextProductId(), "Product4", 75, 75));
        products.add(new Product(dbManager.generateNextProductId(), "Product5", 0, 500));

        return products;
    }

    private static String generateRandomName() {
        String[] names = {"Ali", "Ayşe", "Mehmet", "Fatma", "Can", "Zeynep", "Ahmet", "Elif", "Mustafa", "Esra"};
        String[] surnames = {"Yılmaz", "Demir", "Aynacı", "Şahin", "Çelik", "Yıldız", "Özdemir", "Arslan", "Doğan", "Ay"};

        return names[random.nextInt(names.length)] + " " +
                surnames[random.nextInt(surnames.length)];
    }

    private static void saveToDatabase(DatabaseManager dbManager,
                                       List<Customer> customers,
                                       List<Product> products) {

        for (Customer customer : customers) {
            dbManager.saveCustomer(customer);
            Logger.getInstance().log(LogType.INFO,
                    String.format("%s müşteri oluşturuldu: %s (Bütçe: %d TL)",
                            customer.getType(),
                            customer.getCustomerName(),
                            (int)customer.getBudget()));
        }

        for (Product product : products) {
            dbManager.saveProduct(product);
            Logger.getInstance().log(LogType.INFO,
                    String.format("Ürün oluşturuldu: %s (Stok: %d, Fiyat: %.0f TL)",
                            product.getProductName(),
                            product.getStock(),
                            product.getPrice()));
        }
    }
}