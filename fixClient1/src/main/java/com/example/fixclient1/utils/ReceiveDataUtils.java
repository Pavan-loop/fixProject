package com.example.fixclient1.utils;

import com.example.fixclient1.model.TableReceivedData;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

public class ReceiveDataUtils {
    public static void addRow(
            TableColumn<TableReceivedData, String> client,
            TableColumn<TableReceivedData, String> symbol,
            TableColumn<TableReceivedData, String> side,
            TableColumn<TableReceivedData, String> orderType,
            TableColumn<TableReceivedData, Double> price,
            TableColumn<TableReceivedData, Integer> quantity,
            TableColumn<TableReceivedData, Integer> remainingQuantity
    ) {
        client.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClientOrdId()));
        symbol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSymbol()));
        side.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSide()));
        orderType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExecType()));
        price.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        quantity.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
        remainingQuantity.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRemainingQuantity()).asObject());
    }
}
