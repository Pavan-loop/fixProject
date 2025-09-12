package com.example.fixclient1.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Curd {
    public List<String> getSymbol() {
        List<String> symbol = new ArrayList<>();
        String sql = "select symbol from stock_inventory";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                symbol.add(rs.getString("symbol"));
            }
            return symbol;
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return symbol;
    }

    public Double getPriceBySymbol(String symbol) {
        Double price = null;
        String sql = "SELECT price FROM stock_inventory WHERE symbol = ?"; // assuming "price" column exists
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, symbol);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                price = rs.getDouble("price");
            }
        } catch (Exception e) {
            System.out.println("Error fetching price for symbol " + symbol + ": " + e.getMessage());
        }
        return price;
    }
}
