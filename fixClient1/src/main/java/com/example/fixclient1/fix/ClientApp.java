package com.example.fixclient1.fix;

import com.example.fixclient1.HelloController;
import com.example.fixclient1.model.ReceivedData;
import javafx.application.Platform;
import quickfix.*;
import quickfix.field.OrigClOrdID;
import quickfix.field.Side;
import quickfix.fix44.ExecutionReport;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ClientApp extends MessageCracker implements Application {

    private final HelloController controller;
    private final Initiator initiator;

    public ClientApp(String configFile, HelloController controller) throws ConfigError {
        this.controller = controller;
        SessionSettings settings = new SessionSettings(configFile);

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory();

        initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    @Override public void onCreate(SessionID sessionID) { System.out.println("Client Created: " + sessionID); }
    @Override public void onLogon(SessionID sessionID) { System.out.println("Client logon to broker: " + sessionID); }
    @Override public void onLogout(SessionID sessionID) { System.out.println("Client logout from the broker: " + sessionID); }
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

        String ordStatus = String.valueOf(report.getOrdStatus().getValue());
        char sideChar = report.getSide().getValue();
        String side = switch (sideChar) {
            case Side.BUY -> "BUY";
            case Side.SELL -> "SELL";
            case Side.SELL_SHORT -> "SELL_SHORT";
            default -> "UNKNOWN";
        };
        String clOrdId = report.getClOrdID().getValue();
        double price = report.isSetPrice() ? report.getPrice().getValue()
                : report.isSetAvgPx() ? report.getAvgPx().getValue() : 0.0;
        int origQty = report.isSetOrderQty() ? (int) report.getOrderQty().getValue() : 0;
        int filled = report.isSetCumQty() ? (int) report.getCumQty().getValue() : 0;
        int remaining = report.isSetLeavesQty() ? (int) report.getLeavesQty().getValue() : Math.max(0, origQty - filled);
        String execId = report.isSetExecID() ? report.getExecID().getValue() : "N/A";


        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("0", "New");
        statusMap.put("1", "Partial Fill");
        statusMap.put("2", "Filled");
        statusMap.put("4", "Canceled");
        statusMap.put("8", "Rejected");
        statusMap.put("F", "Trade Cancel");
        statusMap.put("H", "Trade Bust");

        String execStatus = statusMap.getOrDefault(ordStatus, "Unknown");


        String customStatus = report.isSetField(9001) ? report.getString(9001) : "N/A";
        double marketRefPrice = report.isSetField(9002) ? report.getDouble(9002) : 0.0;
        int orderRemainingQty = report.isSetField(9003) ? report.getInt(9003) : 0;
        int inventoryAfterTrade = report.isSetField(9004) ? report.getInt(9004) : 0;
        String orderExecutionNote = report.isSetField(9005) ? report.getString(9005) : "N/A";
        String execProgressPercent = report.isSetField(9007) ? report.getString(9007) : "N/A";


        ReceivedData data = new ReceivedData();
        data.setClOrdId(clOrdId);
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

        Platform.runLater(() -> controller.addValue(data));
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
