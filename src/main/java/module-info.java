module com.example.inventroymangement {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.inventroymangement to javafx.fxml;
    exports com.example.inventroymangement;
}