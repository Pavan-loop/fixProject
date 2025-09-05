package com.example.fixclient1;

import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.fix.SendFixMessage;
import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.model.TableOrder;
import com.example.fixclient1.model.TableReceivedData;
import com.example.fixclient1.utils.ReceiveDataUtils;
import com.example.fixclient1.utils.TableUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import quickfix.ConfigError;
import quickfix.Initiator;
import quickfix.SessionNotFound;


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

    @FXML
    private TableView<TableReceivedData> tableReceivedData;

    @FXML
    private TableColumn<TableReceivedData, String> reSymbol;
    @FXML
    private TableColumn<TableReceivedData, String> reStatus;
    @FXML
    private TableColumn<TableReceivedData, Double> rePrice;
    @FXML
    private TableColumn<TableReceivedData, Integer> reQuantity;


    private final ObservableList<TableOrder> orderData = FXCollections.observableArrayList();
    private final ObservableList<TableReceivedData> receivedOrderData = FXCollections.observableArrayList();

    Initiator initiator;
    @FXML
    public void initialize() throws ConfigError, InterruptedException {

        try {
            ClientApp clientApp = new ClientApp("C:\\Users\\nichiuser\\Desktop\\fixProject\\fixClient1\\src\\main\\java\\com\\example\\fixclient1\\fix\\initiator.cfg", this);
            initiator = clientApp.start();
        }catch (ConfigError e) {
            e.printStackTrace();
        }

        tableOrder.setItems(orderData);
        tableOrder.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableReceivedData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableOrder.setEditable(true);

        TableUtils.addRow(symbol,side,orderType,orderPrice,orderQuantity);
        ReceiveDataUtils.addRow(reSymbol, reStatus, rePrice, reQuantity);
        orderData.add(new TableOrder("","","", 0.0, 0));

        TableUtils.tableMovement(tableOrder);

    }

    @FXML
    public void onSave() throws SessionNotFound {
        System.out.println(orderData);
        TableOrder s = orderData.get(0);
        TableOrder order = new TableOrder(
                s.getSymbol(),
                s.getSide(),
                s.getOrderType(),
                s.getOrderPrice(),
                s.getOrderQuantity()
        );
        SendFixMessage.send(initiator, order);
    }

    public void addValue(ReceivedData data) {
        tableReceivedData.setItems(receivedOrderData);
        receivedOrderData.add(new TableReceivedData(data.getSymbol(), String.valueOf(data.getExecType()), data.getPrice(), data.getQuantity()));
        System.out.println(receivedOrderData);
    }
}