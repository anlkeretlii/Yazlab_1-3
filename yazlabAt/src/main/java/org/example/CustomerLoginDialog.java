package org.example;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import java.util.List;

public class CustomerLoginDialog extends Dialog<Customer> {
    private final ComboBox<Product> productComboBox;
    private final Spinner<Integer> quantitySpinner;
    private final VBox customerButtonsBox;
    private Customer selectedCustomer;
    private final Button orderButton;

    public CustomerLoginDialog(Stage owner) {
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        setTitle("Müşteri Girişi ve Manuel Sipariş");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label customerLabel = new Label("Müşteri Seçimi:");
        customerLabel.setStyle("-fx-font-weight: bold");

        customerButtonsBox = new VBox(5);
        updateCustomerButtons();

        GridPane orderForm = new GridPane();
        orderForm.setHgap(10);
        orderForm.setVgap(10);
        orderForm.setPadding(new Insets(10, 0, 0, 0));

        Label productLabel = new Label("Ürün:");
        productComboBox = new ComboBox<>();
        productComboBox.setMaxWidth(Double.MAX_VALUE);
        updateProductComboBox();

        // Ürün seçim değişikliğini dinle
        productComboBox.setOnAction(e -> checkOrderButtonState());

        Label quantityLabel = new Label("Miktar:");
        quantitySpinner = new Spinner<>(1, 5, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(100);

        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> checkOrderButtonState());
        //grid
        orderForm.add(productLabel, 0, 0);
        orderForm.add(productComboBox, 1, 0);
        orderForm.add(quantityLabel, 0, 1);
        orderForm.add(quantitySpinner, 1, 1);

        orderButton = new Button("Sipariş Ver");
        orderButton.setMaxWidth(Double.MAX_VALUE);
        orderButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        orderButton.setDisable(true); // Başlangıçta devre dışı
        orderButton.setOnAction(e -> placeOrder());

        content.getChildren().addAll(
                customerLabel,
                customerButtonsBox,
                new Separator(),
                orderForm,
                orderButton
        );

        // Dialog ayarları
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        setResultConverter(buttonType -> selectedCustomer);
    }

    private void checkOrderButtonState() {
        boolean isEnabled = selectedCustomer != null &&
                productComboBox.getValue() != null &&
                quantitySpinner.getValue() != null &&
                quantitySpinner.getValue() > 0;

        if (isEnabled) {
            // Bütçe kontrolü
            Product selectedProduct = productComboBox.getValue();
            double totalCost = selectedProduct.getPrice() * quantitySpinner.getValue();
            isEnabled = selectedCustomer.getBudget() >= totalCost;
        }

        orderButton.setDisable(!isEnabled);
    }

    private void updateCustomerButtons() {
        customerButtonsBox.getChildren().clear();
        List<Customer> customers = DatabaseManager.getInstance().getAllCustomers();

        for (Customer customer : customers) {
            Button customerButton = new Button(
                    String.format("%s (%s) - Bütçe: %.2f TL",
                            customer.getCustomerName(),
                            customer.getType(),
                            customer.getBudget()
                    )
            );

            customerButton.setMaxWidth(Double.MAX_VALUE);
            customerButton.setStyle("-fx-alignment: center-left;");

            customerButton.setOnAction(e -> {
                selectedCustomer = customer;

                customerButtonsBox.getChildren().forEach(node -> {
                    if (node instanceof Button) {
                        ((Button) node).setStyle("-fx-alignment: center-left;");
                    }
                });

                customerButton.setStyle(
                        "-fx-alignment: center-left; " +
                                "-fx-background-color: #4CAF50; " +
                                "-fx-text-fill: white;"
                );
                checkOrderButtonState();
            });

            customerButtonsBox.getChildren().add(customerButton);
        }
    }

    private void updateProductComboBox() {
        List<Product> products = StockManager.getInstance().getAllProducts();
        productComboBox.getItems().clear();
        for (Product product : products) {
            if (product.getStock() > 0) {
                productComboBox.getItems().add(product);
            }
        }

        // Ürün gösterim formatını ayarla
        productComboBox.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                if (product == null) return "";
                return String.format("%s - Stok: %d - Fiyat: %.2f TL",
                        product.getProductName(),
                        product.getStock(),
                        product.getPrice()
                );
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        });
    }

    private void placeOrder() {
        if (selectedCustomer == null || productComboBox.getValue() == null) {
            showAlert("Lütfen müşteri ve ürün seçin.", Alert.AlertType.WARNING);
            return;
        }

        Product selectedProduct = productComboBox.getValue();
        int quantity = quantitySpinner.getValue();
        double totalCost = selectedProduct.getPrice() * quantity;

        if (selectedCustomer.getBudget() < totalCost) {
            showAlert("Yetersiz bütçe!", Alert.AlertType.ERROR);
            return;
        }

        OrderManager.Order order = OrderManager.getInstance().placeOrder(
                selectedCustomer,
                selectedProduct,
                quantity,
                true
        );

        if (order != null) {
            // Log oluştur
            Logger.log(LogType.INFO, selectedCustomer.getCustomerID(),
                    String.format("Manuel sipariş oluşturuldu - Müşteri: %s, Ürün: %s, Miktar: %d",
                            selectedCustomer.getCustomerName(),
                            selectedProduct.getProductName(),
                            quantity)
            );

            showAlert("Sipariş başarıyla oluşturuldu!", Alert.AlertType.INFORMATION);
            close();
        } else {
            showAlert("Sipariş oluşturulurken bir hata oluştu!", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Hata" : type == Alert.AlertType.WARNING ? "Uyarı" : "Bilgi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}