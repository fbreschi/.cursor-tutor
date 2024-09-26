package com.example.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
// host=127.0.0.1 port=5432 dbname=fbreschi user=postgres password=xxxxxxx sslmode=prefer connect_timeout=10 //

    private static final String DB_USER = "fbreschi";
    private static final String DB_PASSWORD = "postgres";

    public static void main(String[] args) throws IOException {
        createTable();
        insertSampleData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = generateHtml();
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static void createTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users " +
                    "(id SERIAL PRIMARY KEY, " +
                    " name TEXT NOT NULL, " +
                    " email TEXT NOT NULL UNIQUE)";
            stmt.executeUpdate(sql);
            System.out.println("Table created successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertSampleData() {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT (email) DO NOTHING";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String[][] users = {
                {"Alice", "alice@example.com"},
                {"Bob", "bob@example.com"},
                {"Charlie", "charlie@example.com"}
            };

            for (String[] user : users) {
                pstmt.setString(1, user[0]);
                pstmt.setString(2, user[1]);
                pstmt.executeUpdate();
            }
            System.out.println("Sample data inserted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<User> getUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private static String generateHtml() {
        List<User> users = getUsers();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>User Database</title></head><body>");
        html.append("<h1>User Database</h1>");
        html.append("<table border='1'><tr><th>ID</th><th>Name</th><th>Email</th></tr>");
        for (User user : users) {
            html.append("<tr><td>").append(user.getId()).append("</td>");
            html.append("<td>").append(user.getName()).append("</td>");
            html.append("<td>").append(user.getEmail()).append("</td></tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }

    static class User {
        private final long id;
        private final String name;
        private final String email;

        User(long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
}
