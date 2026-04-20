# Inventory Management System

A lightweight desktop application for managing product inventory with persistent text-based storage. Built with JavaFX featuring concurrent background operations, modern lambda-based programming patterns, and comprehensive error resilience.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Folder Structure](#folder-structure)
- [Features](#features)
    - [Core Inventory Operations](#core-inventory-operations)
    - [Persistent Text Storage](#persistent-text-storage)
    - [Real-Time Search & Filtering](#real-time-search--filtering)
    - [Background Auto-Save](#background-auto-save)
    - [Data Validation & Error Recovery](#data-validation--error-recovery)
- [Technical Implementation](#technical-implementation)
    - [Threading Architecture](#threading-architecture)
    - [Lambda Expression Usage](#lambda-expression-usage)
    - [Error Handling Strategy](#error-handling-strategy)
- [File Format Specification](#file-format-specification)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)

---

## Project Overview

This application provides a single-window interface for CRUD operations on inventory items. All data persists to a local pipe-delimited text file (`inventory.txt`), making it portable, human-readable, and editable with any text editor. The system handles concurrent file access gracefully and recovers from corrupted data without crashing.

**Key Design Philosophy:** The UI never freezes. All disk I/O happens on background threads. All user interactions respond instantly.

---

## Folder Structure
inventory-manager/
├── src/
│   └── com/
│       └── inventory/
│           ├── Main.java                 # Application entry point
│           ├── controller/
│           │   └── InventoryController.java   # UI logic & event handling
│           ├── model/
│           │   └── Item.java             # Data entity (ID, Name, Qty, Price)
│           ├── service/
│           │   ├── FileService.java      # Text file read/write operations
│           │   └── InventoryService.java # Business logic & data operations
│           ├── task/
│           │   └── AutoSaveTask.java     # Background save runnable
│           ├── util/
│           │   └── Validator.java        # Input validation utilities
│           └── view/
│               └── inventory.fxml        # JavaFX scene layout
├── data/
│   └── inventory.txt                     # Persistent storage (auto-created)
├── css/
│   └── style.css                         # Optional JavaFX styling
├── lib/                                  # External dependencies (if any)
├── README.md
└── build.gradle / pom.xml                # Build configuration
plain
Copy


---

## Features

### Core Inventory Operations

| Operation | Description |
|-----------|-------------|
| **Add Item** | Insert new product with auto-generated or manual ID. Validates uniqueness before commit. |
| **Edit Inline** | Double-click any cell in the table to edit directly. Changes trigger dirty-state flagging. |
| **Delete Item** | Remove selected row with confirmation dialog. Supports single or multi-select removal. |
| **View All** | Master list displayed in sortable `TableView` with column reordering support. |

**Item Fields:**
- `ID` — Unique alphanumeric identifier (e.g., `INV001`)
- `Name` — Product description
- `Quantity` — Integer stock count
- `Price` — Decimal unit price in local currency

---

### Persistent Text Storage

Data survives application restarts via pipe-delimited flat file storage.

**Characteristics:**
- **Human-readable:** Open `inventory.txt` in Notepad, Excel, or any editor
- **Zero dependencies:** No database server or external libraries required
- **Portable:** Copy the `data/` folder to move inventory between machines
- **Atomic writes:** Save operations write to temporary file first, then rename to prevent corruption on crash

**File Location:** `data/inventory.txt` (created automatically on first run if missing)

---

### Real-Time Search & Filtering

A live search field filters the inventory table instantly as you type.

**Behavior:**
- Case-insensitive matching on product name and ID
- Filters applied via `FilteredList` predicate updated on every keystroke
- No lag: filtering runs on JavaFX Application Thread with lightweight lambda predicates
- Clear search field to restore full inventory view

---

### Background Auto-Save

Inventory changes persist automatically without manual intervention or UI interruption.

**Mechanism:**
- `ScheduledExecutorService` triggers save task every 30 seconds
- Save only executes if data is "dirty" (modified since last save)
- Status bar displays last successful save timestamp
- Manual "Save Now" button available for immediate persistence

**Thread Safety:**
- Background thread handles all file I/O
- `Platform.runLater()` pushes UI updates (status messages) back to JavaFX thread
- Concurrent modification prevented via synchronized inventory list access

---

### Data Validation & Error Recovery

| Scenario | System Response |
|----------|-----------------|
| **Empty inventory file** | Initialize with blank state, create file on first save |
| **Corrupt line in file** | Skip malformed entry, log to status bar, load all valid lines |
| **Duplicate ID on add/edit** | Reject operation, highlight offending field, display alert |
| **Non-numeric quantity/price** | `TextFormatter` prevents invalid input at keystroke level |
| **Negative numbers** | Validation fails, show inline error, block commit |
| **File locked by external process** | Retry once after 500ms; if still locked, alert user and retain in-memory data |
| **Disk full / permission denied** | Catch `IOException`, display error dialog, preserve unsaved changes in memory |
| **Manual file edit detected** | File watcher thread detects external modification; prompt user to reload |

---

## Technical Implementation

### Threading Architecture

| Component | Threading Model | Purpose |
|-----------|-----------------|---------|
| **JavaFX Application Thread** | Primary UI thread | Table rendering, event handling, scene updates |
| **Auto-Save Thread** | `ScheduledExecutorService` single thread | Periodic background serialization to disk |
| **File Watcher Thread** | Dedicated daemon thread | Monitors `inventory.txt` for external modifications using polling or `WatchService` |
| **Import/Export Tasks** | `Task` / `Service` instances | Optional bulk operations without freezing table interaction |

**Concurrency Controls:**
- Inventory list wrapped in `FXCollections.observableArrayList()` for thread-safe UI binding
- File write operations synchronized to prevent interleaving during auto-save and manual save
- Background threads marked `setDaemon(true)` to prevent JVM hanging on close

---

### Lambda Expression Usage

Lambdas replace anonymous inner classes throughout the codebase for conciseness and functional clarity:

| Context | Lambda Application |
|---------|-------------------|
| **Event Handlers** | Button `setOnAction`, menu item handlers |
| **Table Cell Factories** | Custom rendering and inline editing commit handlers |
| **Property Listeners** | `ChangeListener` for search field text, dirty-state flags |
| **Stream Operations** | Calculate inventory totals, filter expired items, map to display strings |
| **Task Callbacks** | `setOnSucceeded`, `setOnFailed`, `setOnCancelled` for background operations |
| **Runnable Submission** | `Platform.runLater(() -> updateStatus(message))` |
| **Predicate Definitions** | `FilteredList` search criteria updated dynamically |

---

### Error Handling Strategy

**Layered Defense:**

1. **Prevention Layer**
    - `TextFormatter` on numeric fields restricts input to valid characters
    - Input length limits prevent buffer issues
    - Uniqueness check before commit prevents duplicate IDs

2. **Validation Layer**
    - `Validator` utility class centralizes business rules
    - Returns `Optional<String>` error message; empty means valid
    - Controller consumes validation results to highlight fields

3. **Execution Layer**
    - Try-catch blocks around every file operation
    - Specific handlers for `IOException`, `NumberFormatException`, `SecurityException`
    - Graceful degradation: if file load fails, start with empty inventory rather than crash

4. **Recovery Layer**
    - In-memory inventory remains authoritative during file errors
    - Failed auto-save retries on next cycle
    - User can manually export data if persistent storage becomes unavailable

---

## File Format Specification

**File:** `data/inventory.csv`

**Encoding:** UTF-8

**Delimiter:** Pipe character `|`

**Line Format:**

ID|NAME|QUANTITY|PRICE
plain
Copy


**Example:**

INV001|Wireless Optical Mouse|15|29.99
INV002|USB-C to USB-A Cable|42|8.50
INV003|Mechanical Keyboard Brown Switch|7|89.00
INV004|27-inch IPS Monitor|3|249.99
plain
Copy


**Constraints:**
- ID: Non-empty, no pipe characters, unique across file
- Name: Non-empty, no pipe characters
- Quantity: Integer, zero or positive
- Price: Decimal, zero or positive

**Blank lines** are ignored during load.

---

## Getting Started

### Prerequisites

- JDK 11 or higher (JavaFX included or configured separately)
- Maven or Gradle (optional, for dependency management)

### Build & Run

```bash
# Compile
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -d out src/com/inventory/**/*.java

# Run
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp out com.inventory.Main

Or use your IDE's JavaFX project template.
Usage Guide
First Launch

    Run application. Empty table appears.
    data/inventory.txt created automatically after first modification.

Adding Items

    Click Add Item button.
    Fill fields in dialog. ID uniqueness verified automatically.
    Click Save. Item appears in table immediately.

Editing Items

    Double-click any cell in table row.
    Enter new value. Press Enter to commit, Escape to cancel.
    Invalid values trigger inline error and revert.

Searching

    Type in Search field at top.
    Table filters in real-time as you type.
    Clear field to show all items.

Manual Save

    Click Save Now button (floppy disk icon).
    Status bar confirms: "Saved at HH:MM:SS".

External Editing

    Open data/inventory.txt in any editor while app runs.
    Save file. App detects change and prompts: "Inventory file modified externally. Reload?"
    Choose Yes to refresh, No to keep current state (may overwrite external changes on next save).