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

public class RegistrationController {

    private static final Logger LOGGER = Logger.getLogger(RegistrationController.class.getName());
    private static final int MIN_PASSWORD_LENGTH = 4;

    @FXML private TextField         emailField;
    @FXML private PasswordField    passwordField;
    @FXML private Button           registerButton;
    @FXML private Label            errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Hyperlink        loginLink;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        emailField.setOnAction(event -> handleRegister());
        passwordField.setOnAction(event -> handleRegister());

        loadingIndicator.setVisible(false);
        errorLabel.setText("");
    }

    @FXML
    public void handleRegister() {
        String email    = emailField.getText();
        String password = passwordField.getText();

        if (!Validator.isNotEmpty(email) || !Validator.isNotEmpty(password)) {
            showError("Please fill in all fields.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }

        setLoading(true);

        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.register(email.trim(), password);
            }
        };

        registerTask.setOnSucceeded(event -> {
            setLoading(false);
            if (registerTask.getValue()) {
                showSuccess("Account created! Redirecting to login...");
                redirectToLogin();
            } else {
                showError("An account with this email already exists.");
            }
        });

        registerTask.setOnFailed(event -> {
            setLoading(false);
            LOGGER.log(Level.SEVERE, "Registration task failed unexpectedly.",
                    registerTask.getException());
            showError("An unexpected error occurred. Please try again.");
        });

        Thread thread = new Thread(registerTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleLoginLink() {
        redirectToLogin();
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            registerButton.setDisable(loading);
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

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            errorLabel.setStyle("-fx-text-fill: #2e7d32;");
            errorLabel.setText(message);
        });
    }

    private void redirectToLogin() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/inventory/view/Login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) loginLink.getScene().getWindow();

                Scene scene = new Scene(root, 420, 520);
                stage.setScene(scene);
                stage.setTitle("Inventory Manager - Sign In");
                stage.setMinWidth(380);
                stage.setMinHeight(450);
                stage.show();

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load Login.fxml", e);
                showError("Failed to redirect. Please click the login link.");
            }
        });
    }
}