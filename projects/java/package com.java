package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseWebApp {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/fbreschi";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new MainHandler());
        server.createContext("/insert", new InsertHandler());
        server.createContext("/delete", new DeleteHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 8080");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.stop(0);
            System.out.println("Server stopped.");
        }));
    }

    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = generateHtml();
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class InsertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                Map<String, String> params = parseFormData(t);
                String name = params.get("name");
                String surname = params.get("surname");
                insertRecord(name, surname);
            }
            t.getResponseHeaders().add("Location", "/");
            t.sendResponseHeaders(302, -1);
        }
    }

    static class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                Map<String, String> params = parseFormData(t);
                String id = params.get("id");
                deleteRecord(id);
            }
            t.getResponseHeaders().add("Location", "/");
            t.sendResponseHeaders(302, -1);
        }
    }

    private static int getNextId() {
        String sql = "SELECT MAX(id) FROM test";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1; // If table is empty or error occurred
    }

    private static void insertRecord(String name, String surname) {
        String sql = "INSERT INTO test (id, Name, Surname) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int nextId = getNextId();
            pstmt.setInt(1, nextId);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void deleteRecord(String id) {
        String sql = "DELETE FROM test WHERE ID = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(id));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<Record> getRecords() {
        List<Record> records = new ArrayList<>();
        String sql = "SELECT * FROM test";
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Record record = new Record(
                    resultSet.getInt("ID"),
                    resultSet.getString("Name"),
                    resultSet.getString("Surname")
                );
                records.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    private static String generateHtml() {
        List<Record> records = getRecords();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Database Web App</title></head><body>");
        html.append("<h1>Database Records</h1>");
        html.append("<table border='1'><tr><th>ID</th><th>Name</th><th>Surname</th><th>Action</th></tr>");
        for (Record record : records) {
            html.append("<tr><td>").append(record.getId()).append("</td>");
            html.append("<td>").append(record.getName()).append("</td>");
            html.append("<td>").append(record.getSurname()).append("</td>");
            html.append("<td><form method='post' action='/delete'>");
            html.append("<input type='hidden' name='id' value='").append(record.getId()).append("'>");
            html.append("<input type='submit' value='Delete'></form></td></tr>");
        }
        html.append("</table>");
        html.append("<h2>Insert New Record</h2>");
        html.append("<form method='post' action='/insert'>");
        html.append("Name: <input type='text' name='name' required><br>");
        html.append("Surname: <input type='text' name='surname' required><br>");
        html.append("<input type='submit' value='Insert'>");
        html.append("</form>");
        html.append("</body></html>");
        return html.toString();
    }

    private static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        String formData = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : formData.split("&")) {
            String[] keyValue = pair.split("=");
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    static class Record {
        private final int id;
        private final String name;
        private final String surname;

        Record(int id, String name, String surname) {
            this.id = id;
            this.name = name;
            this.surname = surname;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getSurname() { return surname; }
    }
}the dr