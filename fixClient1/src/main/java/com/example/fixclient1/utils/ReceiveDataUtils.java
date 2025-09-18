package com.example.fixclient1.utils;

import com.example.fixclient1.model.TableReceivedData;
import javafx.scene.control.TableColumn;

public class ReceiveDataUtils {

    public static void addRow(
            TableColumn<TableReceivedData, String> client,
            TableColumn<TableReceivedData, String> execId,
            TableColumn<TableReceivedData, String> symbol,
            TableColumn<TableReceivedData, String> side,
            TableColumn<TableReceivedData, String> execType,
            TableColumn<TableReceivedData, Double> price,
            TableColumn<TableReceivedData, Integer> filledQuantity,
            TableColumn<TableReceivedData, Integer> remainingQuantity,
            TableColumn<TableReceivedData, Integer> canceledQuantity,
            TableColumn<TableReceivedData, Integer> originalQuantity
    ) {
        client.setCellValueFactory(cellData -> cellData.getValue().clientOrdIdProperty());
        execId.setCellValueFactory(cellData -> cellData.getValue().execIdProperty());
        symbol.setCellValueFactory(cellData -> cellData.getValue().symbolProperty());
        side.setCellValueFactory(cellData -> cellData.getValue().sideProperty());
        execType.setCellValueFactory(cellData -> cellData.getValue().execTypeProperty());
        price.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
        filledQuantity.setCellValueFactory(cellData -> cellData.getValue().filledQuantityProperty().asObject());
        remainingQuantity.setCellValueFactory(cellData -> cellData.getValue().remainingQuantityProperty().asObject());
        canceledQuantity.setCellValueFactory(cellData -> cellData.getValue().canceledQuantityProperty().asObject());
        originalQuantity.setCellValueFactory(cellData -> cellData.getValue().originalQuantityProperty().asObject());
    }
}
