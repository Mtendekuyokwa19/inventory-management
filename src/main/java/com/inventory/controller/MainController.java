package com.inventory.controller;

import com.inventory.dao.InventoryDAO;
import com.inventory.model.Item;
import com.inventory.service.AutoSaveService;
import com.inventory.util.Validator;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private AutoSaveService autoSaveService;
    private Item lastDeletedItem;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private volatile String pendingSearch = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupEditableTable();
        setupSearchFilter();
        loadData();

        // Initialize auto-save service
        autoSaveService = new AutoSaveService(dao,
                this::updateStatus,
                this::performAutoSave
        );

        // Start auto-save every 30 seconds
        autoSaveService.start(30);

        updateStatus("Ready - Auto-save every 30 seconds");
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
    }

    private void setupEditableTable() {
        tableView.setEditable(true);

        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(event -> {
            String newName = event.getNewValue();
            Optional<String> error = Validator.validateName(newName);
            if (error.isPresent()) {
                showAlert("Validation Error", error.get());
                tableView.refresh();
            } else {
                event.getRowValue().setName(newName);
                markDirty();
            }
        });

        colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantity.setOnEditCommit(event -> {
            int newQty = event.getNewValue();
            Optional<String> error = Validator.validateQuantity(newQty);
            if (error.isPresent()) {
                showAlert("Validation Error", error.get());
                tableView.refresh();
            } else {
                event.getRowValue().setQuantity(newQty);
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
            BigDecimal newPrice = event.getNewValue();
            Optional<String> error = Validator.validatePrice(newPrice);
            if (error.isPresent()) {
                showAlert("Validation Error", error.get());
                tableView.refresh();
            } else {
                event.getRowValue().setPrice(newPrice);
                markDirty();
                refreshTable();
            }
        });
    }

    private void setupSearchFilter() {
        filteredData = new FilteredList<>(masterData, p -> true);
        tableView.setItems(filteredData);

        searchField.textProperty().addListener((obs, old, newValue) -> {
            String filterText = newValue;
            pendingSearch = filterText;

            searchExecutor.submit(() -> {
                String searchTerm = pendingSearch;
                List<Item> filtered = masterData.stream()
                        .filter(item -> {
                            if (searchTerm == null || searchTerm.isEmpty()) return true;
                            String lowerFilter = searchTerm.toLowerCase();
                            return item.getId().toLowerCase().contains(lowerFilter) ||
                                    item.getName().toLowerCase().contains(lowerFilter);
                        })
                        .collect(Collectors.toList());

                if (searchTerm == pendingSearch) {
                    Platform.runLater(() -> {
                        filteredData.setPredicate(item -> filtered.contains(item));
                        updateStatus("Found " + filtered.size() + " items");
                    });
                }
            });
        });
    }

    private void loadData() {
        masterData.setAll(dao.loadAllItems());
        dirty.set(false);
        refreshTable();
        updateStatus("Loaded " + masterData.size() + " items");
    }

    private void performAutoSave() {
        if (dirty.get()) {
            boolean success = dao.saveAllItems(new ArrayList<>(masterData));
            if (success) {
                dirty.set(false);
            }
        }
    }

    @FXML
    private void handleAddItem() {
        String id = Validator.generateUniqueId(new ArrayList<>(masterData));

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Item");
        nameDialog.setHeaderText("Auto-generated ID: " + id);
        nameDialog.setContentText("Name:");

        Optional<String> nameResult = nameDialog.showAndWait();
        if (!nameResult.isPresent() || nameResult.get().trim().isEmpty()) {
            return;
        }
        String name = nameResult.get().trim();

        Optional<String> nameError = Validator.validateName(name);
        if (nameError.isPresent()) {
            showAlert("Validation Error", nameError.get());
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog();
        qtyDialog.setTitle("Add Item");
        qtyDialog.setHeaderText("Item: " + name);
        qtyDialog.setContentText("Quantity:");

        Optional<String> qtyResult = qtyDialog.showAndWait();
        if (!qtyResult.isPresent()) {
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyResult.get().trim());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid quantity format");
            return;
        }

        Optional<String> qtyError = Validator.validateQuantity(qty);
        if (qtyError.isPresent()) {
            showAlert("Validation Error", qtyError.get());
            return;
        }

        TextInputDialog priceDialog = new TextInputDialog();
        priceDialog.setTitle("Add Item");
        priceDialog.setHeaderText("Item: " + name + " | Qty: " + qty);
        priceDialog.setContentText("Price:");

        Optional<String> priceResult = priceDialog.showAndWait();
        if (!priceResult.isPresent()) {
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceResult.get().trim());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid price format");
            return;
        }

        Optional<String> priceError = Validator.validatePrice(price);
        if (priceError.isPresent()) {
            showAlert("Validation Error", priceError.get());
            return;
        }

        Item newItem = new Item(id, name, qty, price);
        masterData.add(newItem);
        markDirty();
        refreshTable();
        updateStatus("Added item: " + id);
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
            lastDeletedItem = selected; // NEW - save before removing
            masterData.remove(selected);
            markDirty();
            refreshTable();
            updateStatus("Deleted item: " + selected.getId() + " (click Undo to restore)");
        }
    }

    @FXML
    private void handleUndoDelete() { // NEW METHOD
        if (lastDeletedItem == null) {
            showAlert("Nothing to Undo", "No recently deleted item to restore.");
            return;
        }
        String restoredId = lastDeletedItem.getId();
        masterData.add(lastDeletedItem);
        lastDeletedItem = null;
        markDirty();
        refreshTable();
        updateStatus("Restored item: " + restoredId);
    }

    @FXML
    private void handleSaveNow() {
        if (autoSaveService != null) {
            boolean success = autoSaveService.saveNow(new ArrayList<>(masterData));
            if (success) {
                dirty.set(false);
            }
        } else {
            boolean success = dao.saveAllItems(new ArrayList<>(masterData));
            if (success) {
                dirty.set(false);
                updateStatus("Saved manually at " + java.time.LocalTime.now().toString().substring(0, 8));
            } else {
                updateStatus("Save failed!");
                showAlert("Error", "Failed to save data");
            }
        }
    }

    private void markDirty() {
        dirty.set(true);
        updateStatus("✏️ Unsaved changes");
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
        if (autoSaveService != null) {
            autoSaveService.stop();
        }
        searchExecutor.shutdown();
        if (dirty.get()) {
            dao.saveAllItems(new ArrayList<>(masterData));
        }
    }
}