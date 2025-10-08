package com.example.fixclient1;

import com.example.fixclient1.database.Curd;
import com.example.fixclient1.fix.ClientApp;
import com.example.fixclient1.model.MarketDataRow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.CheckComboBox;

import java.io.IOException;
import java.util.List;

/**
 * JavaFX controller for the Market Enquiry view that subscribes to and displays live market data.
 *
 * Initializes a TableView of MarketDataRow and a ControlsFX CheckComboBox of symbols, loading
 * symbols from the database via Curd during initialize(). When the user requests data, it sends a
 * MarketDataRequest through ClientApp for the selected symbols (showing a warning if none), creating
 * rows as needed. Incoming updates are applied via updateMarketData, which sets price, quantity,
 * DMA (5/8/13/50/200), and OHLC on the FX thread and refreshes the table.
 * Threading: UI updates are dispatched with Platform.runLater.
 * Use setClientApp to bind to the FIX client; getInstance exposes the controller for external wiring.
 *
 * @see ClientApp#sendMarketDataRequest(String[])
 * @see ClientApp#setMarketEnquiryController(MarketEnquiryController)
 * @see MarketDataRow
 */
public class MarketEnquiryController {

    @FXML private CheckComboBox<String> symbolDropdown;
    @FXML private TableView<MarketDataRow> tableMarketData;
    @FXML private TableColumn<MarketDataRow, String> mdSymbol;
    @FXML private TableColumn<MarketDataRow, Double> mdPrice;
    @FXML private TableColumn<MarketDataRow, Integer> mdQty;
    @FXML private TableColumn<MarketDataRow, Double> mdDma5;
    @FXML private TableColumn<MarketDataRow, Double> mdDma8;
    @FXML private TableColumn<MarketDataRow, Double> mdDma13;
    @FXML private TableColumn<MarketDataRow, Double> mdDma50;
    @FXML private TableColumn<MarketDataRow, Double> mdDma200;
    @FXML private TableColumn<MarketDataRow, Double> mdOpen;
    @FXML private TableColumn<MarketDataRow, Double> mdHigh;
    @FXML private TableColumn<MarketDataRow, Double> mdLow;



    private ClientApp clientApp;
    private final ObservableList<MarketDataRow> marketDataList = FXCollections.observableArrayList();
    private final ObservableList<String> symbolList = FXCollections.observableArrayList();


    private static MarketEnquiryController instance;

    /**
 * Returns the current MarketEnquiryController instance created by the FXML loader.
 * Note: The value is assigned in initialize() and may be null if the controller
 * has not been initialized yet.
 *
 * @return the current controller instance, or null if not initialized
 */
    public static MarketEnquiryController getInstance() {
        return instance;
    }

    /**
 * Initializes the controller after FXML loading.
 * - Saves this controller as the static instance.
 * - Binds TableView to marketDataList and sets constrained resize policy.
 * - Sets cell value factories for columns (symbol, price, quantity, DMA 5/8/13/50/200, open, high, low).
 * - Loads symbols via Curd, fills symbolList, and populates the ControlsFX CheckComboBox.
 *
 * @throws IOException if initialization encounters I/O errors
 */
    @FXML
    public void initialize() throws IOException {
        instance = this;

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
        mdOpen.setCellValueFactory(cell -> cell.getValue().openProperty().asObject());
        mdHigh.setCellValueFactory(cell -> cell.getValue().highProperty().asObject());
        mdLow.setCellValueFactory(cell -> cell.getValue().lowProperty().asObject());

        Curd curd = new Curd();
        List<String> symbols = curd.getSymbol();

        symbolList.addAll(symbols);
        symbolDropdown.getItems().addAll(symbolList);
    }

    /**
 * Binds the given ClientApp to this controller and registers it for market data updates.

 * Stores the reference and, if non-null, calls clientApp.setMarketEnquiryController(this)
 * so incoming market data (trade, OHLC, DMA) is forwarded to updateMarketData.
 * If null is provided, the local reference is cleared and no registration occurs.
 *
 * @param clientApp the FIX client to wire to this controller; may be null to clear the reference
 * @see ClientApp#setMarketEnquiryController(MarketEnquiryController)
 */
    public void setClientApp(ClientApp clientApp) {
        this.clientApp = clientApp;
        if (this.clientApp != null) {
            this.clientApp.setMarketEnquiryController(this);
        }
    }

    /**
 * Handles the UI action to request market data for the checked symbols.

 * Reads the selected symbols from the ControlsFX CheckComboBox; if none are selected,
 * shows a warning and returns. For each selected symbol, ensures a MarketDataRow exists,
 * creating a zero-initialized placeholder when absent. If a ClientApp is bound, delegates
 * to send a FIX MarketDataRequest for the selected symbols; otherwise logs an error.
 * Any exceptions during the request are caught and printed.
 *
 * @see ClientApp#sendMarketDataRequest(String[])
 * @see MarketDataRow
 */
    @FXML
    private void onRequestMarketData() {
        List<String> selectedSymbols = symbolDropdown.getCheckModel().getCheckedItems();

        if (selectedSymbols == null || selectedSymbols.isEmpty()) {
            showAlert();
            return;
        }

        for (String symbol : selectedSymbols) {
            boolean exists = marketDataList.stream()
                    .anyMatch(row -> row.getSymbol().equals(symbol));
            if (!exists) {
                marketDataList.add(new MarketDataRow(symbol, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
            }
        }


        try {
            if (clientApp != null) {
                clientApp.sendMarketDataRequest(selectedSymbols.toArray(new String[0]));
            } else {
                System.err.println("ClientApp not set in MarketEnquiryController.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
 * Updates or inserts a MarketDataRow for the given symbol on the FX thread, then refreshes the table.

 * If a row matching the symbol exists, updates price, quantity, DMA (5/8/13/50/200), and OHLC values;
 * otherwise creates and adds a new row. UI changes are dispatched via Platform.runLater.
 *
 * @param symbol the instrument symbol
 * @param price the latest price
 * @param quantity the latest traded quantity
 * @param dma5 the DMA 5 value
 * @param dma8 the DMA 8 value
 * @param dma13 the DMA 13 value
 * @param dma50 the DMA 50 value
 * @param dma200 the DMA 200 value
 * @param open the session open price
 * @param high the session high price
 * @param low the session low price
 */
    public void updateMarketData(String symbol, double price, int quantity , double dma5, double dma8, double dma13, double dma50, double dma200, double open, double high, double low) {
        Platform.runLater(() -> {
            boolean updated = false;
            for (MarketDataRow r : marketDataList) {
                if (r.getSymbol().equals(symbol)) {
                    r.setPrice(price);
                    r.setQuantity(quantity);
                    r.setDma5((int) dma5);
                    r.setDma8((int) dma8);
                    r.setDma13((int) dma13);
                    r.setDma50((int) dma50);
                    r.setDma200((int) dma200);
                    r.setOpen(open);
                    r.setHigh(high);
                    r.setLow(low);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                marketDataList.add(new MarketDataRow(symbol, price, quantity, dma5, dma8, dma13, dma50, dma200, open, high, low));
            }
            tableMarketData.refresh();
        });
    }

    /**
 * Displays a modal JavaFX warning prompting the user to select a symbol.

 * Invoked when no symbols are chosen in the Market Enquiry view; shows a titled
 * alert without a header and waits for user acknowledgment before returning.
 * Must be called on the FX application thread.
 */
    private void showAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Market Enquiry");
        alert.setHeaderText(null);
        alert.setContentText("Please select a symbol.");
        alert.showAndWait();
    }
}
