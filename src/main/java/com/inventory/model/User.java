package com.inventory.model;

/**
 * Represents an authenticated user in the Inventory Management System.
 */
public class User {

    private final String email;
    private final String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "User{email='" + email + "'}";
    }
}