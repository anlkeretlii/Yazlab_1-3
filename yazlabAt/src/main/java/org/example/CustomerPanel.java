package org.example;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import javafx.scene.paint.Color;

public class CustomerPanel extends VBox {
    private final TableView<CustomerViewModel> customerTable;
    private final MainGUI mainGUI;

    public static class CustomerViewModel {
        private final Customer customer;
        private String status = "Beklemede";
        private double priorityScore;

        public CustomerViewModel(Customer customer) {
            this.customer = customer;
            this.priorityScore = customer.calculatePriorityScore();
        }

    }

    public CustomerPanel(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
        setPadding(new Insets(10));
        setSpacing(10);

        customerTable = new TableView<>();
        setupCustomerTable();

        SplitPane splitPane = new SplitPane();

        VBox customerListSection = createCustomerListSection();

        VBox orderFormSection = createOrderFormSection();

        splitPane.getItems().addAll(customerListSection, orderFormSection);
        splitPane.setDividerPositions(0.6);

        getChildren().add(splitPane);

        startPeriodicUpdate();
    }

    private void setupCustomerTable() {
        // Tür kolonu
        TableColumn<CustomerViewModel, String> typeCol = new TableColumn<>("Tür");
        typeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().customer.getType().toString()));

        // Bütçe kolonu
        TableColumn<CustomerViewModel, Double> budgetCol = new TableColumn<>("Bütçe");
        budgetCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().customer.getBudget()).asObject());

        // Toplam Harcama kolonu
        TableColumn<CustomerViewModel, Double> spentCol = new TableColumn<>("Toplam Harcama");
        spentCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(0.0).asObject()); // Default 0.0

        // Durum kolonu
        TableColumn<CustomerViewModel, String> statusCol = new TableColumn<>("Durum");
        statusCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty("Beklemede"));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTextFill(Color.GREEN);
                }
            }
        });

        customerTable.getColumns().addAll(typeCol, budgetCol, spentCol, statusCol);
    }

    private VBox createCustomerListSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));

        Label title = new Label("Müşteriler");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        Button addCustomerBtn = new Button("Yeni Müşteri");
        addCustomerBtn.setMaxWidth(Double.MAX_VALUE);

        section.getChildren().addAll(title, customerTable, addCustomerBtn);
        return section;
    }

    private VBox createOrderFormSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));

        Label title = new Label("Sipariş Oluştur");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Müşteri Seçin");

        ComboBox<Product> productComboBox = new ComboBox<>();
        productComboBox.setPromptText("Ürün Seçin");

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 5, 1);
        quantitySpinner.setEditable(true);

        Button orderButton = new Button("Sipariş Ver");
        orderButton.setMaxWidth(Double.MAX_VALUE);

        section.getChildren().addAll(
                title,
                new Label("Müşteri:"),
                customerComboBox,
                new Label("Ürün:"),
                productComboBox,
                new Label("Miktar:"),
                quantitySpinner,
                orderButton
        );

        return section;
    }

    private void startPeriodicUpdate() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::updateCustomerTable);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void updateCustomerTable() {
        List<Customer> customers = DatabaseManager.getInstance().getAllCustomers();
        List<CustomerViewModel> viewModels = customers.stream()
                .map(CustomerViewModel::new)
                .collect(java.util.stream.Collectors.toList());
        customerTable.setItems(FXCollections.observableArrayList(viewModels));
    }
}