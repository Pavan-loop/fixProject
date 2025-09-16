package com.example.fixclient1.fix;

import com.example.fixclient1.HelloController;
import com.example.fixclient1.model.ReceivedData;
import javafx.application.Platform;
import quickfix.*;
import quickfix.fix44.ExecutionReport;

import java.util.HashMap;
import java.util.Map;

public class ClientApp extends MessageCracker implements Application {

    private final HelloController controller;
    private SessionSettings settings;
    private final Initiator initiator;


    public ClientApp(String configFile, HelloController controller) throws ConfigError {
        this.controller = controller;
        settings = new SessionSettings(configFile);

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory();

        initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    @Override
    public void onCreate(SessionID sessionID) {
        System.out.println("Client Created: " + sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println("Client logon to broker: " + sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        System.out.println("Client logout from the broker: " + sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        System.out.println("Client sending heartbeat message: " + message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        System.out.println("Client receiving heartbeat message: " + message);
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        System.out.println("Message of client to broker: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public void onMessage(ExecutionReport executionReport, SessionID sessionID) throws FieldNotFound {
        String status = String.valueOf(executionReport.getOrdStatus().getValue());

        Map<String, String> conStatus = new HashMap<>();
        conStatus.put("0", "New");
        conStatus.put("1", "Partial Fill");
        conStatus.put("2", "Fill");
        conStatus.put("4", "Canceled");   // ✅ include cancel code
        conStatus.put("8", "Rejected");
        conStatus.put("F", "Trade Cancel");
        conStatus.put("H", "Trade Bust");

        int cumQty = (int) executionReport.getCumQty().getValue();
        int leavesQty;
        try {
            leavesQty = (int) executionReport.getLeavesQty().getValue();
        } catch (FieldNotFound e) {
            leavesQty = (int) (executionReport.getOrderQty().getValue() - executionReport.getCumQty().getValue());
        }

        char sideChar = executionReport.getSide().getValue();
        String side;
        switch (sideChar) {
            case '1': side = "BUY"; break;
            case '2': side = "SELL"; break;
            case '5': side = "SELL_SHORT"; break;
            default: side = "UNKNOWN"; break;
        }

        // ✅ Use OrigClOrdID if status = 4 (Canceled)
        String clOrdId = executionReport.getClOrdID().getValue();
        if ("4".equals(status) && executionReport.isSetField(41)) { // 41 = OrigClOrdID
            clOrdId = executionReport.getOrigClOrdID().getValue();
        }

        ReceivedData data = new ReceivedData(
                clOrdId,   // ✅ now maps back to original order row
                executionReport.getSymbol().getValue(),
                side,
                conStatus.getOrDefault(status, status),
                executionReport.isSetAvgPx() ? executionReport.getAvgPx().getValue() : 0.0,
                cumQty,
                leavesQty
        );

        Platform.runLater(() -> controller.addValue(data));
    }



    public Initiator start() {
        try {
            initiator.start();
            System.out.println("Client started...");
            return initiator;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return initiator;
    }

    public void stop() {
        if (initiator != null) {
            initiator.stop();
        }
    }
}
