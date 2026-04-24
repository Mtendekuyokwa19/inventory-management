package com.inventory.controller;

import com.inventory.dao.InventoryDAO;
import com.inventory.model.Item;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainController {

    @FXML private TableView<Item> tableView;
    @FXML private TableColumn<Item, String> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Integer> colQuantity;
    @FXML private TableColumn<Item, BigDecimal> colPrice;
    @FXML private TableColumn<Item, BigDecimal> colTotal;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private ObservableList<Item> masterData = FXCollections.observableArrayList();
    private FilteredList<Item> filteredData;
    private InventoryDAO dao = new InventoryDAO();
    private AtomicBoolean dirty = new AtomicBoolean(false);
    private ScheduledExecutorService autoSaveExecutor;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupEditableTable();
        setupSearchFilter();
        loadData();
        startAutoSaveService();
        updateStatus("Ready");
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalValue"));

        // Format price column
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<BigDecimal>() {
            @Override
            public String toString(BigDecimal object) {
                return object != null ? String.format("%.2f", object) : "";
            }
            @Override
            public BigDecimal fromString(String string) {
                try {
                    return new BigDecimal(string);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        }));

        colTotal.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<BigDecimal>() {
            @Override
            public String toString(BigDecimal object) {
                return object != null ? String.format("%.2f", object) : "";
            }
            @Override
            public BigDecimal fromString(String string) {
                return BigDecimal.ZERO;
            }
        }));
    }

    private void setupEditableTable() {
        tableView.setEditable(true);

        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            markDirty();
        });

        colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantity.setOnEditCommit(event -> {
            if (event.getNewValue() >= 0) {
                event.getRowValue().setQuantity(event.getNewValue());
                markDirty();
                refreshTable();
            }
        });

        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<BigDecimal>() {
            @Override
            public String toString(BigDecimal object) {
                return object != null ? String.format("%.2f", object) : "";
            }
            @Override
            public BigDecimal fromString(String string) {
                try {
                    return new BigDecimal(string);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        }));
        colPrice.setOnEditCommit(event -> {
            if (event.getNewValue().compareTo(BigDecimal.ZERO) >= 0) {
                event.getRowValue().setPrice(event.getNewValue());
                markDirty();
                refreshTable();
            }
        });
    }

    private void setupSearchFilter() {
        filteredData = new FilteredList<>(masterData, p -> true);
        tableView.setItems(filteredData);

        searchField.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerFilter = newValue.toLowerCase();
                return item.getId().toLowerCase().contains(lowerFilter) ||
                        item.getName().toLowerCase().contains(lowerFilter);
            });
        });
    }

    private void loadData() {
        masterData.setAll(dao.loadAllItems());
        dirty.set(false);
        refreshTable();
        updateStatus("Loaded " + masterData.size() + " items");
    }

    private void startAutoSaveService() {
        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        autoSaveExecutor.scheduleAtFixedRate(() -> {
            if (dirty.get()) {
                boolean success = dao.saveAllItems(new ArrayList<>(masterData));
                Platform.runLater(() -> {
                    if (success) {
                        dirty.set(false);
                        updateStatus("Auto-saved at " + java.time.LocalTime.now().toString());
                    } else {
                        updateStatus("Auto-save failed!");
                    }
                });
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @FXML
    private void handleAddItem() {
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Add Item");
        idDialog.setHeaderText("Enter Item ID");
        idDialog.setContentText("ID:");

        Optional<String> idResult = idDialog.showAndWait();
        if (idResult.isPresent() && !idResult.get().trim().isEmpty()) {
            String id = idResult.get().trim();

            if (dao.isIdExists(id, masterData)) {
                showAlert("Error", "ID already exists: " + id);
                return;
            }

            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Add Item");
            nameDialog.setHeaderText("Enter Item Name");
            nameDialog.setContentText("Name:");

            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String name = nameResult.get().trim();

                TextInputDialog qtyDialog = new TextInputDialog();
                qtyDialog.setTitle("Add Item");
                qtyDialog.setHeaderText("Enter Quantity");
                qtyDialog.setContentText("Quantity:");

                Optional<String> qtyResult = qtyDialog.showAndWait();
                if (qtyResult.isPresent()) {
                    try {
                        int qty = Integer.parseInt(qtyResult.get().trim());
                        if (qty < 0) throw new NumberFormatException();

                        TextInputDialog priceDialog = new TextInputDialog();
                        priceDialog.setTitle("Add Item");
                        priceDialog.setHeaderText("Enter Price");
                        priceDialog.setContentText("Price:");

                        Optional<String> priceResult = priceDialog.showAndWait();
                        if (priceResult.isPresent()) {
                            BigDecimal price = new BigDecimal(priceResult.get().trim());
                            if (price.compareTo(BigDecimal.ZERO) >= 0) {
                                Item newItem = new Item(id, name, qty, price);
                                masterData.add(newItem);
                                markDirty();
                                refreshTable();
                                updateStatus("Added item: " + id);
                            }
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Invalid quantity or price");
                    }
                }
            }
        }
    }

    @FXML
    private void handleDeleteItem() {
        Item selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an item to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete item: " + selected.getId());
        confirm.setContentText("Are you sure?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            masterData.remove(selected);
            markDirty();
            refreshTable();
            updateStatus("Deleted item: " + selected.getId());
        }
    }

    @FXML
    private void handleSaveNow() {
        boolean success = dao.saveAllItems(new ArrayList<>(masterData));
        if (success) {
            dirty.set(false);
            updateStatus("Saved manually at " + java.time.LocalTime.now().toString());
        } else {
            updateStatus("Save failed!");
            showAlert("Error", "Failed to save data");
        }
    }

    private void markDirty() {
        dirty.set(true);
        updateStatus("Unsaved changes");
    }

    private void refreshTable() {
        tableView.refresh();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdown();
            try {
                autoSaveExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (dirty.get()) {
            dao.saveAllItems(new ArrayList<>(masterData));
        }
    }
}