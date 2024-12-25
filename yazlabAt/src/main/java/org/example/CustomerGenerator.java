package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomerGenerator {
    private static final Random random = new Random();

    public static List<Customer> generateRandomCustomers() {
        List<Customer> customers = new ArrayList<>();

        int totalCustomers = random.nextInt(6) + 5; // 5 to 10

        // min 2 pre
        int premiumCount = Math.max(2, random.nextInt(totalCustomers - 1));
        int standardCount = totalCustomers - premiumCount;

        for (int i = 0; i < premiumCount; i++) {
            customers.add(createPremiumCustomer());
        }

        for (int i = 0; i < standardCount; i++) {
            customers.add(createStandardCustomer());
        }

        return customers;
    }

    private static PremiumCustomer createPremiumCustomer() {
        return new PremiumCustomer(
                DatabaseManager.getInstance().generateNextCustomerId(),
                generateRandomName(),
                generateRandomBudget()
        );
    }

    private static StandardCustomer createStandardCustomer() {
        return new StandardCustomer(
                DatabaseManager.getInstance().generateNextCustomerId(),
                generateRandomName(),
                generateRandomBudget()
        );
    }

    private static double generateRandomBudget() {
        return 500 + random.nextDouble() * 2500; // 500 to 3000 TL
    }
    //Rastgele isim ve soy isimler eklenebilir.
    private static String generateRandomName() {
        String[] firstNames = {"Ali", "Ayşe", "Mehmet", "Fatma", "Can", "Zeynep", "Ahmet", "Elif", "Mustafa", "Esra"};
        String[] lastNames = {"Yılmaz", "Demir", "Aynacı", "Şahin", "Çelik", "Yıldız", "Özdemir", "Arslan", "Doğan", "Ay"};

        return firstNames[random.nextInt(firstNames.length)] + " " +
                lastNames[random.nextInt(lastNames.length)];
    }
}