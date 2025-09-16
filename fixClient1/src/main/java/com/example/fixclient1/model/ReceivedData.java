package com.example.fixclient1.model;

import javafx.beans.property.SimpleIntegerProperty;

public class ReceivedData {
    private String clOrdId;
    private String symbol;
    private String side;
    private String execType;
    private Double price;
    private Integer quantity;
    private Integer remainingQuantity;

    public ReceivedData(String clOrdId, String symbol, String side, String execType,
                        Double price, Integer quantity, Integer remainingQuantity) {
        this.clOrdId = clOrdId;
        this.symbol = symbol;
        this.side = side;
        this.execType = execType;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
    }

    public ReceivedData() {}

    public String getClOrdId() {
        return clOrdId;
    }

    public void setClOrdId(String clOrdId) {
        this.clOrdId = clOrdId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getExecType() {
        return execType;
    }

    public void setExecType(String execType) {
        this.execType = execType;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    @Override
    public String toString() {
        return "ReceivedData{" +
                "clientId='" + clOrdId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", execType='" + execType + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", remainingQuantity=" + remainingQuantity +
                '}';
    }
}
