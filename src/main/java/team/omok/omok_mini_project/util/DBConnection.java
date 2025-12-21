package team.omok.omok_mini_project.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL =
            "jdbc:postgresql://192.168.56.101:5432/omok";
    private static final String USER = "omokuser";
    private static final String PASSWORD = "omokpass";

    public static Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
