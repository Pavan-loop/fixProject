package com.example.fixclient1.model;

import javafx.beans.property.*;

public class MarketDataRow {

    private final StringProperty symbol;
    private final DoubleProperty price;
    private final IntegerProperty quantity;
    private final DoubleProperty dma5;
    private final DoubleProperty dma8;
    private final DoubleProperty dma13;
    private final DoubleProperty dma50;
    private final DoubleProperty dma200;
    private final DoubleProperty open;
    private final DoubleProperty high;
    private final DoubleProperty low;

    public MarketDataRow(String symbol, double price, int quantity,
                         double dma5, double dma8, double dma13,
                         double dma50, double dma200, double open, double high, double low) {
        this.symbol = new SimpleStringProperty(symbol);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.dma5 = new SimpleDoubleProperty(dma5);
        this.dma8 = new SimpleDoubleProperty(dma8);
        this.dma13 = new SimpleDoubleProperty(dma13);
        this.dma50 = new SimpleDoubleProperty(dma50);
        this.dma200 = new SimpleDoubleProperty(dma200);
        this.open = new SimpleDoubleProperty(open);
        this.high = new SimpleDoubleProperty(high);
        this.low = new SimpleDoubleProperty(low);
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

    public double getDma5() { return dma5.get(); }
    public void setDma5(int dma5) { this.dma5.set(dma5); }
    public DoubleProperty dma5Property() { return dma5; }

    public double getDma8() { return dma8.get(); }
    public void setDma8(int dma8) { this.dma8.set(dma8); }
    public DoubleProperty dma8Property() { return dma8; }

    public double getDma13() { return dma13.get(); }
    public void setDma13(int dma13) { this.dma13.set(dma13); }
    public DoubleProperty dma13Property() { return dma13; }

    public double getDma50() { return dma50.get(); }
    public void setDma50(int dma50) { this.dma50.set(dma50); }
    public DoubleProperty dma50Property() { return dma50; }

    public double getDma200() { return dma200.get(); }
    public void setDma200(int dma200) { this.dma200.set(dma200); }
    public DoubleProperty dma200Property() { return dma200;}

    public double getOpen() { return open.get(); }
    public void setOpen(double open) { this.open.set(open); }
    public DoubleProperty openProperty() { return open; }

    public double getHigh() { return high.get(); }
    public void setHigh(double high) { this.high.set(high); }
    public DoubleProperty highProperty() { return high; }

    public double getLow() { return low.get(); }
    public void setLow(double low) { this.low.set(low); }
    public DoubleProperty lowProperty() { return low; }

    @Override
    public String toString() {
        return "MarketDataRow{" +
                "symbol=" + symbol +
                ", price=" + price +
                ", quantity=" + quantity +
                ", dma5=" + dma5 +
                ", dma8=" + dma8 +
                ", dma13=" + dma13 +
                ", dma50=" + dma50 +
                ", dma200=" + dma200 +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                '}';
    }
}
