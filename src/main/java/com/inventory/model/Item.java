package com.inventory.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Item {
    private String id;
    private String name;
    private int quantity;
    private BigDecimal price;

    // Constructor
    public Item(String id, String name, int quantity, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }

    // Calculate total value
    public BigDecimal getTotalValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    // Convert to pipe-delimited string for file storage
    public String toFileString() {
        return String.format("%s|%s|%d|%.2f", id, name, quantity, price);
    }

    // Create Item from pipe-delimited string
    public static Item fromFileString(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 4) return null;
        try {
            String id = parts[0].trim();
            String name = parts[1].trim();
            int quantity = Integer.parseInt(parts[2].trim());
            BigDecimal price = new BigDecimal(parts[3].trim());
            if (id.isEmpty() || name.isEmpty() || quantity < 0 || price.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return new Item(id, name, quantity, price);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return toFileString();
    }
}