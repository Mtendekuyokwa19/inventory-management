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
import java.util.Optional;
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
    private AutoSaveService autoSaveService;

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
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Add Item");
        idDialog.setHeaderText("Enter Item ID (e.g., INV001)");
        idDialog.setContentText("ID:");

        Optional<String> idResult = idDialog.showAndWait();
        if (idResult.isPresent() && !idResult.get().trim().isEmpty()) {
            String id = idResult.get().trim();

            // Validate ID using Validator
            Optional<String> idError = Validator.validateId(id, masterData);
            if (idError.isPresent()) {
                showAlert("Validation Error", idError.get());
                return;
            }

            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Add Item");
            nameDialog.setHeaderText("Enter Item Name");
            nameDialog.setContentText("Name:");

            Optional<String> nameResult = nameDialog.showAndWait();
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String name = nameResult.get().trim();

                // Validate Name
                Optional<String> nameError = Validator.validateName(name);
                if (nameError.isPresent()) {
                    showAlert("Validation Error", nameError.get());
                    return;
                }

                TextInputDialog qtyDialog = new TextInputDialog();
                qtyDialog.setTitle("Add Item");
                qtyDialog.setHeaderText("Enter Quantity");
                qtyDialog.setContentText("Quantity:");

                Optional<String> qtyResult = qtyDialog.showAndWait();
                if (qtyResult.isPresent()) {
                    try {
                        int qty = Integer.parseInt(qtyResult.get().trim());

                        // Validate Quantity
                        Optional<String> qtyError = Validator.validateQuantity(qty);
                        if (qtyError.isPresent()) {
                            showAlert("Validation Error", qtyError.get());
                            return;
                        }

                        TextInputDialog priceDialog = new TextInputDialog();
                        priceDialog.setTitle("Add Item");
                        priceDialog.setHeaderText("Enter Price");
                        priceDialog.setContentText("Price:");

                        Optional<String> priceResult = priceDialog.showAndWait();
                        if (priceResult.isPresent()) {
                            try {
                                BigDecimal price = new BigDecimal(priceResult.get().trim());

                                // Validate Price
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

                            } catch (NumberFormatException e) {
                                showAlert("Error", "Invalid price format");
                            }
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Invalid quantity format");
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
        // Final save before closing
        if (dirty.get()) {
            dao.saveAllItems(new ArrayList<>(masterData));
        }
    }
}