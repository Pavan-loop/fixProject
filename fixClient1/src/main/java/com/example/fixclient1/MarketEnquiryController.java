package com.example.fixclient1;

import com.example.fixclient1.database.Curd;
import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.model.MarketDataRow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

public class MarketEnquiryController {

    @FXML private ComboBox<String> symbolDropdown;
    @FXML private TableView<MarketDataRow> tableMarketData;
    @FXML private TableColumn<MarketDataRow, String> mdSymbol;
    @FXML private TableColumn<MarketDataRow, Double> mdPrice;
    @FXML private TableColumn<MarketDataRow, Integer> mdQty;
    @FXML private TableColumn<MarketDataRow, Double> mdDma5;
    @FXML private TableColumn<MarketDataRow, Double> mdDma8;
    @FXML private TableColumn<MarketDataRow, Double> mdDma13;
    @FXML private TableColumn<MarketDataRow, Double> mdDma50;
    @FXML private TableColumn<MarketDataRow, Double> mdDma200;


    private ClientApp clientApp;
    private final ObservableList<MarketDataRow> marketDataList = FXCollections.observableArrayList();
    private final ObservableList<String> symbolList = FXCollections.observableArrayList();


    private static MarketEnquiryController instance;

    public static MarketEnquiryController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() throws IOException {
        instance = this; // Save this instance for external retrieval

        tableMarketData.setItems(marketDataList);
        tableMarketData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        mdSymbol.setCellValueFactory(cell -> cell.getValue().symbolProperty());
        mdPrice.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());
        mdQty.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        mdDma5.setCellValueFactory(cell -> cell.getValue().dma5Property().asObject());
        mdDma8.setCellValueFactory(cell -> cell.getValue().dma8Property().asObject());
        mdDma13.setCellValueFactory(cell -> cell.getValue().dma13Property().asObject());
        mdDma50.setCellValueFactory(cell -> cell.getValue().dma50Property().asObject());
        mdDma200.setCellValueFactory(cell -> cell.getValue().dma200Property().asObject());


        Curd curd = new Curd();
        List<String> symbols = curd.getSymbol();

        symbolList.addAll(symbols);
        symbolDropdown.setItems(symbolList);
    }

    public void setClientApp(ClientApp clientApp) {
        this.clientApp = clientApp;
        if (this.clientApp != null) {
            this.clientApp.setMarketEnquiryController(this);
        }
    }

    @FXML
    private void onRequestMarketData() {
        String symbol = symbolDropdown.getValue();
        if (symbol == null || symbol.isBlank()) {
            showAlert();
            return;
        }

        marketDataList.clear();
        MarketDataRow row = new MarketDataRow(symbol, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0);
        marketDataList.add(row);

        try{
        if (clientApp != null) {
            clientApp.sendMarketDataRequest(new String[]{symbol});
        } else {
            System.err.println("ClientApp not set in MarketEnquiryController.");
        }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateMarketData(String symbol, double price, int quantity , double dma5, double dma8, double dma13, double dma50, double dma200) {
        Platform.runLater(() -> {
            for (MarketDataRow r : marketDataList) {
                if (r.getSymbol().equals(symbol)) {
                    r.setPrice(price);
                    r.setQuantity(quantity);
                    r.setDma5((int) dma5);
                    r.setDma8((int) dma8);
                    r.setDma13((int) dma13);
                    r.setDma50((int) dma50);
                    r.setDma200((int) dma200);
                    tableMarketData.refresh();
                    return;
                }
            }
            marketDataList.add(new MarketDataRow(symbol, price, quantity, dma5, dma8, dma13, dma50, dma200));
        });
    }

    private void showAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Market Enquiry");
        alert.setHeaderText(null);
        alert.setContentText("Please select a symbol.");
        alert.showAndWait();
    }
}
