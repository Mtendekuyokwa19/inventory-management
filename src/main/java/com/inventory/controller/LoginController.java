package com.inventory.controller;

import com.inventory.service.AuthService;
import com.inventory.util.Validator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());


    @FXML private TextField         emailField;
    @FXML private PasswordField     passwordField;
    @FXML private Button            loginButton;
    @FXML private Hyperlink        registerLink;
    @FXML private Label             errorLabel;
    @FXML private ProgressIndicator loadingIndicator;

  
    private final AuthService authService = new AuthService();

    
    @FXML
    public void initialize() {
        // Allow pressing Enter in either field to trigger login
        emailField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());

        loadingIndicator.setVisible(false);
        errorLabel.setText("");
    }

  

    @FXML
    public void handleLogin() {
        String email    = emailField.getText();
        String password = passwordField.getText();

        // --- Client-side validation -------------------------------------
        if (!Validator.isNotEmpty(email) || !Validator.isNotEmpty(password)) {
            showError("Please fill in all fields.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }

        // --- Background authentication (UI must never freeze) -----------
        setLoading(true);

        Task<Boolean> authTask = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.authenticate(email, password).isPresent();
            }
        };

        authTask.setOnSucceeded(event -> {
            setLoading(false);
            if (authTask.getValue()) {
                navigateToMainView();
            } else {
                showError("Invalid email or password.");
            }
        });

        authTask.setOnFailed(event -> {
            setLoading(false);
            LOGGER.log(Level.SEVERE, "Authentication task failed unexpectedly.",
                    authTask.getException());
            showError("An unexpected error occurred. Please try again.");
        });

        Thread thread = new Thread(authTask);
        thread.setDaemon(true);
        thread.start();
    }

   

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            loginButton.setDisable(loading);
            emailField.setDisable(loading);
            passwordField.setDisable(loading);
            loadingIndicator.setVisible(loading);
            if (loading) {
                errorLabel.setText("");
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> errorLabel.setText(message));
    }

  
    private void navigateToMainView() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/inventory/view/main-view.fxml"));
                Parent root = loader.load();

                // Let MainController finish its own initialisation
                MainController mainController = loader.getController();

                Stage stage = (Stage) loginButton.getScene().getWindow();

                Scene scene = new Scene(root, 1000, 600);
                stage.setScene(scene);
                stage.setTitle("Inventory Management System");
                stage.setMinWidth(800);
                stage.setMinHeight(500);
                stage.show();

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load main-view.fxml", e);
                showError("Failed to open the application. Please restart.");
            }
        });
    }

    @FXML
    public void handleRegisterLink() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/inventory/view/Registration.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) registerLink.getScene().getWindow();

                Scene scene = new Scene(root, 420, 520);
                stage.setScene(scene);
                stage.setTitle("Inventory Manager - Create Account");
                stage.setMinWidth(380);
                stage.setMinHeight(450);
                stage.show();

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load Registration.fxml", e);
                showError("Failed to open registration. Please try again.");
            }
        });
    }
}
