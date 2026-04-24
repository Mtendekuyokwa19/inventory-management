package com.inventory.util;

import com.inventory.model.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Validator class for validating inventory items before saving.
 * Prevents invalid data from corrupting the inventory file.
 */
public class Validator {

    /**
     * Validate Item ID
     * Rules:
     * - Cannot be empty
     * - Cannot contain pipe character (|) - breaks file format
     * - Must be unique in the inventory
     *
     * @param id The ID to validate
     * @param existingItems List of existing items to check uniqueness
     * @return Optional empty if valid, or error message if invalid
     */
    public static Optional<String> validateId(String id, List<Item> existingItems) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.of(" ID cannot be empty");
        }

        if (id.contains("|")) {
            return Optional.of(" ID cannot contain pipe character '|'");
        }

        if (id.contains(" ")) {
            return Optional.of("ID cannot contain spaces (use underscores or dashes)");
        }

        if (existingItems != null && existingItems.stream().anyMatch(item -> item.getId().equalsIgnoreCase(id))) {
            return Optional.of(" ID already exists: " + id);
        }

        return Optional.empty();
    }

    /**
     * Validate Item Name
     * Rules:
     * - Cannot be empty
     * - Cannot contain pipe character (|) - breaks file format
     *
     * @param name The name to validate
     * @return Optional empty if valid, or error message if invalid
     */
    public static Optional<String> validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.of(" Name cannot be empty");
        }

        if (name.contains("|")) {
            return Optional.of(" Name cannot contain pipe character '|'");
        }

        return Optional.empty();
    }

    /**
     * Validate Item Quantity
     * Rules:
     * - Cannot be negative
     * - Must be a valid integer
     *
     * @param quantity The quantity to validate
     * @return Optional empty if valid, or error message if invalid
     */
    public static Optional<String> validateQuantity(int quantity) {
        if (quantity < 0) {
            return Optional.of(" Quantity cannot be negative");
        }

        if (quantity > 999999) {
            return Optional.of(" Quantity too high (max 999,999)");
        }

        return Optional.empty();
    }

    /**
     * Validate Item Price
     * Rules:
     * - Cannot be null
     * - Cannot be negative
     * - Maximum reasonable price
     *
     * @param price The price to validate
     * @return Optional empty if valid, or error message if invalid
     */
    public static Optional<String> validatePrice(BigDecimal price) {
        if (price == null) {
            return Optional.of(" Price cannot be empty");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of(" Price cannot be negative");
        }

        if (price.compareTo(new BigDecimal("10000000")) > 0) {
            return Optional.of(" Price too high (max 10,000,000)");
        }

        // Check for too many decimal places
        if (price.scale() > 2) {
            return Optional.of(" Price can only have 2 decimal places");
        }

        return Optional.empty();
    }

    /**
     * Validate full Item (all fields)
     *
     * @param item The item to validate
     * @param existingItems Existing items for ID uniqueness check
     * @return Optional empty if valid, or error message if invalid
     */
    public static Optional<String> validateItem(Item item, List<Item> existingItems) {
        // Skip ID uniqueness check for existing item (editing)
        Optional<String> idError = validateId(item.getId(), null);
        if (idError.isPresent()) {
            return idError;
        }

        Optional<String> nameError = validateName(item.getName());
        if (nameError.isPresent()) {
            return nameError;
        }

        Optional<String> qtyError = validateQuantity(item.getQuantity());
        if (qtyError.isPresent()) {
            return qtyError;
        }

        Optional<String> priceError = validatePrice(item.getPrice());
        if (priceError.isPresent()) {
            return priceError;
        }

        return Optional.empty();
    }

    /**
     * Check if ID is unique (for adding new items)
     */
    public static boolean isUniqueId(String id, List<Item> existingItems) {
        return existingItems.stream().noneMatch(item -> item.getId().equalsIgnoreCase(id));
    }

    /**
     * Generate a new unique ID based on existing items
     * Format: INV001, INV002, etc.
     */
    public static String generateUniqueId(List<Item> existingItems) {
        int maxNum = 0;
        for (Item item : existingItems) {
            String id = item.getId();
            if (id.startsWith("INV")) {
                try {
                    int num = Integer.parseInt(id.substring(3));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-numeric IDs
                }
            }
        }
        return String.format("INV%03d", maxNum + 1);
    }
}