package com.example.fixclient1.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TableReceivedData {
    private SimpleStringProperty clientOrdId;
    private SimpleStringProperty symbol;
    private SimpleStringProperty side;
    private SimpleStringProperty execType;
    private SimpleDoubleProperty price;
    private SimpleIntegerProperty quantity;

    public TableReceivedData(String clientOrdId, String symbol, String side, String execType, Double price, Integer quantity) {
        this.clientOrdId = new SimpleStringProperty(clientOrdId);
        this.symbol = new SimpleStringProperty(symbol);
        this.side = new SimpleStringProperty(side);
        this.execType = new SimpleStringProperty(execType);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
    }

    public String getSymbol() {
        return symbol.get();
    }

    public SimpleStringProperty symbolProperty() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol.set(symbol);
    }

    public String getExecType() {
        return execType.get();
    }

    public SimpleStringProperty execTypeProperty() {
        return execType;
    }

    public void setExecType(String execType) {
        this.execType.set(execType);
    }

    public double getPrice() {
        return price.get();
    }

    public SimpleDoubleProperty priceProperty() {
        return price;
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    public int getQuantity() {
        return quantity.get();
    }

    public SimpleIntegerProperty quantityProperty() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public String getSide() {
        return side.get();
    }

    public SimpleStringProperty sideProperty() {
        return side;
    }

    public void setSide(String side) {
        this.side.set(side);
    }

    public String getClientOrdId() {
        return clientOrdId.get();
    }

    public SimpleStringProperty clientOrdIdProperty() {
        return clientOrdId;
    }

    public void setClientOrdId(String clientOrdId) {
        this.clientOrdId.set(clientOrdId);
    }

    @Override
    public String toString() {
        return "TableReceivedData{" +
                "client Id=" + clientOrdId +
                ", symbol=" + symbol +
                ", side=" + side +
                ", execType=" + execType +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
