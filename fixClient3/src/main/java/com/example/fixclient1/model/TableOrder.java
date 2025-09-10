package com.example.fixclient1.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TableOrder {
    private SimpleStringProperty symbol;
    private SimpleStringProperty side;
    private SimpleStringProperty orderType;
    private SimpleDoubleProperty orderPrice;
    private SimpleIntegerProperty orderQuantity;

    public TableOrder(String symbol, String side, String orderType, Double orderPrice, Integer orderQuantity) {
        this.symbol = new SimpleStringProperty(symbol);
        this.side = new SimpleStringProperty(side);
        this.orderType = new SimpleStringProperty(orderType);
        this.orderPrice = new SimpleDoubleProperty(orderPrice);
        this.orderQuantity = new SimpleIntegerProperty(orderQuantity);
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

    public String getSide() {
        return side.get();
    }

    public SimpleStringProperty sideProperty() {
        return side;
    }

    public void setSide(String side) {
        this.side.set(side);
    }

    public String getOrderType() {
        return orderType.get();
    }

    public SimpleStringProperty orderTypeProperty() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType.set(orderType);
    }

    public double getOrderPrice() {
        return orderPrice.get();
    }

    public SimpleDoubleProperty orderPriceProperty() {
        return orderPrice;
    }

    public void setOrderPrice(double orderPrice) {
        this.orderPrice.set(orderPrice);
    }

    public int getOrderQuantity() {
        return orderQuantity.get();
    }

    public SimpleIntegerProperty orderQuantityProperty() {
        return orderQuantity;
    }

    public void setOrderQuantity(int orderQuantity) {
        this.orderQuantity.set(orderQuantity);
    }

    @Override
    public String toString() {
        return "TableOrder{" +
                "symbol=" + symbol +
                ", side=" + side +
                ", orderType=" + orderType +
                ", orderPrice=" + orderPrice +
                ", orderQuantity=" + orderQuantity +
                '}';
    }
}
