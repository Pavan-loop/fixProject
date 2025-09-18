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
    @FXML private TableColumn<TableReceivedData, String> reExecId;
    @FXML private TableColumn<TableReceivedData, String> reSymbol;
    @FXML private TableColumn<TableReceivedData, String> reSide;
    @FXML private TableColumn<TableReceivedData, String> reStatus;
    @FXML private TableColumn<TableReceivedData, Double> rePrice;
    @FXML private TableColumn<TableReceivedData, Integer> reFilledQuantity;
    @FXML private TableColumn<TableReceivedData, Integer> reRemainingQuantity;
    @FXML private TableColumn<TableReceivedData, Integer> reCanceledQuantity;
    @FXML private TableColumn<TableReceivedData, Integer> reOriginalQuantity;

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

        ReceiveDataUtils.addRow(reClient, reExecId, reSymbol, reSide, reStatus,
                rePrice, reFilledQuantity, reRemainingQuantity, reCanceledQuantity, reOriginalQuantity);

        reStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "A":
                            setText("Pending");
                            setStyle("-fx-background-color: burlywood; -fx-text-fill: black;"); break;
                        case "Fill": setStyle("-fx-background-color: lightgreen; -fx-text-fill: black;"); break;
                        case "Rejected": setStyle("-fx-background-color: lightcoral; -fx-text-fill: black;"); break;
                        case "Partial Fill": setStyle("-fx-background-color: khaki; -fx-text-fill: black;"); break;
                        case "Canceled": setStyle("-fx-background-color: gray; -fx-text-fill: black;"); break;
                        case "New": setStyle("-fx-background-color: lightblue; -fx-text-fill: black;"); break;
                        default: setStyle(""); break;
                    }
                }
            }
        });

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
                setGraphic(empty ? null : sendButton);
            }
        });
    }

    private void handleSendOrder(TableOrder order) throws SessionNotFound {
        if (order == null) return;

        boolean valid = true;
        if (order.getSymbol() == null || order.getSymbol().isBlank()) { TableUtils.highlightCell(order, symbol, tableOrder); valid = false; }
        if (order.getSide() == null || order.getSide().isBlank()) { TableUtils.highlightCell(order, side, tableOrder); valid = false; }
        if (order.getOrderType() == null || order.getOrderType().isBlank()) { TableUtils.highlightCell(order, orderType, tableOrder); valid = false; }
        if (order.getOrderPrice() <= 0) { TableUtils.highlightCell(order, orderPrice, tableOrder); valid = false; }
        if (order.getOrderQuantity() <= 0) { TableUtils.highlightCell(order, orderQuantity, tableOrder); valid = false; }

        if (!valid) { showAlert("Invalid Order", "Please fill all required fields."); return; }

        System.out.println("Sending order: " + order);

        ReceivedData data = SendFixMessage.send(initiator, order);
        uData.add(data);
        addValue(data);

        if (orderData.indexOf(order) == orderData.size() - 1) {
            orderData.add(new TableOrder("", "", "", 0.0, 0));
        }
    }

    /**
     * Add or update received execution report in the table
     */
    public void addValue(ReceivedData data) {
        Platform.runLater(() -> {
            tableReceivedData.setItems(receivedOrderData);

            boolean isCancel = "Canceled".equalsIgnoreCase(data.getExecType());

            if (isCancel) {

                Optional<TableReceivedData> existingRowOpt = receivedOrderData.stream()
                        .filter(r -> r.getClientOrdId().equals(data.getClOrdId()))
                        .reduce((first, second) -> second);

                int orig, filled, price;
                double px;

                if (existingRowOpt.isPresent()) {
                    TableReceivedData last = existingRowOpt.get();
                    orig = last.getOriginalQuantity();
                    filled = last.getFilledQuantity();
                    px = last.getPrice();
                } else {

                    orig = data.getOriginalQuantity() != null ? data.getOriginalQuantity() : 0;
                    filled = data.getFilledQuantity() != null ? data.getFilledQuantity() : 0;
                    px = data.getPrice() != null ? data.getPrice() : 0.0;
                }

                int remaining = 0;
                int canceled = Math.max(0, orig - filled);

                TableReceivedData row = new TableReceivedData(
                        data.getClOrdId(),
                        data.getExecId(),
                        data.getSymbol(),
                        data.getSide(),
                        "Canceled",
                        px,
                        orig,
                        filled,
                        remaining,
                        canceled,
                        data.getSessionID()
                );

                receivedOrderData.add(row);


                cancelOrder.getItems().remove(data.getClOrdId());
            }
            else {
                Optional<TableReceivedData> existingRowOpt = receivedOrderData.stream()
                        .filter(r -> r.getClientOrdId().equals(data.getClOrdId()))
                        .findFirst();

                if (existingRowOpt.isPresent()) {
                    TableReceivedData row = existingRowOpt.get();

                    if (data.getOriginalQuantity() != null && data.getOriginalQuantity() > 0 && row.getOriginalQuantity() == 0) {
                        row.setOriginalQuantity(data.getOriginalQuantity());
                    }

                    row.setExecType(data.getExecType());
                    if (data.getPrice() != null && data.getPrice() > 0.0) row.setPrice(data.getPrice());

                    if (data.getFilledQuantity() != null) {
                        if (data.getFilledQuantity() == 0 && "Canceled".equalsIgnoreCase(data.getExecType()) && row.getFilledQuantity() > 0) {
                        } else {
                            row.setFilledQuantity(data.getFilledQuantity());
                        }
                    }

                    if (data.getRemainingQuantity() != null) {
                        row.setRemainingQuantity(data.getRemainingQuantity());
                    }

                    int calcCanceled = Math.max(0, row.getOriginalQuantity() - row.getFilledQuantity() - row.getRemainingQuantity());
                    row.setCanceledQuantity(calcCanceled);

                    receivedOrderData.remove(row);
                    receivedOrderData.add(row);
                } else {
                    int orig = (data.getOriginalQuantity() != null) ? data.getOriginalQuantity() : 0;
                    int filled = (data.getFilledQuantity() != null) ? data.getFilledQuantity() : 0;
                    int remaining = (data.getRemainingQuantity() != null) ? data.getRemainingQuantity() : Math.max(0, orig - filled);
                    int canceled = Math.max(0, orig - filled - remaining);

                    TableReceivedData row = new TableReceivedData(
                            data.getClOrdId(),
                            data.getExecId(),
                            data.getSymbol(),
                            data.getSide(),
                            data.getExecType(),
                            data.getPrice() == null ? 0.0 : data.getPrice(),
                            orig,
                            filled,
                            remaining,
                            canceled,
                            data.getSessionID()
                    );
                    receivedOrderData.add(row);
                }

                if (!"Fill".equalsIgnoreCase(data.getExecType()) &&
                        !"Rejected".equalsIgnoreCase(data.getExecType())) {
                    if (!cancelOrder.getItems().contains(data.getClOrdId())) cancelOrder.getItems().add(data.getClOrdId());
                } else {
                    cancelOrder.getItems().remove(data.getClOrdId());
                }
            }
        });
    }



    public void onCancel() {
        String clOrdId = cancelOrder.getValue();
        if (clOrdId == null || clOrdId.isEmpty()) { showAlert("Cancel Error", "Please select a valid order to cancel."); return; }

        Optional<ReceivedData> find = uData.stream().filter(f -> f.getClOrdId().equals(clOrdId)).findFirst();
        if (find.isEmpty()) { showAlert("Cancel Error", "Order with ClOrdId not found."); return; }

        ReceivedData data = find.get();
        System.out.println("Cancelling Order => ClOrdId: " + data.getClOrdId() + ", Symbol: " + data.getSymbol());
        try { SendFixMessage.cancelOrder(initiator, data); }
        catch (SessionNotFound e) { showAlert("FIX Session Error", "Could not cancel order due to session error."); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
