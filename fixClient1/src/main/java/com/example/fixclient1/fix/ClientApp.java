package com.example.fixclient1.fix;

import com.example.fixclient1.HelloController;
import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.MarketEnquiryController; // adjust package if different
import javafx.application.Platform;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.fix44.MarketDataIncrementalRefresh;

import java.util.HashMap;
import java.util.Map;

public class ClientApp extends MessageCracker implements Application {

    private final HelloController controller;
    private final Initiator initiator;
    private SessionID activeSessionID;

    // NEW: reference to the market enquiry controller
    private MarketEnquiryController marketEnquiryController;

    public ClientApp(String configFile, HelloController controller) throws ConfigError {
        this.controller = controller;
        SessionSettings settings = new SessionSettings(configFile);

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory();

        initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    // setter for MarketEnquiryController so HelloController or app wiring can register it
    public void setMarketEnquiryController(MarketEnquiryController controller) {
        this.marketEnquiryController = controller;
    }

    @Override public void onCreate(SessionID sessionID) { System.out.println("Client Created: " + sessionID); }
    @Override public void onLogon(SessionID sessionID) { System.out.println("Client logon to broker: " + sessionID); this.activeSessionID = sessionID; }
    @Override public void onLogout(SessionID sessionID) { System.out.println("Client logout from the broker: " + sessionID); if (activeSessionID != null && activeSessionID.equals(sessionID)) { activeSessionID = null; } }
    @Override public void toAdmin(Message message, SessionID sessionID) { System.out.println("Client sending admin message: " + message); }
    @Override public void fromAdmin(Message message, SessionID sessionID) { System.out.println("Client receiving admin message: " + message); }
    @Override public void toApp(Message message, SessionID sessionID) { System.out.println("Message of client to broker: " + message); }
    @Override public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            System.out.println("Received from broker: " + message);
            crack(message, sessionID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Handler
    public void onMessage(ExecutionReport report, SessionID sessionID) throws FieldNotFound {
        // --- Existing ExecutionReport handling (unchanged) ---
        char sideChar = report.getSide().getValue();
        String side = switch (sideChar) {
            case Side.BUY -> "BUY";
            case Side.SELL -> "SELL";
            case Side.SELL_SHORT -> "SELL_SHORT";
            default -> "UNKNOWN";
        };

        String clOrdId = report.isSetField(ClOrdID.FIELD) ? report.getClOrdID().getValue() : null;
        String origClOrdId = report.isSetField(OrigClOrdID.FIELD) ? report.getString(OrigClOrdID.FIELD) : null;
        String execId = report.isSetExecID() ? report.getExecID().getValue() : "N/A";
        String symbol = report.isSetField(Symbol.FIELD) ? report.getSymbol().getValue() : "";

        double price = report.isSetField(Price.FIELD) ? report.getPrice().getValue()
                : (report.isSetField(AvgPx.FIELD) ? report.getAvgPx().getValue() : 0.0);
        int origQty = report.isSetField(OrderQty.FIELD) ? (int) report.getOrderQty().getValue() : 0;
        int filled = report.isSetField(CumQty.FIELD) ? (int) report.getCumQty().getValue() : 0;
        int remaining = report.isSetField(LeavesQty.FIELD) ? (int) report.getLeavesQty().getValue() : Math.max(0, origQty - filled);

        String ordStatusRaw = report.isSetField(OrdStatus.FIELD) ? String.valueOf(report.getOrdStatus().getValue()) : null;
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("A", "Pending");
        statusMap.put("0", "New");
        statusMap.put("1", "Partial Fill");
        statusMap.put("2", "Filled");
        statusMap.put("4", "Canceled");
        statusMap.put("8", "Rejected");
        statusMap.put("F", "Trade Cancel");
        statusMap.put("H", "Trade Bust");

        String execStatus = ordStatusRaw != null ? statusMap.getOrDefault(ordStatusRaw, "Unknown") : "Unknown";

        if (report.isSetField(ExecType.FIELD)) {
            char execTypeChar = report.getExecType().getValue();
            switch (execTypeChar) {
                case ExecType.NEW -> execStatus = "New";
                case ExecType.PARTIAL_FILL -> execStatus = "Partial Fill";
                case ExecType.FILL -> execStatus = "Filled";
                case ExecType.CANCELED -> execStatus = "Canceled";
                case ExecType.REJECTED -> execStatus = "Rejected";
                case ExecType.REPLACED -> execStatus = "Replaced";
                case ExecType.PENDING_NEW -> execStatus = "Pending";
            }
        }

        String customStatus = report.isSetField(9001) ? report.getString(9001) : "N/A";
        double marketRefPrice = report.isSetField(9002) ? report.getDouble(9002) : 0.0;
        int orderRemainingQty = report.isSetField(9003) ? report.getInt(9003) : 0;
        int inventoryAfterTrade = report.isSetField(9004) ? report.getInt(9004) : 0;
        String orderExecutionNote = report.isSetField(9005) ? report.getString(9005) : "N/A";
        String execProgressPercent = report.isSetField(9007) ? report.getString(9007) : "N/A";

        ReceivedData data = new ReceivedData();
        data.setClOrdId(clOrdId);
        data.setOrigClOrdId(origClOrdId);
        data.setExecId(execId);
        data.setSide(side);
        data.setExecType(execStatus);
        data.setPrice(price);
        data.setOriginalQuantity(origQty);
        data.setFilledQuantity(filled);
        data.setRemainingQuantity(remaining);

        data.setCustomStatus(customStatus);
        data.setMarketReferencePrice(marketRefPrice);
        data.setOrderRemainingQuantity(orderRemainingQty);
        data.setInventoryAfterTrade(inventoryAfterTrade);
        data.setOrderExecutionNote(orderExecutionNote);
        data.setExecutionProgressPercent(execProgressPercent);
        data.setSessionID(sessionID);
        data.setSymbol(symbol);

        Platform.runLater(() -> controller.addValue(data));
    }

    // Handler for full refresh snapshot messages
    @Handler
    public void onMessage(MarketDataSnapshotFullRefresh msg, SessionID sessionID) throws FieldNotFound {
        // Symbol can be present at message level
        String symbol = msg.isSetField(Symbol.FIELD) ? msg.getString(Symbol.FIELD) : null;

        int noMDEntries = msg.isSetField(NoMDEntries.FIELD) ? msg.getInt(NoMDEntries.FIELD) : 0;
        for (int i = 1; i <= noMDEntries; i++) {
            Group g = msg.getGroup(i, NoMDEntries.FIELD);

            char mdEntryType = g.isSetField(MDEntryType.FIELD) ? g.getChar(MDEntryType.FIELD) : '\0';
            double price = g.isSetField(MDEntryPx.FIELD) ? g.getDouble(MDEntryPx.FIELD) : 0.0;
            int size = 0;
            if (g.isSetField(MDEntrySize.FIELD)) {
                // MDEntrySize is a double in FIX, cast to int if appropriate
                size = (int) g.getDouble(MDEntrySize.FIELD);
            }

            // Symbol can also be present inside group (depends on provider)
            if (symbol == null && g.isSetField(Symbol.FIELD)) {
                symbol = g.getString(Symbol.FIELD);
            }

            // Only process when symbol is present
            if (symbol != null && !symbol.isBlank()) {
                final String sym = symbol;
                final double px = price;
                final int qty = size;

                if (marketEnquiryController != null) {
                    // update on FX thread via controller method
                    marketEnquiryController.updateMarketData(sym, px, qty);
                }
            }
        }
    }

    // Handler for incremental refresh messages (some providers use this)
    @Handler
    public void onMessage(MarketDataIncrementalRefresh msg, SessionID sessionID) throws FieldNotFound {
        // The incremental refresh has repeating groups, similar parse
        int noMDEntries = msg.isSetField(NoMDEntries.FIELD) ? msg.getInt(NoMDEntries.FIELD) : 0;
        for (int i = 1; i <= noMDEntries; i++) {
            Group g = msg.getGroup(i, NoMDEntries.FIELD);

            char mdEntryType = g.isSetField(MDEntryType.FIELD) ? g.getChar(MDEntryType.FIELD) : '\0';
            double price = g.isSetField(MDEntryPx.FIELD) ? g.getDouble(MDEntryPx.FIELD) : 0.0;
            int size = 0;
            if (g.isSetField(MDEntrySize.FIELD)) {
                size = (int) g.getDouble(MDEntrySize.FIELD);
            }

            String symbol = null;
            if (g.isSetField(Symbol.FIELD)) {
                symbol = g.getString(Symbol.FIELD);
            } else if (msg.isSetField(Symbol.FIELD)) {
                symbol = msg.getString(Symbol.FIELD);
            }

            if (symbol != null && !symbol.isBlank()) {
                final String sym = symbol;
                final double px = price;
                final int qty = size;

                if (marketEnquiryController != null) {
                    marketEnquiryController.updateMarketData(sym, px, qty);
                }
            }
        }
    }

    public void sendOrderReplace(String origClOrdID, String newClOrdID, String symbol,
                                 double newPrice, int newQuantity, char side) {
        if (activeSessionID == null) {
            System.err.println("No active session. Cannot send Order Replace.");
            return;
        }

        quickfix.fix44.OrderCancelReplaceRequest replaceRequest = new quickfix.fix44.OrderCancelReplaceRequest();
        replaceRequest.set(new OrigClOrdID(origClOrdID));
        replaceRequest.set(new ClOrdID(newClOrdID));
        replaceRequest.set(new Side(side));
        replaceRequest.set(new Symbol(symbol));
        replaceRequest.set(new TransactTime());
        replaceRequest.set(new OrdType(OrdType.LIMIT));
        replaceRequest.set(new Price(newPrice));
        replaceRequest.set(new OrderQty(newQuantity));
        replaceRequest.set(new TimeInForce(TimeInForce.DAY));

        try {
            Session.sendToTarget(replaceRequest, activeSessionID);
            System.out.println("Sent Order Replace: " + symbol + " [OrigClOrdID=" + origClOrdID + "]");
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    public void sendMarketDataRequest(String[] symbols) {
        if (activeSessionID == null) {
            System.err.println("No active session. Cannot send MarketDataRequest.");
            return;
        }

        MarketDataRequest mdReq = new MarketDataRequest();
        mdReq.set(new MDReqID("MD_" + System.currentTimeMillis()));
        mdReq.set(new SubscriptionRequestType('1')); //
        mdReq.set(new MarketDepth(1));
        mdReq.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));

        MarketDataRequest.NoMDEntryTypes entryType = new MarketDataRequest.NoMDEntryTypes();
        entryType.set(new MDEntryType(MDEntryType.TRADE));
        mdReq.addGroup(entryType);

        for (String symbol : symbols) {
            MarketDataRequest.NoRelatedSym group = new MarketDataRequest.NoRelatedSym();
            group.set(new Symbol(symbol));
            mdReq.addGroup(group);
        }

        try {
            Session.sendToTarget(mdReq, activeSessionID);
            System.out.println("Sent MarketDataRequest for symbols: " + String.join(",", symbols));
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    public Initiator start() {
        try {
            initiator.start();
            System.out.println("Client started...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return initiator;
    }

    public void stop() {
        if (initiator != null) initiator.stop();
    }
}
