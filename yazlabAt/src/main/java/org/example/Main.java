package org.example;

import javafx.application.Application;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Veritabanını başlat
            DatabaseInitializer.initializeDatabase();

            // Thread yöneticisini al
            ThreadManager threadManager = ThreadManager.getInstance();

            // Müşteri thread'lerini başlat
            List<Customer> customers = DatabaseManager.getInstance().getAllCustomers();
            for (Customer customer : customers) {
                threadManager.addCustomer(customer);
            }



            // GUI'yi başlat
            Application.launch(MainGUI.class, args);

        } catch (Exception e) {
            System.err.println("Uygulama başlatma hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
}