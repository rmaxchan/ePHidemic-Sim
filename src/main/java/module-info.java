module com.example.epidemicsim {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.epidemicsim to javafx.fxml;
    exports com.epidemicsim;
}