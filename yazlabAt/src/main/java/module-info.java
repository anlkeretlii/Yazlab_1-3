module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;
    requires org.slf4j;

    // MongoDB için düzeltilmiş modül isimleri
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;

    exports org.example;
    opens org.example to javafx.graphics, javafx.fxml, javafx.base;
}