package com.example.fixclient1;

import com.example.fixclient1.fix.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import quickfix.ConfigError;
import quickfix.Initiator;

public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private HelloController helloIncludeRootController;

    @FXML
    private Label connectionInfoLabel;

    private Initiator initiator;
    private ClientApp clientApp;

    @FXML
    private void onLogin() {
        try {
            if (clientApp == null) {
                clientApp = new ClientApp(
                        "C:\\Users\\nichiuser\\Downloads\\fixProject\\fixClient1\\src\\main\\java\\com\\example\\fixclient1\\fix\\initiator.cfg",
                        helloIncludeRootController
                );

                clientApp.setOnLogon(() ->
                        Platform.runLater(() -> {
                            statusLabel.setText("Connected ✅");
                            statusLabel.getStyleClass().removeAll("status-disconnected");
                            if (!statusLabel.getStyleClass().contains("status-connected")) {
                                statusLabel.getStyleClass().add("status-connected");
                            }

                            String clientId = clientApp.getClientId();
                            String brokerId = clientApp.getBrokerId();
                            connectionInfoLabel.setText("Client: " + clientId + " | Broker: " + brokerId);
                        })
                );

                clientApp.setOnLogout(() ->
                        Platform.runLater(() -> {
                            statusLabel.setText("Disconnected ❌");
                            statusLabel.getStyleClass().removeAll("status-connected");
                            if (!statusLabel.getStyleClass().contains("status-disconnected")) {
                                statusLabel.getStyleClass().add("status-disconnected");
                            }

                            connectionInfoLabel.setText("");
                        })
                );

                Platform.runLater(() -> {
                    MarketEnquiryController marketEnquiryController = MarketEnquiryController.getInstance();
                    if (marketEnquiryController != null) {
                        marketEnquiryController.setClientApp(clientApp);
                        clientApp.setMarketEnquiryController(marketEnquiryController);
                    } else {
                        System.out.println("MarketEnquiryController instance is null");
                    }
                });

                initiator = clientApp.start();
                helloIncludeRootController.setInitiator(initiator);
                helloIncludeRootController.setClientApp(clientApp);
            }

            showAlert("Login", "Logon request sent.");
        } catch (ConfigError e) {
            e.printStackTrace();
            showAlert("Error", "Failed to start FIX client: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        if (clientApp != null) {
            clientApp.logoutAndCancelPendingOrders();
            clientApp = null;
            // Set status label to disconnected (red) style
            Platform.runLater(() -> {
                statusLabel.setText("Disconnected ❌");
                statusLabel.getStyleClass().removeAll("status-connected");
                if (!statusLabel.getStyleClass().contains("status-disconnected")) {
                    statusLabel.getStyleClass().add("status-disconnected");
                }
                connectionInfoLabel.setText("");
            });
            showAlert("Logout", "Logout request sent.");
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
