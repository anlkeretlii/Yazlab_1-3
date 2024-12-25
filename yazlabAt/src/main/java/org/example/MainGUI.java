package org.example;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainGUI extends Application {
    private static MainGUI instance;
    private TableView<Customer> customerTable;
    private TableView<Product> productTable;
    private VBox logPanel;
    private TextFlow logArea;
    private TableView<OrderManager.Order> pendingOrdersTable;
    private ScrollPane logScrollPane;
    private StockVisualizationPanel stockVisualizationPanel;
    private static Scene mainScene;





    public static MainGUI getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) {

        instance = this;
        primaryStage.setTitle("Stok ve Sipariş Yönetim Sistemi");
        BorderPane mainLayout = new BorderPane();
        MenuBar menuBar = createMenuBar();
        mainLayout.setTop(menuBar);
        TabPane tabPane = new TabPane();
        Tab mainTab = createMainTab();
        tabPane.getTabs().add(mainTab);
        mainLayout.setCenter(tabPane);

        Scene scene = new Scene(mainLayout, 1600, 1000);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
        loadInitialData();
    }

    public Scene getMainScene() {
        return mainScene;
    }

    private Tab createMainTab() {
        Tab tab = new Tab("Stok ve Sipariş Yönetim Sistemi");
        tab.setClosable(false);

        // Ana panel içeriği
        BorderPane contentPane = new BorderPane();

        // Sol panel - Müşteriler
        VBox customerSection = createCustomerSection();
        contentPane.setLeft(customerSection);

        // Orta panel - Ürün yönetimi
        VBox productSection = createProductSection();
        contentPane.setCenter(productSection);

        // Sağ panel - Loglar
        VBox logSection = createLogSection();
        contentPane.setRight(logSection);

        tab.setContent(contentPane);
        return tab;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Dosya");
        MenuItem exitItem = new MenuItem("Çıkış");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().add(exitItem);

        Menu viewMenu = new Menu("Görünüm");
        MenuItem refreshItem = new MenuItem("Yenile");
        refreshItem.setOnAction(e -> refreshAllTables());
        viewMenu.getItems().add(refreshItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu);
        return menuBar;
    }

    private VBox createCustomerSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setPrefWidth(550);

        Label title = new Label("Müşteriler");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        customerTable = new TableView<>();
        setupCustomerTable();
        customerTable.setPrefHeight(400);

        stockVisualizationPanel = new StockVisualizationPanel();

        section.getChildren().addAll(
                title,
                customerTable,
                stockVisualizationPanel
        );

        return section;
    }

    private VBox createProductSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setPrefWidth(400);

        Label title = new Label("Ürün Yönetimi");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        // Ürün tablosu
        productTable = new TableView<>();
        setupProductTable();
        productTable.setPrefHeight(300);

        // Buton grubu
        HBox buttonGroup = new HBox(10);
        Button addProductBtn = new Button("Yeni Ürün Ekle");
        Button deleteProductBtn = new Button("Ürün Sil");
        Button updateStockBtn = new Button("Stok Güncelle");

        // Buton olayları
        addProductBtn.setOnAction(e -> showAddProductDialog());
        deleteProductBtn.setOnAction(e -> showDeleteProductDialog());
        updateStockBtn.setOnAction(e -> showUpdateStockDialog());

        buttonGroup.getChildren().addAll(addProductBtn, deleteProductBtn, updateStockBtn);

        // Bekleyen siparişler
        Label ordersTitle = new Label("Bekleyen Siparişler");
        ordersTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");
        setupOrdersTable();
        HBox bulkActionButtons = new HBox(10);
        bulkActionButtons.setPadding(new Insets(5));

        Button approveSelectedButton = new Button("Seçili Siparişleri Onayla");
        approveSelectedButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        approveSelectedButton.setOnAction(e -> {
            ObservableList<OrderManager.Order> selectedOrders = pendingOrdersTable.getSelectionModel().getSelectedItems();
            if (!selectedOrders.isEmpty()) {
                handleBulkApproval(selectedOrders);
            } else {
                showAlert("Lütfen onaylanacak siparişleri seçin", Alert.AlertType.WARNING);
            }
        });

        Button approveAllButton = new Button("Tüm Siparişleri Onayla");
        approveAllButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        approveAllButton.setOnAction(e -> {
            ObservableList<OrderManager.Order> allOrders = pendingOrdersTable.getItems();
            if (!allOrders.isEmpty()) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Onay");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Tüm siparişleri onaylamak istediğinizden emin misiniz?");

                confirmation.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        handleBulkApproval(allOrders);
                    }
                });
            } else {
                showAlert("Onaylanacak sipariş bulunmuyor", Alert.AlertType.INFORMATION);
            }
        });

        bulkActionButtons.getChildren().addAll(approveSelectedButton, approveAllButton);

        section.getChildren().addAll(
                title,
                productTable,
                buttonGroup,
                new Separator(),
                ordersTitle,
                pendingOrdersTable,
                bulkActionButtons
        );

        return section;
    }

    private VBox createLogSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setPrefWidth(500);

        Label title = new Label("Sistem Logları");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold");

        logArea = new TextFlow();
        logArea.setLineSpacing(5);

        logScrollPane = new ScrollPane(logArea);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setPrefViewportHeight(700);
        logScrollPane.setStyle("-fx-background-color: white;");

        Button customerLoginButton = new Button("Müşteri Girişi");
        customerLoginButton.setMaxWidth(Double.MAX_VALUE);
        customerLoginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        customerLoginButton.setOnAction(e -> {
            Stage stage = (Stage) customerLoginButton.getScene().getWindow();
            CustomerLoginDialog dialog = new CustomerLoginDialog(stage);
            dialog.showAndWait();
        });

        section.getChildren().addAll(
                title,
                logScrollPane,
                customerLoginButton
        );
        return section;
    }
    private void setupCustomerTable() {
        TableColumn<Customer, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCustomerID()).asObject());
        idCol.setPrefWidth(50);
        // Tür kolonu
        TableColumn<Customer, String> nameCol = new TableColumn<>("İsim");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCustomerName()));

        TableColumn<Customer, String> typeCol = new TableColumn<>("Tür");
        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getType().toString()));

        // Bütçe kolonu
        TableColumn<Customer, Double> budgetCol = new TableColumn<>("Bütçe");
        budgetCol.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getBudget()).asObject());

        // Toplam Harcama kolonu
        TableColumn<Customer, Double> spentCol = new TableColumn<>("Toplam H.");
        spentCol.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getTotalSpent()).asObject());

        // Durum kolonu
        TableColumn<Customer, String> statusCol = new TableColumn<>("Durum");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty("Beklemede"));

        nameCol.setMinWidth(150);
        nameCol.setMaxWidth(300);

        // Para birimlerini formatlı göster
        budgetCol.setCellFactory(column -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f TL", item));
                }
            }
        });

        spentCol.setCellFactory(column -> new TableCell<Customer, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f TL", item));
                }
            }
        });
        customerTable.getColumns().clear();
        customerTable.getColumns().addAll(idCol, nameCol, typeCol, budgetCol, spentCol, statusCol);
    }
    private void setupProductTable() {
        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        TableColumn<Product, String> nameCol = new TableColumn<>("Ürün Adı");
        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stok");
        TableColumn<Product, Double> priceCol = new TableColumn<>("Fiyat");

        idCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getProductID()).asObject());
        nameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getProductName()));
        stockCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getStock()).asObject());
        priceCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrice()).asObject());

        productTable.getColumns().addAll(idCol, nameCol, stockCol, priceCol);
    }
    private void setupOrdersTable() {
        pendingOrdersTable = new TableView<>();
        pendingOrdersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Tooltip selectionTip = new Tooltip(
                "Çoklu seçim için:\n" +
                        "- Ctrl (veya ⌘) tuşunu basılı tutarak tek tek seçin\n" +
                        "- Shift tuşunu basılı tutarak aralık seçin"
        );
        pendingOrdersTable.setTooltip(selectionTip);
        // Seçili satırları vurgulama
        pendingOrdersTable.setRowFactory(tv -> {
            TableRow<OrderManager.Order> row = new TableRow<>() {
                @Override
                protected void updateItem(OrderManager.Order order, boolean empty) {
                    super.updateItem(order, empty);
                    if (empty || order == null) {
                        setStyle("");
                    } else {
                        Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
                        if (customer != null && customer.getType() == Customer.CustomerType.PREMIUM) {
                            // Premium müşteri için stil
                            setStyle("-fx-background-color: #fff3e0;");
                        } else {
                            setStyle("");
                        }
                        // Seçili durum için stil
                        if (getTableView().getSelectionModel().getSelectedItems().contains(order)) {
                            setStyle(getStyle() + "; -fx-border-color: #1976d2; -fx-border-width: 1;");
                        }
                    }
                }
            };
            // Seçim değişikliğinde stili değiştirme
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                OrderManager.Order order = row.getItem();
                if (order != null) {
                    Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
                    if (isSelected) {
                        String baseStyle = (customer != null && customer.getType() == Customer.CustomerType.PREMIUM)
                                ? "-fx-background-color: #fff3e0;"
                                : "";
                        row.setStyle(baseStyle + "-fx-border-color: #1976d2; -fx-border-width: 1;");
                    } else {
                        row.setStyle((customer != null && customer.getType() == Customer.CustomerType.PREMIUM)
                                ? "-fx-background-color: #fff3e0;"
                                : "");
                    }
                }
            });

            return row;
        });
        //CheckBox kolonu
        TableColumn<OrderManager.Order, Boolean> selectCol = new TableColumn<>("");
        selectCol.setPrefWidth(30);
        selectCol.setCellValueFactory(param -> new SimpleBooleanProperty(
                pendingOrdersTable.getSelectionModel().getSelectedItems().contains(param.getValue())
        ));
        selectCol.setCellFactory(param -> new CheckBoxTableCell<>());

        // Sipariş ID kolonu
        TableColumn<OrderManager.Order, Integer> orderIdCol = new TableColumn<>("Sipariş ID");
        orderIdCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getOrderId()).asObject());

        // Müşteri ID kolonu
        TableColumn<OrderManager.Order, Integer> customerIdCol = new TableColumn<>("Müşteri ID");
        customerIdCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCustomerID()).asObject());

        // Ürün kolonu
        TableColumn<OrderManager.Order, String> productCol = new TableColumn<>("Ürün");
        productCol.setCellValueFactory(data -> {
            Product product = StockManager.getInstance().getProduct(data.getValue().getProductID());
            return new SimpleStringProperty(product != null ? product.getProductName() : "Unknown");
        });

        // Miktar kolonu
        TableColumn<OrderManager.Order, Integer> quantityCol = new TableColumn<>("Miktar");
        quantityCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantity()).asObject());

        // Öncelik kolonu
        TableColumn<OrderManager.Order, Double> priorityCol = new TableColumn<>("Öncelik");
        priorityCol.setCellValueFactory(data -> {
            OrderManager.Order order = data.getValue();
            double priority = OrderManager.getInstance().getOrderPriority(order);
            return new SimpleDoubleProperty(priority).asObject();
        });

        priorityCol.setCellFactory(column -> new TableCell<OrderManager.Order, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", item));
                    OrderManager.Order order = getTableView().getItems().get(getIndex());
                    Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
                    if (customer != null && customer.getType() == Customer.CustomerType.PREMIUM) {
                        setTextFill(Color.web("#e65100")); // Premium müşteri rengi
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.BLACK);
                        setStyle("");
                    }
                }
            }
        });
        orderIdCol.setCellFactory(column -> new TableCell<OrderManager.Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    OrderManager.Order order = getTableRow().getItem();
                    if (order != null && order.isManualOrder()) {
                        setStyle("-fx-background-color: #E8F5E9;");
                        setTextFill(javafx.scene.paint.Color.web("#2E7D32"));
                    } else {
                        setStyle("");
                        setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                }
            }
        });
        // İşlem kolonu
        TableColumn<OrderManager.Order, String> actionCol = new TableColumn<>("İşlem");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button onaylaButton = new Button("Onayla");

            {
                onaylaButton.setOnAction(event -> {
                    OrderManager.Order order = getTableView().getItems().get(getIndex());
                    Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
                    if (customer != null) {
                        OrderManager.getInstance().processOrder(order, customer);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(onaylaButton);
                }
            }
        });
        customerIdCol.setCellFactory(column -> createStyledCell());
        productCol.setCellFactory(column -> createStyledCell());
        quantityCol.setCellFactory(column -> createStyledCell());
        pendingOrdersTable.getColumns().addAll(
                selectCol, orderIdCol, customerIdCol, productCol,
                quantityCol, priorityCol, actionCol
        );
        pendingOrdersTable.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<OrderManager.Order>) change -> {
                    int selectedCount = pendingOrdersTable.getSelectionModel().getSelectedItems().size();
                    Platform.runLater(() -> updateSelectionButtonText(selectedCount));
                });

        pendingOrdersTable.setFocusTraversable(true);
        pendingOrdersTable.setFocusModel(new TableView.TableViewFocusModel<>(pendingOrdersTable));

        VBox bulkActionButtons = new VBox(5);
        bulkActionButtons.setPadding(new Insets(5));

        // Seçili siparişleri onayla butonu
        Button approveSelectedButton = new Button("Seçili Siparişleri Onayla");
        approveSelectedButton.setMaxWidth(Double.MAX_VALUE);
        approveSelectedButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        approveSelectedButton.setOnAction(e -> {
            ObservableList<OrderManager.Order> selectedOrders = pendingOrdersTable.getSelectionModel().getSelectedItems();
            if (!selectedOrders.isEmpty()) {
                handleBulkApproval(selectedOrders);
            } else {
                showAlert("Lütfen onaylanacak siparişleri seçin", Alert.AlertType.WARNING);
            }
        });

        // Tüm siparişleri onayla butonu
        Button approveAllButton = new Button("Tüm Siparişleri Onayla");
        approveAllButton.setMaxWidth(Double.MAX_VALUE);
        approveAllButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        approveAllButton.setOnAction(e -> {
            ObservableList<OrderManager.Order> allOrders = pendingOrdersTable.getItems();
            if (!allOrders.isEmpty()) {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Onay");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Tüm siparişleri onaylamak istediğinizden emin misiniz?");

                confirmation.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        handleBulkApproval(allOrders);
                    }
                });
            } else {
                showAlert("Onaylanacak sipariş bulunmuyor", Alert.AlertType.INFORMATION);
            }
        });

        // Butonları VBox'a ekle
        bulkActionButtons.getChildren().addAll(approveSelectedButton, approveAllButton);

        // Tablo ve butonları içeren ana container
        VBox mainContainer = new VBox(5);
        mainContainer.getChildren().addAll(pendingOrdersTable, bulkActionButtons);

        // Periyodik güncelleme başlat
        startOrderTableUpdate();
    }
    public TableView<OrderManager.Order> getPendingOrdersTable() {
        return pendingOrdersTable;
    }

    private <T> TableCell<OrderManager.Order, T> createStyledCell() {
        return new TableCell<OrderManager.Order, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    OrderManager.Order order = getTableRow().getItem();
                    if (order != null && order.isManualOrder()) {
                        setStyle("-fx-background-color: #E8F5E9;"); // Açık yeşil arka plan
                        setTextFill(javafx.scene.paint.Color.web("#2E7D32")); // Koyu yeşil yazı
                    } else {
                        setStyle("");
                        setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                }
            }
        };
    }
    private void updateSelectionButtonText(int selectedCount) {
        Node parent = pendingOrdersTable.getParent();
        if (parent instanceof VBox) {
            VBox vbox = (VBox) parent;
            for (Node node : vbox.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    for (Node button : hbox.getChildren()) {
                        if (button instanceof Button && ((Button) button).getText().contains("Seçili")) {
                            String baseText = "Seçili Siparişleri Onayla";
                            ((Button) button).setText(selectedCount > 0 ?
                                    String.format("%s (%d)", baseText, selectedCount) : baseText);
                        }
                    }
                }
            }
        }
    }

    private void handleBulkApproval(List<OrderManager.Order> orders) {
        try {
            AdminPanel.getAdminLock().acquire();
            int successCount = 0;

            for (OrderManager.Order order : orders) {
                Customer customer = DatabaseManager.getInstance().getCustomer(order.getCustomerID());
                if (customer != null) {
                    OrderManager.getInstance().processOrder(order, customer);
                    successCount++;
                }
            }

            int finalSuccessCount = successCount;
            Platform.runLater(() -> {
                String message = String.format("%d sipariş başarıyla onaylandı.", finalSuccessCount);
                if (finalSuccessCount < orders.size()) {
                    message += String.format("\n%d sipariş işlenemedi.", orders.size() - finalSuccessCount);
                }
                showAlert(message, Alert.AlertType.INFORMATION);
            });

        } catch (InterruptedException e) {
            Logger.log(LogType.ERROR, "Toplu sipariş onaylama hatası: " + e.getMessage());
            showAlert("Sipariş onaylama sırasında hata oluştu", Alert.AlertType.ERROR);
        } finally {
            AdminPanel.getAdminLock().release();
        }
    }

    private void startOrderTableUpdate() {
        // Tabloyu her saniye güncelle
        Timeline updateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updatePendingOrdersTable();
        }));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }
    public void updatePendingOrdersTable() {
        try {
            OrderManager orderManager = OrderManager.getInstance();
            List<OrderManager.Order> pendingOrders = orderManager.getPendingOrders();

            Platform.runLater(() -> {
                try {
                    // Mevcut seçimleri koru
                    List<Integer> selectedOrderIds = new ArrayList<>();
                    for (OrderManager.Order selected : pendingOrdersTable.getSelectionModel().getSelectedItems()) {
                        selectedOrderIds.add(selected.getOrderId());
                    }

                    // Siparişleri öncelik skorlarına göre sırala
                    pendingOrders.sort((o1, o2) -> {
                        double score1 = orderManager.getOrderPriority(o1);
                        double score2 = orderManager.getOrderPriority(o2);
                        return Double.compare(score2, score1);
                    });

                    pendingOrdersTable.setItems(FXCollections.observableArrayList(pendingOrders));
                    for (OrderManager.Order order : pendingOrders) {
                        if (selectedOrderIds.contains(order.getOrderId())) {
                            pendingOrdersTable.getSelectionModel().select(order);
                        }
                    }
                    pendingOrdersTable.refresh();

                } catch (Exception e) {
                    Logger.log(LogType.ERROR, "Sipariş tablosu güncelleme hatası: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Sipariş listesi alınamadı: " + e.getMessage());
        }
    }

    private void loadInitialData() {
        // Müşteri tablosunu güncelle
        customerTable.setItems(FXCollections.observableArrayList(
                DatabaseManager.getInstance().getAllCustomers()
        ));
        // Ürün tablosunu güncelle
        productTable.setItems(FXCollections.observableArrayList(
                DatabaseManager.getInstance().getAllProducts()
        ));
    }
    private void refreshAllTables() {
        loadInitialData();
    }
    public void updateLogPanel(String logMessage) {
        Platform.runLater(() -> {
            Text messageText = new Text(logMessage + "\n");
            messageText.setFill(Color.BLACK);
            logArea.getChildren().add(messageText);
            // Auto-scroll düzeltmesi
            logScrollPane.setVvalue(1.0);
        });
    }

    public void updateLogPanel(ColorLogger.ColorLogEntry logEntry) {
        Platform.runLater(() -> {
            Text timeText = new Text("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ");
            timeText.setFill(Color.GRAY);

            Text typeText = new Text(logEntry.getType().getDescription() + ": ");

            Text messageText = new Text(logEntry.getMessage() + "\n");
            messageText.setFill(logEntry.getColor());

            logArea.getChildren().addAll(timeText, typeText, messageText);
            logScrollPane.setVvalue(1.0);
        });
    }

    public void updateCustomerTable() {
        Platform.runLater(() -> {
            List<Customer> customers = DatabaseManager.getInstance().getAllCustomers();
            customerTable.setItems(FXCollections.observableArrayList(customers));
            customerTable.refresh();
        });
    }

    public void updateProductTable() {
        Platform.runLater(() -> {
            List<Product> products = StockManager.getInstance().getAllProducts();
            productTable.getItems().clear();
            productTable.setItems(FXCollections.observableArrayList(products));
            productTable.refresh();

            // Debug için log
            System.out.println("Ürün tablosu güncellendi. Ürün sayısı: " + products.size());
        });
    }

    private void showAddProductDialog() {
        try {
            AdminPanel.getAdminLock().acquire(); // Admin işlemlerinde thread durdurma

            Dialog<Product> dialog = new Dialog<>();
            dialog.setTitle("Yeni Ürün Ekle");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField nameField = new TextField();
            TextField stockField = new TextField();
            TextField priceField = new TextField();

            grid.add(new Label("Ürün Adı:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Stok:"), 0, 1);
            grid.add(stockField, 1, 1);
            grid.add(new Label("Fiyat:"), 0, 2);
            grid.add(priceField, 1, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    try {
                        String name = nameField.getText();
                        int stock = Integer.parseInt(stockField.getText());
                        double price = Double.parseDouble(priceField.getText());

                        int newId = DatabaseManager.getInstance().generateNextProductId();
                        Product newProduct = new Product(newId, name, stock, price);
                        StockManager.getInstance().addProduct(newProduct);
                        Platform.runLater(() -> {
                            updateProductTable();
                            showAlert("Ürün başarıyla eklendi", Alert.AlertType.INFORMATION);
                        });
                        return newProduct;
                    } catch (NumberFormatException e) {
                        showAlert("Geçersiz sayısal değer", Alert.AlertType.ERROR);
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            AdminPanel.getAdminLock().release();
        }
    }

    // Ürün silme dialog penceresi
    private void showDeleteProductDialog() {
        try {
            AdminPanel.getAdminLock().acquire(); // Admin kilidi al

            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct == null) {
                showAlert("Lütfen silinecek ürünü seçin", Alert.AlertType.WARNING);
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ürün Silme Onayı");
            alert.setHeaderText(null);
            alert.setContentText(selectedProduct.getProductName() + " ürününü silmek istediğinize emin misiniz?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    StockManager.getInstance().removeProduct(selectedProduct.getProductID());
                    updateProductTable();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            AdminPanel.getAdminLock().release(); // Admin kilidini serbest bırak threadler çalışır
        }
    }

    private void showUpdateStockDialog() {
        try {
            AdminPanel.getAdminLock().acquire(); // Admin kilidi al

            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct == null) {
                showAlert("Lütfen güncellenecek ürünü seçin", Alert.AlertType.WARNING);
                return;
            }

            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Stok Güncelle");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField stockField = new TextField(String.valueOf(selectedProduct.getStock()));
            grid.add(new Label("Yeni Stok Miktarı:"), 0, 0);
            grid.add(stockField, 1, 0);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    try {
                        int newStock = Integer.parseInt(stockField.getText());
                        if (newStock >= 0) {
                            StockManager.getInstance().updateProductStock(
                                    selectedProduct.getProductID(), newStock);

                            // GUI güncellemesini zorla
                            Platform.runLater(() -> {
                                updateProductTable();
                                showAlert("Stok başarıyla güncellendi", Alert.AlertType.INFORMATION);
                            });

                            return newStock;
                        } else {
                            showAlert("Stok miktarı negatif olamaz", Alert.AlertType.ERROR);
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Geçersiz stok miktarı", Alert.AlertType.ERROR);
                    }
                }
                return null;
            });

            dialog.showAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            AdminPanel.getAdminLock().release();
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Hata" : "Uyarı");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
