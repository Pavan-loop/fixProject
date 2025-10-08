package com.example.fixclient1.fix;

import com.example.fixclient1.HelloController;
import com.example.fixclient1.model.ReceivedData;
import com.example.fixclient1.MarketEnquiryController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import quickfix.*;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.MessageFactory;
import quickfix.field.*;
import quickfix.fix44.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * FIX 4.4 client built on QuickFIX/J that manages the initiator lifecycle, session state, and message flow.

 * - Initializes a SocketInitiator from a settings file and maintains the active SessionID.
 * - Cracks inbound application messages:
 *   - ExecutionReport: maps fields (including custom tags 9001–9007) into ReceivedData and updates HelloController on the FX thread.
 *   - MarketDataSnapshotFullRefresh/MarketDataIncrementalRefresh: extracts trade, OHLC, and DMA (5/8/13/50/200) values and updates MarketEnquiryController.
 * - Sends outbound messages:
 *   - OrderCancelReplaceRequest for order amendments.
 *   - MarketDataRequest subscriptions (trade/open/high/low) for one or more symbols with depth 1.
 * - Provides start/stop controls for the initiator and basic admin/app logging hooks.

 * Threading: UI updates are dispatched via Platform.runLater.
 */
public class ClientApp extends MessageCracker implements Application {

    private final HelloController controller;
    private final Initiator initiator;
    private SessionID activeSessionID;
    private final SessionSettings settings;
    private MarketEnquiryController marketEnquiryController;

    private Runnable onLogon;
    private Runnable onLogout;



    public void setOnLogon(Runnable onLogon) {
        this.onLogon = onLogon;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /**
 * Initializes the FIX 4.4 client with a UI controller and prepares the QuickFIX/J initiator.

 * Loads SessionSettings from the provided file, creates file-backed store and log factories,
 * builds a FIX 4.4 MessageFactory, and constructs a SocketInitiator bound to this Application.
 * This constructor does not start the initiator.
 *
 * @param configFile path to the QuickFIX/J settings file used to configure sessions.
 * @param controller UI controller to receive updates from inbound application messages.
 * @throws ConfigError if the settings file cannot be read or contains invalid configuration.
 */
    public ClientApp(String configFile, HelloController controller) throws ConfigError {
        this.controller = controller;
        this.settings = new SessionSettings(configFile);

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory();

        initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    /**
 * Registers the MarketEnquiryController to receive market data updates.

 * When set, onMessage handlers forward trade, OHLC, and DMA values to the controller
 * via updateMarketData. Pass null to disable UI updates.
 *
 * @param controller the MarketEnquiryController to bind, or null to unbind.
 */
    public void setMarketEnquiryController(MarketEnquiryController controller) {
        this.marketEnquiryController = controller;
    }

    /**
 * QuickFIX/J callback invoked when a session is created.

 * Logs the newly created session to stdout; this precedes logon and does not imply authentication.
 *
 * @param sessionID the FIX SessionID of the newly created session.
 */
    @Override public void onCreate(SessionID sessionID) { System.out.println("Client Created: " + sessionID); }

    /**
 * QuickFIX/J callback invoked when the session successfully logs on.

 * Logs the event to stdout and stores the active SessionID for subsequent outbound requests.
 *
 * @param sessionID the FIX SessionID of the logged-on session.
 */
    @Override public void onLogon(SessionID sessionID) {
        System.out.println("Client logon to broker: " + sessionID);
        this.activeSessionID = sessionID;
        if (onLogon != null) {
            onLogon.run(); // notify UI
        }
    }

    /**
 * QuickFIX/J callback invoked when a session logs out.

 * Logs the logout event and, if the given session matches the current active session,
 * clears the active SessionID to avoid using a stale session for outbound messages.
 *
 * @param sessionID the FIX SessionID of the session that logged out.
 */
    @Override public void onLogout(SessionID sessionID) {
        System.out.println("Client logout from the broker: " + sessionID);

        if (onLogout != null) {
            Platform.runLater(onLogout); // just notify UI
        }
        if (activeSessionID != null && activeSessionID.equals(sessionID)) {
            activeSessionID = null;
        }


    }

    public void logoutAndCancelPendingOrders() {
        if (controller != null && activeSessionID != null) {
            controller.cancelPartiallyFilledOrdersOnLogout(activeSessionID);
        }
        stop();
    }


    /**
 * QuickFIX/J callback invoked before an outbound admin message is sent to the counterparty.

 * Logs the admin message to stdout for diagnostics; does not modify the message.
 *
 * @param message   the outbound admin Message (e.g., Logon, Heartbeat).
 * @param sessionID the FIX SessionID associated with the message.
 */
    @Override public void toAdmin(Message message, SessionID sessionID) { System.out.println("Client sending admin message: " + message); }

    /**
 * QuickFIX/J callback invoked when an inbound admin message is received from the counterparty.

 * Logs the admin message to stdout for diagnostics; does not modify session state or the message.
 *
 * @param message   the inbound admin Message (e.g., Logon, Heartbeat, TestRequest, Reject).
 * @param sessionID the FIX SessionID associated with the message.
 */
    @Override public void fromAdmin(Message message, SessionID sessionID) { System.out.println("Client receiving admin message: " + message); }

    /**
 * QuickFIX/J callback invoked before an outbound application message is sent to the counterparty.

 * Logs the application message to stdout for diagnostics; does not alter the message or session state.
 *
 * @param message   the outbound application Message (e.g., NewOrderSingle, MarketDataRequest, OrderCancelReplaceRequest).
 * @param sessionID the FIX SessionID associated with the message.
 */
    @Override public void toApp(Message message, SessionID sessionID) { System.out.println("Message of client to broker: " + message); }

    /**
 * Handles inbound application-level messages from the counterparty.

 * Logs the received FIX message and delegates to MessageCracker to route it
 * to the appropriate onMessage handler (e.g., ExecutionReport, MarketDataSnapshotFullRefresh,
 * MarketDataIncrementalRefresh). Processing errors are caught and logged.
 *
 * @param message   the inbound application Message.
 * @param sessionID the FIX SessionID associated with the message.
 * @throws FieldNotFound           if a required field is missing.
 * @throws IncorrectDataFormat     if a field has an invalid format.
 * @throws IncorrectTagValue       if a field contains an invalid value.
 * @throws UnsupportedMessageType  if the message type is not supported.
 */
    @Override public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            System.out.println("Received from broker: " + message);
            crack(message, sessionID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
 * Handles an inbound FIX 4.4 ExecutionReport, maps relevant fields (incl. custom tags 9001–9007)
 * into a ReceivedData model, and dispatches the update to HelloController on the FX thread.
 * Extracts side, IDs, symbol, price (Price or AvgPx), quantities (OrderQty, CumQty, LeavesQty with fallbacks),
 * and resolves execution status from OrdStatus/ExecType. Reads custom metadata (9001–9005, 9007).
 * Missing fields are handled with sensible defaults.
 * Threading: UI updates are posted via Platform.runLater.
 *
 * @param report    the inbound ExecutionReport to process.
 * @param sessionID the session associated with the message.
 * @throws FieldNotFound if required FIX fields are missing when accessed.
 */
    @Handler
    public void onMessage(ExecutionReport report, SessionID sessionID) throws FieldNotFound {
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



    /**
 * Processes an inbound FIX 4.4 ExecutionReport, extracts core fields and custom tags (9001–9007),
 * maps them into a ReceivedData, and dispatches a UI update to HelloController on the FX thread
 * via Platform.runLater. Derives side, IDs, symbol, price (Price/AvgPx), and quantities
 * (OrderQty, CumQty, LeavesQty) with sensible defaults, and resolves status from OrdStatus/ExecType.
 *
 * @param msg    the inbound ExecutionReport to process.
 * @param sessionID the FIX session associated with the message.
 * @throws FieldNotFound if required FIX fields are missing.
 */
    @Handler
    public void onMessage(MarketDataSnapshotFullRefresh msg, SessionID sessionID) throws FieldNotFound {
        String symbol = msg.isSetField(Symbol.FIELD) ? msg.getString(Symbol.FIELD) : null;

        double open = 0.0, high = 0.0, low = 0.0;
        double dma5 = 0.0, dma8 = 0.0, dma13 = 0.0, dma50 = 0.0, dma200 = 0.0;
        double lastTradePx = 0.0;
        int lastTradeQty = 0;

        int noMDEntries = msg.isSetField(NoMDEntries.FIELD) ? msg.getInt(NoMDEntries.FIELD) : 0;
        for (int i = 1; i <= noMDEntries; i++) {
            Group g = msg.getGroup(i, NoMDEntries.FIELD);

            char mdEntryType = g.isSetField(MDEntryType.FIELD) ? g.getChar(MDEntryType.FIELD) : '\0';
            double price = g.isSetField(MDEntryPx.FIELD) ? g.getDouble(MDEntryPx.FIELD) : 0.0;
            int size = g.isSetField(MDEntrySize.FIELD) ? (int) g.getDouble(MDEntrySize.FIELD) : 0;

            if (symbol == null && g.isSetField(Symbol.FIELD)) {
                symbol = g.getString(Symbol.FIELD);
            }

            switch (mdEntryType) {
                case '4': open = price; break;
                case '7': high = price; break;
                case '8': low = price; break;
                case MDEntryType.TRADE: lastTradePx = price; lastTradeQty = size; break;
            }

            if (g.isSetField(9010)) dma5 = g.getDouble(9010);
            if (g.isSetField(9011)) dma8 = g.getDouble(9011);
            if (g.isSetField(9012)) dma13 = g.getDouble(9012);
            if (g.isSetField(9013)) dma50 = g.getDouble(9013);
            if (g.isSetField(9014)) dma200 = g.getDouble(9014);
        }

        if (symbol != null && !symbol.isBlank() && marketEnquiryController != null) {
            marketEnquiryController.updateMarketData(symbol, lastTradePx, lastTradeQty,
                    dma5, dma8, dma13, dma50, dma200, open, high, low);
        }
    }

    /**
 * Processes an inbound FIX 4.4 ExecutionReport and updates HelloController.

 * Extracts side, IDs, symbol, price (with AvgPx fallback), and quantities (OrderQty/CumQty/LeavesQty),
 * resolves status from OrdStatus/ExecType, reads custom tags 9001–9007 into ReceivedData, and
 * posts the UI update on the FX thread via Platform.runLater. Missing fields use sensible defaults.
 *
 * @param msg    inbound ExecutionReport.
 * @param sessionID the session associated with the message.
 * @throws FieldNotFound if required FIX fields are missing.
 */
    @Handler
    public void onMessage(MarketDataIncrementalRefresh msg, SessionID sessionID) throws FieldNotFound {
        String symbol = null;
        double open = 0.0, high = 0.0, low = 0.0;
        double dma5 = 0.0, dma8 = 0.0, dma13 = 0.0, dma50 = 0.0, dma200 = 0.0;
        double lastTradePx = 0.0;
        int lastTradeQty = 0;

        int noMDEntries = msg.isSetField(NoMDEntries.FIELD) ? msg.getInt(NoMDEntries.FIELD) : 0;
        for (int i = 1; i <= noMDEntries; i++) {
            Group g = msg.getGroup(i, NoMDEntries.FIELD);

            char mdEntryType = g.isSetField(MDEntryType.FIELD) ? g.getChar(MDEntryType.FIELD) : '\0';
            double price = g.isSetField(MDEntryPx.FIELD) ? g.getDouble(MDEntryPx.FIELD) : 0.0;
            int size = g.isSetField(MDEntrySize.FIELD) ? (int) g.getDouble(MDEntrySize.FIELD) : 0;

            if (symbol == null) {
                if (g.isSetField(Symbol.FIELD)) symbol = g.getString(Symbol.FIELD);
                else if (msg.isSetField(Symbol.FIELD)) symbol = msg.getString(Symbol.FIELD);
            }

            switch (mdEntryType) {
                case '4': open = price; break;
                case '7': high = price; break;
                case '8': low = price; break;
                case MDEntryType.TRADE: lastTradePx = price; lastTradeQty = size; break;
            }

            if (g.isSetField(9010)) dma5 = g.getDouble(9010);
            if (g.isSetField(9011)) dma8 = g.getDouble(9011);
            if (g.isSetField(9012)) dma13 = g.getDouble(9012);
            if (g.isSetField(9013)) dma50 = g.getDouble(9013);
            if (g.isSetField(9014)) dma200 = g.getDouble(9014);
        }

        if (symbol != null && !symbol.isBlank() && marketEnquiryController != null) {
            marketEnquiryController.updateMarketData(symbol, lastTradePx, lastTradeQty,
                    dma5, dma8, dma13, dma50, dma200, open, high, low);
        }
    }


    /**
 * Sends a FIX 4.4 OrderCancelReplaceRequest to amend an existing order.

 * Requires an active QuickFIX/J session; otherwise logs an error and returns.
 * Populates OrigClOrdID, new ClOrdID, Side, Symbol, TransactTime, OrdType=LIMIT,
 * Price, OrderQty, and TimeInForce=DAY, then sends via Session.sendToTarget.
 * Any SessionNotFound is caught and logged.
 *
 * @param origClOrdID the original client order ID of the order to amend.
 * @param newClOrdID  the new client order ID for the replacement.
 * @param symbol      the instrument symbol.
 * @param newPrice    the new limit price.
 * @param newQuantity the new total order quantity.
 * @param side        the FIX Side value (e.g., Side.BUY, Side.SELL).
 */
    public void sendOrderReplace(String origClOrdID, String newClOrdID, String symbol,
                                 double newPrice, int newQuantity, char side) {
        if (activeSessionID == null) {
            System.err.println("No active session. Cannot send Order Replace.");
            return;
        }

        OrderCancelReplaceRequest replaceRequest = new OrderCancelReplaceRequest();
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


    /**
 * Sends a FIX 4.4 MarketDataRequest for the given symbols.

 * Requires an active QuickFIX/J session; otherwise logs an error and returns.
 * Builds the request with a timestamped MDReqID, SubscriptionRequestType=1 (snapshot + updates),
 * MarketDepth=1, MDUpdateType=FULL_REFRESH, and MDEntryTypes for trade, open, high, and low, then
 * sends it via Session.sendToTarget. Any SessionNotFound is caught and logged.
 *
 * @param symbols the instrument symbols to subscribe to; each is added as a NoRelatedSym group.
 */
    public void sendMarketDataRequest(String[] symbols) {
        if (activeSessionID == null) {
            System.err.println("No active session. Cannot send MarketDataRequest.");
            return;
        }

        MarketDataRequest mdReq = new MarketDataRequest();
        mdReq.set(new MDReqID("MD_" + System.currentTimeMillis()));
        mdReq.set(new SubscriptionRequestType('1'));
        mdReq.set(new MarketDepth(1));
        mdReq.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));

        char[] mdTypes = new char[] {
                MDEntryType.TRADE,
                '4',
                '7',
                '8'
        };

        for (char type : mdTypes) {
            MarketDataRequest.NoMDEntryTypes entryType = new MarketDataRequest.NoMDEntryTypes();
            entryType.set(new MDEntryType(type));
            mdReq.addGroup(entryType);
        }

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

    /**
 * Starts the QuickFIX/J initiator backing this client.
  * Invokes Initiator.start() and logs a startup message to stdout; any exceptions
 * are caught and printed for diagnostics.
 *
 * @return the underlying Initiator instance (started when successful).
 */
    public Initiator start() {
        try {
            initiator.start();
            System.out.println("Client started...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return initiator;
    }

    public String getClientId() {
        Iterator<SessionID> it = settings.sectionIterator();
        if (it.hasNext()) {
            return it.next().getSenderCompID();
        }
        return null;
    }

    public String getBrokerId() {
        Iterator<SessionID> it = settings.sectionIterator();
        if (it.hasNext()) {
            return it.next().getTargetCompID();
        }
        return null;
    }




    /**
 * Stops the QuickFIX/J initiator backing this client.

 * Invokes Initiator.stop() to log out and disconnect all sessions; no-op if the
 * initiator reference is null.
 */
    public void stop() {
        if (initiator != null) initiator.stop();
    }
}
