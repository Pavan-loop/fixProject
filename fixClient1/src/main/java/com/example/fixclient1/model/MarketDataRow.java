package com.example.fixclient1.model;

import javafx.beans.property.*;

public class MarketDataRow {

    private final StringProperty symbol;
    private final DoubleProperty price;
    private final IntegerProperty quantity;

    public MarketDataRow(String symbol, double price, int quantity) {
        this.symbol = new SimpleStringProperty(symbol);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
    }


    public String getSymbol() { return symbol.get(); }
    public void setSymbol(String symbol) { this.symbol.set(symbol); }
    public StringProperty symbolProperty() { return symbol; }

    public double getPrice() { return price.get(); }
    public void setPrice(double price) { this.price.set(price); }
    public DoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public IntegerProperty quantityProperty() { return quantity; }
}
