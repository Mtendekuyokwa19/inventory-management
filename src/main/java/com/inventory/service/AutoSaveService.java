package com.inventory.service;

import com.inventory.dao.InventoryDAO;
import com.inventory.model.Item;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AutoSaveService handles background saving of inventory data.
 * Saves automatically every 30 seconds when data has changed.
 * Runs on a separate thread to prevent UI freezing.
 */
public class AutoSaveService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoSaveTask;
    private final InventoryDAO dao;
    private final Consumer<String> statusUpdater;
    private final Runnable onSaveComplete;
    private boolean isRunning = false;

    /**
     * Constructor
     * @param dao Data access object for saving
     * @param statusUpdater Function to update status bar
     * @param onSaveComplete Callback after save completes
     */
    public AutoSaveService(InventoryDAO dao, Consumer<String> statusUpdater, Runnable onSaveComplete) {
        this.dao = dao;
        this.statusUpdater = statusUpdater;
        this.onSaveComplete = onSaveComplete;
    }

    /**
     * Start the auto-save service
     * @param intervalSeconds How often to save (seconds)
     */
    public void start(int intervalSeconds) {
        if (isRunning) {
            return;
        }

        autoSaveTask = scheduler.scheduleAtFixedRate(() -> {
            if (onSaveComplete != null) {
                Platform.runLater(() -> {
                    onSaveComplete.run();
                    if (statusUpdater != null) {
                        statusUpdater.accept("Auto-saved at " + getCurrentTime());
                    }
                });
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        isRunning = true;
        if (statusUpdater != null) {
            Platform.runLater(() -> statusUpdater.accept("Auto-save service started (every " + intervalSeconds + "s)"));
        }
    }

    /**
     * Stop the auto-save service
     */
    public void stop() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel(false);
        }
        scheduler.shutdown();
        isRunning = false;
        if (statusUpdater != null) {
            Platform.runLater(() -> statusUpdater.accept("Auto-save service stopped"));
        }
    }

    /**
     * Perform an immediate manual save
     */
    public boolean saveNow(List<Item> items) {
        boolean success = dao.saveAllItems(items);
        if (statusUpdater != null) {
            Platform.runLater(() -> {
                if (success) {
                    statusUpdater.accept("Manually saved at " + getCurrentTime());
                } else {
                    statusUpdater.accept("Manual save failed!");
                }
            });
        }
        return success;
    }

    private String getCurrentTime() {
        return java.time.LocalTime.now().toString().substring(0, 8);
    }

    public boolean isRunning() {
        return isRunning;
    }
}