package com.example.fixclient1;

import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.model.TableOrder;
import com.example.fixclient1.utils.TableUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import quickfix.ConfigError;

public class HelloController {

    @FXML
    private TableView<TableOrder> tableOrder;

    @FXML
    private TableColumn<TableOrder, String> symbol;
    @FXML
    private TableColumn<TableOrder, String> side;
    @FXML
    private TableColumn<TableOrder, String> orderType;
    @FXML
    private TableColumn<TableOrder, Double> orderPrice;
    @FXML
    private TableColumn<TableOrder, Integer> orderQuantity;

    private final ObservableList<TableOrder> orderData = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws ConfigError, InterruptedException {

        try {
            ClientApp clientApp = new ClientApp("/Users/pavanp/Desktop/fixProtocol/fixClient1/src/main/java/com/example/fixclient1/fix/initiator.cfg");
            clientApp.start();
        }catch (ConfigError e) {
            e.printStackTrace();
        }

        tableOrder.setItems(orderData);
        tableOrder.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableOrder.setEditable(true);

        TableUtils.addRow(symbol,side,orderType,orderPrice,orderQuantity);

        orderData.add(new TableOrder("","","", 0.0, 0));

        TableUtils.tableMovement(tableOrder);

    }

    @FXML
    public void onSave() {
        System.out.println(orderData);
    }
}