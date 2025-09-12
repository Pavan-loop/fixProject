package com.example.fixclient1;

import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.fix.SendFixMessage;
import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.model.TableOrder;
import com.example.fixclient1.model.TableReceivedData;
import com.example.fixclient1.utils.ReceiveDataUtils;
import com.example.fixclient1.utils.TableUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import quickfix.ConfigError;
import quickfix.Initiator;
import quickfix.SessionNotFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HelloController {

    @FXML private TableView<TableOrder> tableOrder;
    @FXML private TableColumn<TableOrder, String> symbol;
    @FXML private TableColumn<TableOrder, String> side;
    @FXML private TableColumn<TableOrder, String> orderType;
    @FXML private TableColumn<TableOrder, Double> orderPrice;
    @FXML private TableColumn<TableOrder, Integer> orderQuantity;
    @FXML private TableColumn<TableOrder, Void> sendCol;

    @FXML private TableView<TableReceivedData> tableReceivedData;
    @FXML private TableColumn<TableReceivedData, String> reClient;
    @FXML private TableColumn<TableReceivedData, String> reSymbol;
    @FXML private TableColumn<TableReceivedData, String> reSide;
    @FXML private TableColumn<TableReceivedData, String> reStatus;
    @FXML private TableColumn<TableReceivedData, Double> rePrice;
    @FXML private TableColumn<TableReceivedData, Integer> reQuantity;

    @FXML private ComboBox<String> cancelOrder;

    private final List<ReceivedData> uData = new ArrayList<>();
    private final ObservableList<TableOrder> orderData = FXCollections.observableArrayList();
    private final ObservableList<TableReceivedData> receivedOrderData = FXCollections.observableArrayList();

    private Initiator initiator;

    @FXML
    public void initialize() {
        try {
            ClientApp clientApp = new ClientApp(
                    "C:\\Users\\nichiuser\\Downloads\\fixProject\\fixClient1\\src\\main\\java\\com\\example\\fixclient1\\fix\\initiator.cfg",
                    this
            );
            initiator = clientApp.start();
        } catch (ConfigError e) {
            e.printStackTrace();
        }

        tableOrder.setItems(orderData);
        tableOrder.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableReceivedData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableOrder.setEditable(true);


        TableUtils.addRow(symbol, side, orderType, orderPrice, orderQuantity);
        ReceiveDataUtils.addRow(reClient, reSymbol, reSide, reStatus, rePrice, reQuantity);


        orderData.add(new TableOrder("", "", "", 0.0, 0));

        addSendButtonToTable();

        TableUtils.tableMovement(tableOrder);
    }

    private void addSendButtonToTable() {
        sendCol.setCellFactory(col -> new TableCell<>() {
            private final Button sendButton = new Button("Send");

            {
                sendButton.setOnAction(e -> {
                    TableOrder order = getTableView().getItems().get(getIndex());
                    try {
                        handleSendOrder(order);
                    } catch (SessionNotFound ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(sendButton);
                }
            }
        });
    }

    private void handleSendOrder(TableOrder order) throws SessionNotFound {
        if (order == null) return;

        boolean valid = true;

        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            TableUtils.highlightCell(order, symbol, tableOrder);
            valid = false;
        }
        if (order.getSide() == null || order.getSide().isBlank()) {
            TableUtils.highlightCell(order, side, tableOrder);
            valid = false;
        }
        if (order.getOrderType() == null || order.getOrderType().isBlank()) {
            TableUtils.highlightCell(order, orderType, tableOrder);
            valid = false;
        }
        if (order.getOrderPrice() <= 0) {
            TableUtils.highlightCell(order, orderPrice, tableOrder);
            valid = false;
        }
        if (order.getOrderQuantity() <= 0) {
            TableUtils.highlightCell(order, orderQuantity, tableOrder);
            valid = false;
        }

        if (!valid) {
            showAlert("Invalid Order", "Please fill all required fields.");
            return;
        }

        System.out.println("Sending order: " + order);
        SendFixMessage.send(initiator, order);

        if (orderData.indexOf(order) == orderData.size() - 1) {
            orderData.add(new TableOrder("", "", "", 0.0, 0));
        }
    }

    public void addValue(ReceivedData data) {
        tableReceivedData.setItems(receivedOrderData);
        uData.add(data);
        receivedOrderData.add(new TableReceivedData(
                data.getClOrdId(),
                data.getSymbol(),
                data.getSide(),
                String.valueOf(data.getExecType()),
                data.getPrice(),
                data.getQuantity()
        ));

        if ("Partial Fill".equals(data.getExecType())) {
            cancelOrder.getItems().add(data.getClOrdId());
        }
    }

    public void onCancel() throws SessionNotFound {
        String clOrdId = cancelOrder.getValue();
        if (clOrdId == null || clOrdId.isEmpty()) return;

        Optional<ReceivedData> find = uData.stream()
                .filter(f -> f.getClOrdId().equals(clOrdId))
                .findFirst();

        find.ifPresent(data -> {
            System.out.println("Cancelling Order => ClOrdId: " + data.getClOrdId() + ", Symbol: " + data.getSymbol());
            try {
                SendFixMessage.cancelOrder(initiator, data);
            } catch (SessionNotFound e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
