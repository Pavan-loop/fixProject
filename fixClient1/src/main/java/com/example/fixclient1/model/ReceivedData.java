package com.example.fixclient1.model;

public class ReceivedData {
    private String symbol;
    private String execType;
    private Double price;
    private Integer quantity;

    public ReceivedData(String symbol, String execType, Double price, Integer quantity) {
        this.symbol = symbol;
        this.execType = execType;
        this.price = price;
        this.quantity = quantity;
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

    @Override
    public String toString() {
        return "ReceivedData{" +
                "symbol='" + symbol + '\'' +
                ", execType=" + execType +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
