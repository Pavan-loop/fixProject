package com.example.fixclient1.fix;

import quickfix.*;
import quickfix.fix44.ExecutionReport;

public class ClientApp extends MessageCracker implements Application {

    private SessionSettings settings;
    private final Initiator initiator;


    public ClientApp(String configFile) throws ConfigError {
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
        System.out.println("Client receives execution report " +
                    "OrderId=" + executionReport.getOrderID().getValue() +
                    ", Status=" + executionReport.getOrdStatus().getValue() +
                    ", FilledQty=" + executionReport.getCumQty().getValue() +
                    ", AvgPx=" + executionReport.getAvgPx().getValue());
    }

    public void start() {
        try {
            initiator.start();
            System.out.println("Client started...");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void stop() {
        if (initiator != null) {
            initiator.stop();
        }
    }
}
