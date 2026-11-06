import java.sql.Connection;
import java.sql.DriverManager;

/**
 * DBConnection Utility
 * Manages database connectivity to MySQL / MariaDB (XAMPP).
 * Demonstrates: JDBC Driver initialization, connection configuration, and exception propagation.
 */
public class DBConnection {

    // Database configuration for XAMPP default setup
    private static final String URL = "jdbc:mysql://localhost:3306/grandstay_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            // Explicitly register MySQL Connector/J driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found! Ensure mysql-connector-j JAR is in classpath.");
        }
    }

    /**
     * Establishes and returns an active Connection object to grandstay_db.
     */
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Quick test method to verify connectivity.
     */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
