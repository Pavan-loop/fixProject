package com.example.fixclient1.utils;

import com.example.fixclient1.database.Curd;
import com.example.fixclient1.model.TableOrder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

public class TableUtils {

    public static void addRow(
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

        ObservableList<String> sides = FXCollections.observableArrayList("Buy", "Sell");
        side.setCellValueFactory(cellData -> cellData.getValue().sideProperty());
        side.setCellFactory(ComboBoxTableCell.forTableColumn(sides));

        ObservableList<String> order = FXCollections.observableArrayList("LIMIT", "MARKET");
        orderType.setCellValueFactory(cellData -> cellData.getValue().orderTypeProperty());
        orderType.setCellFactory(ComboBoxTableCell.forTableColumn(order));

        orderPrice.setCellValueFactory(cellData -> cellData.getValue().orderPriceProperty().asObject());
        orderPrice.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        orderPrice.setOnEditCommit(event -> event.getRowValue().setOrderPrice(event.getNewValue()));

        orderQuantity.setCellValueFactory(cellData -> cellData.getValue().orderQuantityProperty().asObject());
        orderQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        orderQuantity.setOnEditCommit(event -> event.getRowValue().setOrderQuantity(event.getNewValue()));
    }

    public static void tableMovement(TableView<TableOrder> tableOrder) {
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
}
