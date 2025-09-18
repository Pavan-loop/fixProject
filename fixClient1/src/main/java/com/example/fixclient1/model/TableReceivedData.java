package com.example.fixclient1.model;

import javafx.beans.property.*;
import quickfix.SessionID;

public class TableReceivedData {
    private final SimpleStringProperty clientOrdId;
    private final SimpleStringProperty symbol;
    private final SimpleStringProperty side;
    private final SimpleStringProperty execType;
    private final SimpleDoubleProperty price;
    private final SimpleIntegerProperty originalQuantity;
    private final SimpleIntegerProperty filledQuantity;
    private final SimpleIntegerProperty remainingQuantity;
    private final SimpleIntegerProperty canceledQuantity;
    private SessionID sessionID;

    public TableReceivedData(String clientOrdId, String symbol, String side,
                             String execType, double price, int originalQuantity, int filledQuantity,
                             int remainingQuantity, int canceledQuantity, SessionID sessionID) {
        this.clientOrdId = new SimpleStringProperty(clientOrdId);
        this.symbol = new SimpleStringProperty(symbol);
        this.side = new SimpleStringProperty(side);
        this.execType = new SimpleStringProperty(execType);
        this.price = new SimpleDoubleProperty(price);
        this.originalQuantity = new SimpleIntegerProperty(originalQuantity);
        this.filledQuantity = new SimpleIntegerProperty(filledQuantity);
        this.remainingQuantity = new SimpleIntegerProperty(remainingQuantity);
        this.canceledQuantity = new SimpleIntegerProperty(canceledQuantity);
        this.sessionID = sessionID;
    }

    // TableView bindings
    public String getClientOrdId() { return clientOrdId.get(); }
    public SimpleStringProperty clientOrdIdProperty() { return clientOrdId; }
    public void setClientOrdId(String clientOrdId) { this.clientOrdId.set(clientOrdId); }

    public String getSymbol() { return symbol.get(); }
    public SimpleStringProperty symbolProperty() { return symbol; }
    public void setSymbol(String symbol) { this.symbol.set(symbol); }

    public String getSide() { return side.get(); }
    public SimpleStringProperty sideProperty() { return side; }
    public void setSide(String side) { this.side.set(side); }

    public String getExecType() { return execType.get(); }
    public SimpleStringProperty execTypeProperty() { return execType; }
    public void setExecType(String execType) { this.execType.set(execType); }

    public double getPrice() { return price.get(); }
    public SimpleDoubleProperty priceProperty() { return price; }
    public void setPrice(double price) { this.price.set(price); }

    public int getOriginalQuantity() { return originalQuantity.get(); }
    public SimpleIntegerProperty originalQuantityProperty() { return originalQuantity; }
    public void setOriginalQuantity(int originalQuantity) { this.originalQuantity.set(originalQuantity); }

    public int getFilledQuantity() { return filledQuantity.get(); }
    public SimpleIntegerProperty filledQuantityProperty() { return filledQuantity; }
    public void setFilledQuantity(int filledQuantity) { this.filledQuantity.set(filledQuantity); }

    public int getRemainingQuantity() { return remainingQuantity.get(); }
    public SimpleIntegerProperty remainingQuantityProperty() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity.set(remainingQuantity); }

    public int getCanceledQuantity() { return canceledQuantity.get(); }
    public SimpleIntegerProperty canceledQuantityProperty() { return canceledQuantity; }
    public void setCanceledQuantity(int canceledQuantity) { this.canceledQuantity.set(canceledQuantity); }

    public SessionID getSessionID() { return sessionID; }
    public void setSessionID(SessionID sessionID) { this.sessionID = sessionID; }

    @Override
    public String toString() {
        return "TableReceivedData{" +
                "clientOrdId=" + clientOrdId.get() +
                ", symbol=" + symbol.get() +
                ", side=" + side.get() +
                ", execType=" + execType.get() +
                ", price=" + price.get() +
                ", originalQuantity=" + originalQuantity.get() +
                ", filledQuantity=" + filledQuantity.get() +
                ", remainingQuantity=" + remainingQuantity.get() +
                ", canceledQuantity=" + canceledQuantity.get() +
                ", sessionID=" + sessionID +
                '}';
    }
}
