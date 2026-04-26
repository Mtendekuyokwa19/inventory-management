package com.inventory.dao;

import com.inventory.model.Item;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InventoryDAO {
    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "inventory.txt";
    private static final String FILE_PATH = DATA_DIR + File.separator + FILE_NAME;
    private static final String TEMP_FILE_PATH = DATA_DIR + File.separator + "inventory.tmp";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public InventoryDAO() {
        createDataDirectoryIfNeeded();
    }

    private void createDataDirectoryIfNeeded() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Load all items from file
    public List<Item> loadAllItems() {
        lock.readLock().lock();
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            List<Item> items = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    Item item = Item.fromFileString(line);
                    if (item != null) {
                        items.add(item);
                    } else {
                        System.err.println("Skipping malformed line: " + line);
                    }
                }
            }
            return items;
        } catch (IOException e) {
            System.err.println("Error loading items: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Save all items to file (atomic write using temp file)
    public boolean saveAllItems(List<Item> items) {
        lock.writeLock().lock();
        try {
            // First write to temp file
            File tempFile = new File(TEMP_FILE_PATH);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                for (Item item : items) {
                    writer.write(item.toFileString());
                    writer.newLine();
                }
                writer.flush();
            }

            // Then rename temp file to actual file (atomic operation)
            File actualFile = new File(FILE_PATH);
            Files.move(tempFile.toPath(), actualFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (IOException e) {
            System.err.println("Error saving items: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Check if ID already exists
    public boolean isIdExists(String id, List<Item> items) {
        return items.stream().anyMatch(item -> item.getId().equalsIgnoreCase(id));
    }
}