package com.example.fixclient1.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TableReceivedData {
    private SimpleStringProperty symbol;
    private SimpleStringProperty execType;
    private SimpleDoubleProperty price;
    private SimpleIntegerProperty quantity;

    public TableReceivedData(String symbol, String execType, Double price, Integer quantity) {
        this.symbol = new SimpleStringProperty(symbol);
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

    @Override
    public String toString() {
        return "TableReceivedData{" +
                "symbol=" + symbol +
                ", execType=" + execType +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
