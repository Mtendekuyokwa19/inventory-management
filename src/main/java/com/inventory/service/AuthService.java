package com.inventory.service;

import com.inventory.model.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final String USERS_FILE = "data/users.txt";
    private static final String DELIMITER = "\\|";
    private static final int EXPECTED_FIELDS = 2;

    public Optional<User> authenticate(String email, String password) {
        if (email == null || password == null
                || email.isBlank() || password.isBlank()) {
            return Optional.empty();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Optional<User> candidate = parseLine(line);
                if (candidate.isPresent()) {
                    User user = candidate.get();
                    if (user.getEmail().equalsIgnoreCase(email.trim())
                            && user.getPassword().equals(password)) {
                        return Optional.of(user);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Could not read users file: " + USERS_FILE, e);
        }

        return Optional.empty();
    }

    /**
     * Parses a single pipe-delimited line into a User.
     * Returns Optional.empty() for blank lines or lines with wrong format,
     * so the caller can skip them without crashing.
     */
    private Optional<User> parseLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        String[] parts = line.split(DELIMITER, -1);
        if (parts.length != EXPECTED_FIELDS) {
            LOGGER.log(Level.FINE, "Skipping malformed line in users file.");
            return Optional.empty();
        }
        String storedEmail    = parts[0].trim();
        String storedPassword = parts[1].trim();
        if (storedEmail.isBlank() || storedPassword.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new User(storedEmail, storedPassword));
    }
}