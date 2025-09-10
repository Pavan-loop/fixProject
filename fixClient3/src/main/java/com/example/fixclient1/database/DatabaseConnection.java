package com.example.fixclient1.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private final static String URL = "jdbc:postgresql://192.168.1.92:5432/nichisoft_db";
    private final static String USERNAME = "keerthanhv";
    private final static String PASSWORD = "nichi-in";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
