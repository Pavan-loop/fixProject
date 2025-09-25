package com.example.fixclient1;

import com.example.fixclient1.database.Curd;
import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.model.MarketDataRow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

public class MarketEnquiryController {

    @FXML private ComboBox<String> symbolDropdown;
    @FXML private TableView<MarketDataRow> tableMarketData;
    @FXML private TableColumn<MarketDataRow, String> mdSymbol;
    @FXML private TableColumn<MarketDataRow, Double> mdPrice;
    @FXML private TableColumn<MarketDataRow, Integer> mdQty;

    private ClientApp clientApp;
    private final ObservableList<MarketDataRow> marketDataList = FXCollections.observableArrayList();
    private final ObservableList<String> symbolList = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws IOException {
        tableMarketData.setItems(marketDataList);
        tableMarketData.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        mdSymbol.setCellValueFactory(cell -> cell.getValue().symbolProperty());
        mdPrice.setCellValueFactory(cell -> cell.getValue().priceProperty().asObject());
        mdQty.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());

        Curd curd = new Curd();
        List<String> symbols = curd.getSymbol();

        for(String s : symbols) {
            symbolList.add(s);
        }
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
            showAlert("Market Enquiry", "Please select a symbol.");
            return;
        }

        // Show a placeholder row immediately
        marketDataList.clear();
        MarketDataRow row = new MarketDataRow(symbol, 0.0, 0);
        marketDataList.add(row);

        if (clientApp != null) {
            clientApp.sendMarketDataRequest(new String[]{symbol});
        } else {
            System.err.println("ClientApp not set in MarketEnquiryController.");
        }
    }

    // Called by ClientApp when market data arrives
    public void updateMarketData(String symbol, double price, int quantity) {
        Platform.runLater(() -> {
            // Find an existing row with the same symbol
            for (MarketDataRow r : marketDataList) {
                if (r.getSymbol().equals(symbol)) {
                    r.setPrice(price);
                    r.setQuantity(quantity);
                    tableMarketData.refresh();
                    return;
                }
            }
            // If not present, add a new row
            marketDataList.add(new MarketDataRow(symbol, price, quantity));
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
