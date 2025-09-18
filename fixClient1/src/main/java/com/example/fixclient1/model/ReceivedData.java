package com.example.fixclient1.model;

import quickfix.SessionID;

public class ReceivedData {
    private String clOrdId;
    private String symbol;
    private String side;
    private String execType;
    private Double price;
    private Integer originalQuantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private Integer canceledQuantity;
    private SessionID sessionID;

    public ReceivedData(String clOrdId, String symbol, String side, String execType,
                        Double price, Integer originalQuantity, Integer filledQuantity,
                        Integer remainingQuantity, Integer canceledQuantity, SessionID sessionID) {
        this.clOrdId = clOrdId;
        this.symbol = symbol;
        this.side = side;
        this.execType = execType;
        this.price = price;
        this.originalQuantity = originalQuantity != null ? originalQuantity : 0;
        this.filledQuantity = filledQuantity != null ? filledQuantity : 0;
        this.remainingQuantity = remainingQuantity != null ? remainingQuantity : 0;
        this.canceledQuantity = canceledQuantity != null ? canceledQuantity : 0;
        this.sessionID = sessionID;
    }

    public ReceivedData() {
        this.originalQuantity = 0;
        this.filledQuantity = 0;
        this.remainingQuantity = 0;
        this.canceledQuantity = 0;
    }

    // Getters and setters
    public String getClOrdId() { return clOrdId; }
    public void setClOrdId(String clOrdId) { this.clOrdId = clOrdId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getExecType() { return execType; }
    public void setExecType(String execType) { this.execType = execType; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(Integer originalQuantity) {
        if (this.originalQuantity == null || this.originalQuantity == 0) {
            this.originalQuantity = originalQuantity;
        }
    }

    public Integer getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(Integer filledQuantity) { this.filledQuantity = filledQuantity; }

    public Integer getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public Integer getCanceledQuantity() { return canceledQuantity; }
    public void setCanceledQuantity(Integer canceledQuantity) { this.canceledQuantity = canceledQuantity; }

    public SessionID getSessionID() { return sessionID; }
    public void setSessionID(SessionID sessionID) { this.sessionID = sessionID; }

    @Override
    public String toString() {
        return "ReceivedData{" +
                "clOrdId='" + clOrdId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", execType='" + execType + '\'' +
                ", price=" + price +
                ", originalQuantity=" + originalQuantity +
                ", filledQuantity=" + filledQuantity +
                ", remainingQuantity=" + remainingQuantity +
                ", canceledQuantity=" + canceledQuantity +
                ", sessionID=" + sessionID +
                '}';
    }
}
