import java.sql.*;

public class TestDb {
    public static void main(String[] args) throws Exception {
        System.out.println("Loading driver...");
        Class.forName("org.postgresql.Driver");
        System.out.println("Connecting...");
        Connection conn = DriverManager.getConnection(
            "jdbc:postgresql://localhost:55432/ropa", "ropa", "ropa");
        System.out.println("Connected! DB version: " + conn.getMetaData().getDatabaseProductName());
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM garments")) {
            rs.next();
            System.out.println("Garments count: " + rs.getInt(1));
        }
        conn.close();
        System.out.println("Done!");
    }
}
