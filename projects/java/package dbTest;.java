package dbTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class dbTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/fbreschi", "postgres", "")) {
   		 
//            System.out.println("Java JDBC PostgreSQL Example");
//            // When this class first attempts to establish a connection, it automatically loads any JDBC 4.0 drivers found within 
//            // the class path. Note that your application must manually load any JDBC drivers prior to version 4.0.
////          Class.forName("org.postgresql.Driver"); 
// 
//            System.out.println("Connected to PostgreSQL database!");
//            Statement statement = connection.createStatement();
//            statement.executeUpdate("INSERT INTO test VALUES (4,'Jorge','Medina')");      
//            ResultSet resultSet = statement.executeQuery("SELECT * FROM test");
//            while (resultSet.next()) {
//                System.out.printf("%-2.2s %-2.10s %-2.10s%n",resultSet.getString("ID"), resultSet.getString("Name"),resultSet.getString("Surname"));
//                System.out.println();
//            }
 
        } /*catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC driver not found.");
            e.printStackTrace();
        }*/ catch (SQLException e) {
            System.out.println("Connection failure.");
            e.printStackTrace();
        }
	}
}