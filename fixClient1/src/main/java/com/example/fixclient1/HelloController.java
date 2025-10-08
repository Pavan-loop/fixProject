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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import quickfix.Initiator;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX controller for order entry, submission, and execution tracking.
 * - Initializes a FIX 4.4 ClientApp, starts the Initiator, and wires MarketEnquiryController.
 * - Configures an editable order table (symbol/side/type/price/qty) with a per-row Send button.
 * - Configures a received-executions table, styles status cells, and updates rows via addValue().
 * - Sends NewOrderSingle, and supports cancel/replace via SendFixMessage; maintains a cancelable list.
 * - Validates inputs and highlights invalid cells using TableUtils; appends new blank rows on demand.
 * - Ensures UI updates occur on the FX thread using Platform.runLater.
 * Dependencies: ClientApp, SendFixMessage, TableUtils, ReceiveDataUtils, MarketEnquiryController.
 */
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

    private ClientApp clientApp;

    public void setClientApp(ClientApp clientApp) {
        this.clientApp = clientApp;
    }

    public void setInitiator(Initiator initiator) {
        this.initiator = initiator;
    }

    /**
 * Initializes the controller after FXML loading.
 * - Creates a FIX 4.4 ClientApp from initiator.cfg, links it to MarketEnquiryController on the FX thread,
 *   and starts the Initiator.
 * - Binds order/received tables to their data models, sets resize policies, enables editing, and configures
 *   column editors with TableUtils and value factories with ReceiveDataUtils.
 * - Applies status-based styling to the executions status column.
 * - Adds an initial blank order row, attaches a per-row Send button, and enables TAB navigation via TableUtils.tableMovement.
 * Any ConfigError during client initialization is caught and logged.
 */
    @FXML
    public void initialize() {
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
                        case "Pending" -> setStyle("-fx-background-color: burlywood; -fx-text-fill: black;");
                        case "Filled" -> setStyle("-fx-background-color: #6ef16e; -fx-text-fill: black;");
                        case "Rejected" -> setStyle("-fx-background-color: #fb0000; -fx-text-fill: black;");
                        case "Partial Fill" -> setStyle("-fx-background-color: #ffe800; -fx-text-fill: black;");
                        case "Canceled" -> setStyle("-fx-background-color: gray; -fx-text-fill: black;");
                        case "New" -> setStyle("-fx-background-color: lightblue; -fx-text-fill: black;");
                        default -> setStyle("");
                    }
                }
            }
        });

        orderData.add(new TableOrder("", "", "", "", 0.0, 0));
        addSendButtonToTable();
        TableUtils.tableMovement(tableOrder);
    }

    /**
 * Configures the send column to render a per-row "Send" button in the orders table.
 * Clicking the button submits the row's TableOrder by invoking handleSendOrder(...).
 * The button is displayed only for non-empty cells; any SessionNotFound thrown during
 * submission is caught and logged via stack trace.
 * Call once during initialization after table/columns are set up.
 *
 * @see #handleSendOrder(TableOrder)
 */
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

    /**
 * Validates and submits the provided order.
 * - Highlights missing/invalid fields in the table using TableUtils and shows an alert if validation fails.
 * - On success, sends a FIX NewOrderSingle via SendFixMessage, stores the returned ReceivedData,
 *   updates the executions table through addValue(...), and appends a new blank row if the submitted row was last.
 *
 * @param order the TableOrder to validate and send; ignored if null
 * @throws SessionNotFound if no valid FIX session is available for sending
 * @see TableUtils#highlightCell(TableOrder, javafx.scene.control.TableColumn, javafx.scene.control.TableView)
 * @see SendFixMessage#send(quickfix.Initiator, TableOrder)
 */
    private void handleSendOrder(TableOrder order) throws SessionNotFound {
        if (order == null) return;

        boolean valid = true;
        if (order.getSymbol() == null || order.getSymbol().isBlank()) { TableUtils.highlightCell(order, symbol, tableOrder); valid = false; }
        if (order.getSide() == null || order.getSide().isBlank()) { TableUtils.highlightCell(order, side, tableOrder); valid = false; }
        if (order.getOrderType() == null || order.getOrderType().isBlank()) { TableUtils.highlightCell(order, orderType, tableOrder); valid = false; }
        if (order.getOrderPrice() <= 0) { TableUtils.highlightCell(order, orderPrice, tableOrder); valid = false; }
        if (order.getOrderQuantity() <= 0) { TableUtils.highlightCell(order, orderQuantity, tableOrder); valid = false; }

        if (!valid) {
            showAlert("Invalid Order", "Please fill all required fields.");
            return;
        }

        if (initiator == null || initiator.getSessions().isEmpty() || !initiator.isLoggedOn()) {
            showAlert("Disconnected", "You are disconnected. Please start the client first.");
            return;
        }

        try {
            System.out.println("Sending order: " + order);
            ReceivedData data = SendFixMessage.send(initiator, order);
            uData.add(data);
            addValue(data);

            if (orderData.indexOf(order) == orderData.size() - 1) {
                orderData.add(new TableOrder("", "", "", "", 0.0, 0));
            }
        } catch (Exception e) {
            showAlert("Send Error", "Failed to send order: " + e.getMessage());
        }
    }

    /**
 * Updates the executions view from a ReceivedData event on the FX thread.
 * - Binds the executions table to receivedOrderData, then appends or updates a row.
 * - For execType "Canceled": resolves the target order by OrigClOrdID or ClOrdID, derives quantities,
 *   inserts a "Canceled" row, and removes the order from the cancel list.
 * - Otherwise: updates or creates the row for ClOrdID, recalculates canceled quantity, refreshes the row,
 *   and maintains the cancelable list (adds unless execType is "Fill" or "Rejected").
 * Threading: All UI mutations are scheduled via Platform.runLater.
 * Side effects: Mutates receivedOrderData, tableReceivedData, and cancelOrder items.
 *
 * @param data the execution update to apply; must not be null
 */
    public void addValue(ReceivedData data) {
        Platform.runLater(() -> {
            tableReceivedData.setItems(receivedOrderData);

            boolean isCancel = "Canceled".equalsIgnoreCase(data.getExecType());

            if (isCancel) {
                String matchId = (data.getOrigClOrdId() != null && !data.getOrigClOrdId().isEmpty())
                        ? data.getOrigClOrdId()
                        : data.getClOrdId();

                Optional<TableReceivedData> origRowOpt = receivedOrderData.stream()
                        .filter(r -> r.getClientOrdId().equals(matchId))
                        .findFirst();

                int origQty, filled;
                double px;
                String symbol;

                if (origRowOpt.isPresent()) {
                    TableReceivedData orig = origRowOpt.get();
                    origQty = orig.getOriginalQuantity();
                    filled = orig.getFilledQuantity();
                    px = orig.getPrice();
                    symbol = orig.getSymbol();
                } else {
                    origQty = (data.getOriginalQuantity() != null) ? data.getOriginalQuantity() : 0;
                    filled = (data.getFilledQuantity() != null) ? data.getFilledQuantity() : 0;
                    px = (data.getPrice() != null) ? data.getPrice() : 0.0;
                    symbol = (data.getSymbol() != null) ? data.getSymbol() : "";
                }

                int remaining = 0;
                int canceled = Math.max(0, origQty - filled);

                TableReceivedData row = new TableReceivedData(
                        matchId,
                        data.getExecId(),
                        symbol,
                        data.getSide(),
                        "Canceled",
                        px,
                        origQty,
                        filled,
                        remaining,
                        canceled,
                        data.getSessionID()
                );

                receivedOrderData.add(row);
                cancelOrder.getItems().remove(matchId);

            } else {
                Optional<TableReceivedData> existingRowOpt = receivedOrderData.stream()
                        .filter(r -> r.getClientOrdId().equals(data.getClOrdId()))
                        .findFirst();

                if (existingRowOpt.isPresent()) {
                    TableReceivedData row = existingRowOpt.get();

                    if (data.getExecId() != null && !data.getExecId().isBlank()) {
                        row.setExecId(data.getExecId());
                    }

                    if (data.getOriginalQuantity() != null && data.getOriginalQuantity() > 0 && row.getOriginalQuantity() == 0) {
                        row.setOriginalQuantity(data.getOriginalQuantity());
                    }

                    row.setExecType(data.getExecType());
                    if (data.getPrice() != null && data.getPrice() > 0.0) row.setPrice(data.getPrice());

                    if (data.getFilledQuantity() != null) {
                        row.setFilledQuantity(data.getFilledQuantity());
                    }

                    if (data.getRemainingQuantity() != null) {
                        row.setRemainingQuantity(data.getRemainingQuantity());
                    }

                    int calcCanceled = Math.max(0,
                            row.getOriginalQuantity() - row.getFilledQuantity() - row.getRemainingQuantity());
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

    /**
 * Cancels the order selected in the cancelOrder ComboBox.
 * - Validates the chosen ClOrdId and alerts if none is selected or not found in uData.
 * - Retrieves the matching ReceivedData, logs the action, and submits a FIX OrderCancelRequest via SendFixMessage.
 * - Any session errors are caught and shown as an alert.
 * Side effects: Displays alerts and writes to stdout.
 *
 * @see SendFixMessage#cancelOrder(Initiator, ReceivedData, SessionID)
 */
    public void onCancel() {
        String clOrdId = cancelOrder.getValue();
        if (clOrdId == null || clOrdId.isEmpty()) {
            showAlert("Cancel Error", "Please select a valid order to cancel.");
            return;
        }

        Optional<ReceivedData> find = uData.stream().filter(f -> f.getClOrdId().equals(clOrdId)).findFirst();
        if (find.isEmpty()) {
            showAlert("Cancel Error", "Order with ClOrdId not found.");
            return;
        }

        ReceivedData data = find.get();
        System.out.println("Cancelling Order => ClOrdId: " + data.getClOrdId() + ", Symbol: " + data.getSymbol());
        try {
            SendFixMessage.cancelOrder(initiator, data, data.getSessionID());
        } catch (SessionNotFound e) {
            showAlert("FIX Session Error", "Could not cancel order due to session error.");
        }
    }

    /**
 * FXML event handler that amends the selected order.
 * - Validates the selected ClOrdId from cancelOrder; alerts if none selected or not found in uData.
 * - Opens a dialog prefilled with current price and original quantity; on confirmation, parses inputs,
 *   creates a new ClOrdID (original + "_mod" + timestamp), and sends a FIX OrderCancelReplaceRequest
 *   via SendFixMessage.replaceOrder with the order's symbol and side ('1' for BUY, '2' for SELL).
 * - Parsing or session-related errors are caught and shown as a warning alert.
 * Side effects: Displays dialogs/alerts and may transmit a FIX replace request.
 *
 * @see SendFixMessage#replaceOrder(quickfix.Initiator, String, String, String, double, int, char)
 */
    @FXML
    private void onModify() {
        String clOrdId = cancelOrder.getValue();
        if (clOrdId == null || clOrdId.isEmpty()) {
            showAlert("Modify Error", "Please select a valid order to modify.");
            return;
        }

        Optional<ReceivedData> find = uData.stream().filter(f -> f.getClOrdId().equals(clOrdId)).findFirst();
        if (find.isEmpty()) {
            showAlert("Modify Error", "Order with ClOrdId not found.");
            return;
        }

        ReceivedData data = find.get();

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Modify Order");
        dialog.setHeaderText("Modify Order: " + data.getClOrdId());

        ButtonType modifyButtonType = new ButtonType("Modify", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(modifyButtonType, ButtonType.CANCEL);

        TextField priceField = new TextField(String.valueOf(data.getPrice()));
        TextField qtyField = new TextField(String.valueOf(data.getOriginalQuantity()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Amend Price:"), 0, 0);
        grid.add(priceField, 1, 0);
        grid.add(new Label("Amend Quantity:"), 0, 1);
        grid.add(qtyField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == modifyButtonType) {
                return new Pair<>(priceField.getText(), qtyField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double newPrice = Double.parseDouble(result.getKey());
                int newQty = Integer.parseInt(result.getValue());
                String newClOrdId = data.getClOrdId() + "_mod" + System.currentTimeMillis();

                SendFixMessage.replaceOrder(
                        initiator,
                        data.getClOrdId(),
                        newClOrdId,
                        data.getSymbol(),
                        newPrice,
                        newQty,
                        data.getSide().equalsIgnoreCase("BUY") ? '1' : '2'
                );
            } catch (Exception e) {
                showAlert("Modify Error", "Invalid input: " + e.getMessage());
            }
        });
    }

    /**
     * Cancels all partially filled, new, or pending orders.
     * Call this method when the client logs out.
     */
    public void cancelPartiallyFilledOrdersOnLogout(SessionID sessionID) {
        if (uData == null || uData.isEmpty()) return;

        List<ReceivedData> ordersToCancel = uData.stream()
                .filter(order -> {
                    String type = order.getExecType();
                    return "Partial Fill".equalsIgnoreCase(type)
                            || "Pending".equalsIgnoreCase(type)
                            || "New".equalsIgnoreCase(type);
                })
                .toList();

        for (ReceivedData order : ordersToCancel) {
            try {
                SendFixMessage.cancelOrder(initiator, order, sessionID);
                System.out.println("Auto-canceling order on logout: " + order.getClOrdId());
            } catch (Exception e) {
                showAlert("Logout Cancel Error", "Failed to cancel order " + order.getClOrdId() + ": " + e.getMessage());
            }
        }
    }



    /**
 * Displays a modal warning alert with the provided title and message.
 * The header is omitted and the dialog blocks via showAndWait() until dismissed.
 * Must be invoked on the FX thread.
 *
 * @param title   the window title for the alert
 * @param content the message to display in the alert body
 */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
