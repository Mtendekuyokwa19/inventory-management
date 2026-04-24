package com.inventory.service;




import com.inventory.dao.InventoryDAO;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AutoSaveService {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoSaveTask;
    private final InventoryDAO dao;
    private final Consumer<String> statusUpdater;
    private final Runnable onSaveComplete;

    public AutoSaveService(InventoryDAO dao, Consumer<String> statusUpdater, Runnable onSaveComplete) {
        this.dao = dao;
        this.statusUpdater = statusUpdater;
        this.onSaveComplete = onSaveComplete;
    }

    public void start(int intervalSeconds) {
        autoSaveTask = scheduler.scheduleAtFixedRate(() -> {
            if (onSaveComplete != null) {
                Platform.runLater(onSaveComplete);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
        }
        scheduler.shutdown();
    }
}
