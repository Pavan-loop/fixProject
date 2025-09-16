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
import javafx.scene.control.cell.PropertyValueFactory;
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
    @FXML private TableColumn<TableReceivedData, Integer> reRemainingQuantity;

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
        ReceiveDataUtils.addRow(reClient, reSymbol, reSide, reStatus, rePrice, reQuantity, reRemainingQuantity);

        // 🔹 Add background color logic for status column
        reStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle(""); // reset
                } else {
                    setText(status);

                    switch (status) {
                        case "Fill":
                            setStyle("-fx-background-color: lightgreen; -fx-text-fill: black;");
                            break;
                        case "Rejected":
                            setStyle("-fx-background-color: lightcoral; -fx-text-fill: white;");
                            break;
                        case "Partial Fill":
                            setStyle("-fx-background-color: khaki; -fx-text-fill: black;");
                            break;
                        case "New":
                            setStyle("-fx-background-color: lightblue; -fx-text-fill: black;");
                            break;
                        default:
                            setStyle(""); // default
                            break;
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

    /**
     * Add or update received execution report in the table
     */
    public void addValue(ReceivedData data) {
        Platform.runLater(() -> {
            tableReceivedData.setItems(receivedOrderData);
            uData.add(data);

            // Try to match existing row by ClOrdId
            Optional<TableReceivedData> existing = receivedOrderData.stream()
                    .filter(row -> row.getClientOrdId().equals(data.getClOrdId()))
                    .findFirst();

            // If status = Canceled, also try matching by original order ID from uData
            if (existing.isEmpty() && "Canceled".equals(data.getExecType())) {
                Optional<ReceivedData> orig = uData.stream()
                        .filter(o -> o.getClOrdId().equals(data.getClOrdId()))
                        .findFirst();
                if (orig.isPresent()) {
                    existing = receivedOrderData.stream()
                            .filter(row -> row.getClientOrdId().equals(orig.get().getClOrdId()))
                            .findFirst();
                }
            }

            if (existing.isPresent()) {
                TableReceivedData row = existing.get();
                row.setExecType(data.getExecType());
                row.setPrice(data.getPrice());
                row.setQuantity(data.getQuantity());
                row.setRemainingQuantity(data.getRemainingQuantity());
                receivedOrderData.remove(row);
                receivedOrderData.add(row);
            } else {
                receivedOrderData.add(new TableReceivedData(
                        data.getClOrdId(),
                        data.getSymbol(),
                        data.getSide(),
                        data.getExecType(),
                        data.getPrice(),
                        data.getQuantity(),
                        data.getRemainingQuantity()
                ));
            }

            System.out.println("Updated TableReceivedData: ClOrdId=" + data.getClOrdId()
                    + ", Status=" + data.getExecType()
                    + ", FilledQty=" + data.getQuantity()
                    + ", LeavesQty=" + data.getRemainingQuantity());

            if ("Partial Fill".equals(data.getExecType())) {
                if (!cancelOrder.getItems().contains(data.getClOrdId())) {
                    cancelOrder.getItems().add(data.getClOrdId());
                }
            }
        });
    }


    public void onCancel() {
        String clOrdId = cancelOrder.getValue();
        if (clOrdId == null || clOrdId.isEmpty()) {
            showAlert("Cancel Error", "Please select a valid order to cancel.");
            return;
        }

        Optional<ReceivedData> find = uData.stream()
                .filter(f -> f.getClOrdId().equals(clOrdId))
                .findFirst();

        if (find.isEmpty()) {
            showAlert("Cancel Error", "Order with ClOrdId not found.");
            return;
        }

        ReceivedData data = find.get();
        System.out.println("Cancelling Order => ClOrdId: " + data.getClOrdId() + ", Symbol: " + data.getSymbol());
        try {
            SendFixMessage.cancelOrder(initiator, data);
        } catch (SessionNotFound e) {
            showAlert("FIX Session Error", "Could not cancel order due to session error.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
