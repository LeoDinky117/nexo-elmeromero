package com.nexo.app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    //private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=Nexo;encrypt=true;trustServerCertificate=true";
    private static final String URL = "jdbc:mariadb://localhost:3306/Nexo";
    //private static final String USER = "sa";
    private static final String USER = "root";
    //private static final String PASSWORD = "1234";
    private static final String PASSWORD = "root117";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}