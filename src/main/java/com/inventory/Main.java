package com.inventory;

import com.inventory.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load login first; LoginController will switch to main-view.fxml on success
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/inventory/view/Login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Inventory Management System");
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}