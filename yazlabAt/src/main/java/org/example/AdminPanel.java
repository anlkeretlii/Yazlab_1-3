package org.example;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class AdminPanel extends VBox {
    private final TableView<OrderManager.Order> pendingOrdersTable;
    private final TableView<Product> productTable;
    private final static Semaphore adminLock = new Semaphore(1);
    private final MainGUI mainGUI;
    private TextArea logArea;

    public AdminPanel(MainGUI mainGUI) {
        this.mainGUI = mainGUI;
        setPadding(new Insets(10));
        setSpacing(10);

        SplitPane mainSplitPane = new SplitPane();
        VBox customerSection = createCustomerSection();
        VBox productSection = createProductSection();
        VBox logSection = createLogSection();
        mainSplitPane.getItems().addAll(customerSection, productSection, logSection);
        mainSplitPane.setDividerPositions(0.3, 0.7);

        getChildren().add(mainSplitPane);

        pendingOrdersTable = new TableView<>();
        productTable = new TableView<>();
        setupTables();

        startPeriodicUpdate();
    }

    private VBox createCustomerSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setPrefWidth(300);

        Label title = new Label("Müşteriler");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        TableView<Customer> customerTable = new TableView<>();
        setupCustomerTable(customerTable);

        Button addCustomerBtn = new Button("Yeni Müşteri");
        addCustomerBtn.setMaxWidth(Double.MAX_VALUE);

        section.getChildren().addAll(title, customerTable, addCustomerBtn);
        return section;
    }

    private VBox createProductSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));

        Label title = new Label("Ürün Yönetimi");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");
        setupProductTable();

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(
                new Button("Yeni Ürün Ekle"),
                new Button("Ürün Sil"),
                new Button("Stok Güncelle")
        );

        Label ordersTitle = new Label("Bekleyen Siparişler");
        ordersTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        section.getChildren().addAll(
                title, productTable, buttonBox,
                new Separator(),
                ordersTitle, pendingOrdersTable
        );

        return section;
    }

    private VBox createLogSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setPrefWidth(300);

        Label title = new Label("Sistem Logları");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(20);

        Label priorityTitle = new Label("Öncelik Sırası");
        priorityTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        ListView<String> priorityList = new ListView<>();

        section.getChildren().addAll(title, logArea, priorityTitle, priorityList);
        return section;
    }

    private void setupCustomerTable(TableView<Customer> table) {
        TableColumn<Customer, String> typeCol = new TableColumn<>("Tür");
        TableColumn<Customer, Double> budgetCol = new TableColumn<>("Bütçe");
        TableColumn<Customer, Double> spentCol = new TableColumn<>("Toplam Harcama");
        TableColumn<Customer, String> statusCol = new TableColumn<>("Durum");

        table.getColumns().addAll(typeCol, budgetCol, spentCol, statusCol);
    }

    private void setupProductTable() {
        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        TableColumn<Product, String> nameCol = new TableColumn<>("Ürün Adı");
        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stok");
        TableColumn<Product, Double> priceCol = new TableColumn<>("Fiyat");

        productTable.getColumns().addAll(idCol, nameCol, stockCol, priceCol);
    }

    private void setupTables() {
        // Bekleyen siparişler tablosu kolonları
        TableColumn<OrderManager.Order, Integer> orderIdCol = new TableColumn<>("Sipariş ID");
        TableColumn<OrderManager.Order, Integer> customerIdCol = new TableColumn<>("Müşteri ID");
        TableColumn<OrderManager.Order, String> productCol = new TableColumn<>("Ürün");
        TableColumn<OrderManager.Order, Integer> quantityCol = new TableColumn<>("Miktar");
        TableColumn<OrderManager.Order, Button> actionCol = new TableColumn<>("İşlem");

        pendingOrdersTable.getColumns().addAll(
                orderIdCol, customerIdCol, productCol, quantityCol, actionCol
        );
    }

   /* private VBox createStockManagementSection() {
        VBox stockSection = new VBox(10);
        stockSection.setPadding(new Insets(10));

        ComboBox<Product> productComboBox = new ComboBox<>();
        TextField stockField = new TextField();
        Button updateButton = new Button("Stok Güncelle");

        updateButton.setOnAction(e -> {
            try {
                adminLock.acquire();
                Product selectedProduct = productComboBox.getValue();
                int newStock = Integer.parseInt(stockField.getText());

                if (selectedProduct != null) {
                    StockManager.getInstance().updateProductStock(selectedProduct.getProductID(), newStock);
                    mainGUI.updateProductTable();
                    Logger.getInstance().log(LogType.INFO,
                            String.format("Admin stok güncelledi - Ürün: %s, Yeni Stok: %d",
                                    selectedProduct.getProductName(), newStock));
                }
            } catch (Exception ex) {
                Logger.getInstance().log(LogType.ERROR, "Stok güncelleme hatası: " + ex.getMessage());
            } finally {
                adminLock.release();
            }
        });

        stockSection.getChildren().addAll(
                new Label("Stok Yönetimi"),
                productComboBox,
                new Label("Yeni Stok Miktarı:"),
                stockField,
                updateButton
        );

        // Ürün listesini periyodik güncelle
        Platform.runLater(() -> {
            List<Product> products = StockManager.getInstance().getAllProducts();
            productComboBox.setItems(FXCollections.observableArrayList(products));
        });

        return stockSection;
    }*/



    /*private void handleOrderApproval(OrderManager.Order order) {
        try {
            adminLock.acquire();
            Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
            if (customer != null) {
                OrderManager.getInstance().processOrder(order, customer);
                mainGUI.updateProductTable();
                updatePendingOrders();
            } else {
                Logger.getInstance().log(LogType.ERROR, "Müşteri bulunamadı: " + order.getCustomerID());
            }
        } catch (Exception e) {
            Logger.getInstance().log(LogType.ERROR, "Sipariş onaylama hatası: " + e.getMessage());
        } finally {
            adminLock.release();
        }
    }*/

    /*private void handleBulkApproval() {
        ObservableList<OrderManager.Order> selectedOrders =
                pendingOrdersTable.getSelectionModel().getSelectedItems();

        if (selectedOrders.isEmpty()) {
            showAlert("Lütfen onaylanacak siparişleri seçin.", Alert.AlertType.WARNING);
            return;
        }

        try {
            adminLock.acquire();
            for (OrderManager.Order order : selectedOrders) {
                Customer customer = DatabaseManager.getInstance().getCustomerById(order.getCustomerID());
                if (customer != null) {
                    OrderManager.getInstance().processOrder(order, customer);
                } else {
                    Logger.log(LogType.ERROR, "Müşteri bulunamadı ID: " + order.getCustomerID());
                }
            }
            mainGUI.updateProductTable();
            updatePendingOrders();

            Logger.log(LogType.INFO,
                    String.format("%d adet sipariş toplu olarak onaylandı", selectedOrders.size()));

            showAlert(
                    String.format("%d adet sipariş başarıyla onaylandı.", selectedOrders.size()),
                    Alert.AlertType.INFORMATION
            );

        }  catch (Exception e) {
            Logger.log(LogType.ERROR, "Toplu sipariş onaylama hatası: " + e.getMessage());
            showAlert("Sipariş onaylama sırasında hata oluştu.", Alert.AlertType.ERROR);
        } finally {
            adminLock.release();
        }
    }*/
    private void showAlert(String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(type == Alert.AlertType.ERROR ? "Hata" : "Bilgi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    private void updateTables() {

        List<Product> products = StockManager.getInstance().getAllProducts();
        productTable.setItems(FXCollections.observableArrayList(products));

        List<OrderManager.Order> pendingOrders = OrderManager.getInstance().getPendingOrders();
        pendingOrdersTable.setItems(FXCollections.observableArrayList(pendingOrders));
    }
   /* public void updateLogArea(String logMessage) {
        Platform.runLater(() -> {
            logArea.appendText(logMessage + "\n");
            // Otomatik kaydırma
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }*/


    private void startPeriodicUpdate() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::updateTables);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

   /* private void updatePendingOrders() {
        List<OrderManager.Order> pendingOrders = OrderManager.getInstance().getPendingOrders();
        Platform.runLater(() ->
                pendingOrdersTable.setItems(FXCollections.observableArrayList(pendingOrders)));
    }*/

    public static Semaphore getAdminLock() {
        return adminLock;
    }
}