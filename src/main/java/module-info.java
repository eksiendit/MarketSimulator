module com.example.marketsimulator {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.marketsimulator to javafx.fxml;
    exports com.example.marketsimulator;
}