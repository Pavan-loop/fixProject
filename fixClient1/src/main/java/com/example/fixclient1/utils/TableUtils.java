package com.example.fixclient1.utils;

import com.example.fixclient1.database.Curd;
import com.example.fixclient1.model.TableOrder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import java.util.List;
/**
 * Utilities for configuring and validating a JavaFX TableView of TableOrder entries.
 * Sets up editable columns with ComboBox/TextField cells, loading symbols and prices via Curd,
 * and auto-filling price for MARKET orders. Adds TAB-based cell navigation, validates required
 * fields and positive values, and highlights invalid cells using the "error-cell" style.
 */
public class TableUtils {

    /**
 * Configures editing behavior for TableOrder columns in a JavaFX TableView.
 * Sets value factories, cell factories, and commit handlers:
 * - Symbol/Side/Order Type use ComboBox editors (symbols loaded via Curd).
 * - Order Price/Quantity use TextField editors with numeric converters.
 * On commit, updates the underlying TableOrder. For MARKET orders, auto-fills price from DB
 * via Curd.getPriceBySymbol(symbol); when changing from MARKET to LIMIT, resets price to 0.0.
 *
 * @param symbol        the symbol column; populated from Curd and made editable via ComboBox
 * @param side          the side column ("Buy" or "Sell") with ComboBox editing
 * @param orderType     the order type column ("LIMIT" or "MARKET") with auto-fill/reset logic
 * @param orderPrice    the price column; editable as Double with converter
 * @param orderQuantity the quantity column; editable as Integer with converter
 */public static void addRow(
            TableColumn<TableOrder, String> symbol,
            TableColumn<TableOrder, String> side,
            TableColumn<TableOrder, String> orderType,
            TableColumn<TableOrder, Double> orderPrice,
            TableColumn<TableOrder, Integer> orderQuantity
    ) {
        Curd data = new Curd();
        List<String> dSymbol = data.getSymbol();
        ObservableList<String> symbols = FXCollections.observableArrayList(dSymbol);


        symbol.setCellValueFactory(cellData -> cellData.getValue().symbolProperty());
        symbol.setCellFactory(ComboBoxTableCell.forTableColumn(symbols));
        symbol.setOnEditCommit(event -> {
            TableOrder order = event.getRowValue();
            String selectedSymbol = event.getNewValue();
            order.setSymbol(selectedSymbol);


            if ("MARKET".equalsIgnoreCase(order.getOrderType())) {
                Double dbPrice = data.getPriceBySymbol(selectedSymbol);
                if (dbPrice != null) {
                    order.setOrderPrice(dbPrice);
                }
            }
        });


        ObservableList<String> sides = FXCollections.observableArrayList("Buy", "Sell");
        side.setCellValueFactory(cellData -> cellData.getValue().sideProperty());
        side.setCellFactory(ComboBoxTableCell.forTableColumn(sides));
        side.setOnEditCommit(event -> event.getRowValue().setSide(event.getNewValue()));


        ObservableList<String> orderTypes = FXCollections.observableArrayList("LIMIT", "MARKET");
        orderType.setCellValueFactory(cellData -> cellData.getValue().orderTypeProperty());
        orderType.setCellFactory(ComboBoxTableCell.forTableColumn(orderTypes));
        orderType.setOnEditCommit(event -> {
            TableOrder order = event.getRowValue();
            String oldType = order.getOrderType();
            String newType = event.getNewValue();
            order.setOrderType(newType);

            if ("MARKET".equalsIgnoreCase(newType)) {

                Double dbPrice = data.getPriceBySymbol(order.getSymbol());
                if (dbPrice != null) {
                    order.setOrderPrice(dbPrice);
                }
            } else if ("LIMIT".equalsIgnoreCase(newType) && "MARKET".equalsIgnoreCase(oldType)) {
                order.setOrderPrice(0.0);
            }
        });


        orderPrice.setCellValueFactory(cellData -> cellData.getValue().orderPriceProperty().asObject());
        orderPrice.setCellFactory(col -> new TextFieldTableCell<>(new DoubleStringConverter()));
        orderPrice.setOnEditCommit(event -> {
            TableOrder order = event.getRowValue();
            order.setOrderPrice(event.getNewValue());
        });


        orderQuantity.setCellValueFactory(cellData -> cellData.getValue().orderQuantityProperty().asObject());
        orderQuantity.setCellFactory(col -> new TextFieldTableCell<>(new IntegerStringConverter()));
        orderQuantity.setOnEditCommit(event -> event.getRowValue().setOrderQuantity(event.getNewValue()));
    }


    /**
 * Enables TAB-based horizontal navigation and editing within a TableView row.
 * When TAB is pressed, moves focus and selection to the next column in the same row,
 * wrapping to the first column after the last, and initiates edit on the target cell.
 * Selection and focus updates are deferred to the JavaFX application thread via
 * Platform.runLater, and the event is consumed to prevent default handling.
 *
 * @param tableOrder the TableView of TableOrder entries to enable TAB navigation on
 */public static void tableMovement(TableView<TableOrder> tableOrder) {
        tableOrder.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                TablePosition<?, ?> pos = tableOrder.getFocusModel().getFocusedCell();
                if (pos != null) {
                    int colIndex = pos.getColumn();
                    int rowIndex = pos.getRow();
                    int totalCols = tableOrder.getColumns().size();
                    int nextColIndex = (colIndex + 1) % totalCols;

                    tableOrder.edit(rowIndex, tableOrder.getColumns().get(nextColIndex));
                    Platform.runLater(() -> {
                        tableOrder.getSelectionModel().select(rowIndex);
                        tableOrder.getFocusModel().focus(rowIndex, tableOrder.getColumns().get(nextColIndex));
                        tableOrder.edit(rowIndex, tableOrder.getColumns().get(nextColIndex));
                    });
                    event.consume();
                }
            }
        });
    }

    /**
 * Validates a TableOrder row and highlights any invalid cells.
 *
 * Checks that symbol, side, and order type are non-blank and that price and quantity are positive.
 * For each failed check, applies the "error-cell" style to the corresponding cell in the provided TableView.
 *
 * @param order          the row data to validate
 * @param symbol         the symbol column to highlight when invalid
 * @param side           the side column to highlight when invalid
 * @param orderType      the order type column to highlight when invalid
 * @param orderPrice     the price column to highlight when invalid
 * @param orderQuantity  the quantity column to highlight when invalid
 * @param table          the TableView containing the row
 * @return true if all fields are valid; false otherwise
 */
    public static boolean validateRow(TableOrder order,
                                      TableColumn<TableOrder, String> symbol,
                                      TableColumn<TableOrder, String> side,
                                      TableColumn<TableOrder, String> orderType,
                                      TableColumn<TableOrder, Double> orderPrice,
                                      TableColumn<TableOrder, Integer> orderQuantity,
                                      TableView<TableOrder> table) {
        boolean valid = true;

        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            highlightCell(order, symbol, table);
            valid = false;
        }
        if (order.getSide() == null || order.getSide().isBlank()) {
            highlightCell(order, side, table);
            valid = false;
        }
        if (order.getOrderType() == null || order.getOrderType().isBlank()) {
            highlightCell(order, orderType, table);
            valid = false;
        }
        if (order.getOrderPrice() <= 0) {
            highlightCell(order, orderPrice, table);
            valid = false;
        }
        if (order.getOrderQuantity() <= 0) {
            highlightCell(order, orderQuantity, table);
            valid = false;
        }

        return valid;
    }

    /**
 * Highlights a specific cell in the given TableView by adding the "error-cell" CSS style.
 * Resolves the row for the provided TableOrder and, on the FX thread, refreshes the table
 * and locates the matching TableCell for the given column. If found, the style is added
 * only once to avoid duplicates. If the item/column is not present, no action is taken.
 *
 * @param order  the TableOrder whose row should be highlighted
 * @param column the TableColumn identifying the target cell in the row
 * @param table  the TableView containing the row and column
 */
    public static void highlightCell(TableOrder order, TableColumn<?, ?> column, TableView<TableOrder> table) {
        int rowIndex = table.getItems().indexOf(order);
        Platform.runLater(() -> {
            table.refresh();
            table.lookupAll(".table-row-cell").forEach(node -> {
                if (node instanceof TableRow<?> row && row.getIndex() == rowIndex) {
                    row.lookupAll(".table-cell").forEach(cellNode -> {
                        if (cellNode instanceof TableCell<?, ?> cell && cell.getTableColumn() == column) {
                            if (!cell.getStyleClass().contains("error-cell")) {
                                cell.getStyleClass().add("error-cell");
                            }
                        }
                    });
                }
            });
        });
    }
}
