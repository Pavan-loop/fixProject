package com.example.fixclient1.utils;

import com.example.fixclient1.model.TableOrder;
import com.example.fixclient1.model.TableReceivedData;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

public class ReceiveDataUtils {
    public static void addRow(
            TableColumn<TableReceivedData, String> symbol,
            TableColumn<TableReceivedData, String> orderType,
            TableColumn<TableReceivedData, Double> price,
            TableColumn<TableReceivedData, Integer> quantity
    ) {
        symbol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSymbol()));
        orderType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExecType()));
        price.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        quantity.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
    }
}
