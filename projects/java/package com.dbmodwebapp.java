package com.dbmodwebapp.dao;


import com.dbmodwebapp.main.DatabaseWebApp;
import com.dbmodwebapp.model.Record;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecordDAO {
    public List<Record> getRecords() {
        List<Record> records = new ArrayList<>();
        String sql = "SELECT * FROM test ORDER BY ID";
        try (Connection connection = DriverManager.getConnection(DatabaseWebApp.DB_URL, DatabaseWebApp.DB_USER, DatabaseWebApp.DB_PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                records.add(new Record(
                        resultSet.getInt("ID"),
                        resultSet.getString("Name"),
                        resultSet.getString("Surname")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public String insertRecord(String name, String surname) {
        String sql = "INSERT INTO test (ID, Name, Surname) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DatabaseWebApp.DB_URL, DatabaseWebApp.DB_USER, DatabaseWebApp.DB_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int nextId = getNextId(connection);
            pstmt.setInt(1, nextId);
            pstmt.setString(2, name);
            pstmt.setString(3, surname);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return "Insert failed, no rows affected.";
            }
            return null; // Successful insert
        } catch (SQLException e) {
            e.printStackTrace();
            return "SQL Error: " + e.getMessage();
        }
    }

    public void deleteRecord(int id) {
        String sql = "DELETE FROM test WHERE ID = ?";
        try (Connection connection = DriverManager.getConnection(DatabaseWebApp.DB_URL, DatabaseWebApp.DB_USER, DatabaseWebApp.DB_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void truncateTable() {
        String sql = "TRUNCATE TABLE test";
        try (Connection connection = DriverManager.getConnection(DatabaseWebApp.DB_URL, DatabaseWebApp.DB_USER, DatabaseWebApp.DB_PASSWORD);
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String importCSV(java.io.InputStream fileContent) {
        String sql = "INSERT INTO test (ID, Name, Surname) VALUES (?, ?, ?)";
        try (java.util.Scanner scanner = new java.util.Scanner(fileContent);
             Connection connection = DriverManager.getConnection(DatabaseWebApp.DB_URL, DatabaseWebApp.DB_USER, DatabaseWebApp.DB_PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                String[] data = line.split(",");
                if (data.length == 3) {
                    try {
                        int id = Integer.parseInt(data[0].trim());
                        pstmt.setInt(1, id);
                        pstmt.setString(2, data[1].trim());
                        pstmt.setString(3, data[2].trim());
                        pstmt.executeUpdate();
                    } catch (NumberFormatException e) {
                        // Skip lines where ID is not a valid integer
                        continue;
                    }
                }
            }
            return null; // Successful import
        } catch (SQLException e) {
            e.printStackTrace();
            return "SQL Error: " + e.getMessage();
        }
    }

    private int getNextId(Connection connection) throws SQLException {
        String sql = "SELECT MAX(ID) FROM test";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;
            }
        }
    }

package com.dbmodwebapp.main;

public class DatabaseWebApp {
    public static final String DB_URL = "jdbc:postgresql://172.18.0.2:5432/postgres";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "postgres";
}

package com.dbmodwebapp.model;

public class Record {
    private final int id;
    private final String name;
    private final String surname;

    public Record(int id, String name, String surname) {
        this.id = id;
        this.name = name;
        this.surname = surname;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.dao.RecordDAO;

@WebServlet("/delete")
public class DeleteServlet extends HttpServlet {
    private RecordDAO recordDAO = new RecordDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        recordDAO.deleteRecord(id);
        response.sendRedirect(request.getContextPath() + "/records");
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.util.HTMLGenerator;

@WebServlet("/")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.write(HTMLGenerator.generateHeader("Home", request));
        out.write("<h1>Welcome to the Database Web App</h1>");
        out.write("<p>This is a simple web application to manage database records.</p>");
        out.write("<p>Use the navigation menu to access different features:</p>");
        out.write("<ul>");
        out.write("<li><a href='" + request.getContextPath() + "/records'>Database Records</a> - View and manage records</li>");
        out.write("<li><a href='" + request.getContextPath() + "/maintenance'>Maintenance</a> - Import CSV and truncate table</li>");
        out.write("</ul>");
        out.write(HTMLGenerator.generateFooter());
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.dao.RecordDAO;

@WebServlet("/import")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,    // 1 MB
    maxFileSize = 1024 * 1024 * 10,     // 10 MB
    maxRequestSize = 1024 * 1024 * 15   // 15 MB
)
public class ImportServlet extends HttpServlet {
    private RecordDAO recordDAO = new RecordDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Part filePart = request.getPart("csvFile");
            if (filePart != null) {
                try (InputStream fileContent = filePart.getInputStream()) {
                    String result = recordDAO.importCSV(fileContent);
                    if (result != null) {
                        // If there's an error, display it
                        response.setContentType("text/html");
                        PrintWriter out = response.getWriter();
                        out.println("<html><body>");
                        out.println("<h2>Error importing CSV</h2>");
                        out.println("<p>" + result + "</p>");
                        out.println("<a href='" + request.getContextPath() + "/records'>Back to records</a>");
                        out.println("</body></html>");
                        return;
                    }
                }
            }
            response.sendRedirect(request.getContextPath() + "/records");
        } catch (Exception e) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h2>Error processing file upload</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("<a href='" + request.getContextPath() + "/records'>Back to records</a>");
            out.println("</body></html>");
        }
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.dao.RecordDAO;

@WebServlet("/insert")
public class InsertServlet extends HttpServlet {
    private RecordDAO recordDAO = new RecordDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String surname = request.getParameter("surname");
        String result = recordDAO.insertRecord(name, surname);
        if (result != null) {
            // If there's an error, display it
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h2>Error inserting record</h2>");
            out.println("<p>" + result + "</p>");
            out.println("<a href='" + request.getContextPath() + "/records'>Back to records</a>");
            out.println("</body></html>");
        } else {
            response.sendRedirect(request.getContextPath() + "/records");
        }
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.util.HTMLGenerator;

@WebServlet("/maintenance")
public class MaintenanceServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.write(HTMLGenerator.generateHeader("Maintenance", request));
        out.write("<h1>Database Maintenance</h1>");
        out.write(HTMLGenerator.generateImportForm(request));
        out.write(HTMLGenerator.generateTruncateForm(request));
        out.write(HTMLGenerator.generateFooter());
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.util.List;
import com.dbmodwebapp.model.Record;
import com.dbmodwebapp.dao.RecordDAO;
import com.dbmodwebapp.util.HTMLGenerator;

@WebServlet("/records")
public class RecordsServlet extends HttpServlet {
    private RecordDAO recordDAO = new RecordDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Record> records = recordDAO.getRecords();
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.write(HTMLGenerator.generateHeader("Database Records", request));
        out.write("<h1>Database Records</h1>");
        out.write(HTMLGenerator.generateRecordsTable(records, request));
        out.write(HTMLGenerator.generateInsertForm(request));
        out.write(HTMLGenerator.generateFooter());
    }
}

package com.dbmodwebapp.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import com.dbmodwebapp.dao.RecordDAO;

@WebServlet("/truncate")
public class TruncateServlet extends HttpServlet {
    private RecordDAO recordDAO = new RecordDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        recordDAO.truncateTable();
        response.sendRedirect(request.getContextPath() + "/records");
    }
}

public class HTMLGenerator {
    public static String generateHeader(String title, HttpServletRequest request) {
        StringBuilder header = new StringBuilder();
        header.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        header.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        header.append("<title>").append(title).append(" - Database Web App</title>");
        header.append(StyleGenerator.generateStyles());
        header.append("<script>");
        header.append("let dropArea;");
        header.append("document.addEventListener('DOMContentLoaded', function() {");
        header.append("  dropArea = document.getElementById('drop-area');");
        header.append("  const fileElem = document.getElementById('fileElem');");
        header.append("  const fileName = document.getElementById('file-name');");
        header.append("  const importBtn = document.getElementById('import-btn');");
        header.append("  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {");
        header.append("    dropArea.addEventListener(eventName, preventDefaults, false);");
        header.append("  });");
        header.append("  ['dragenter', 'dragover'].forEach(eventName => {");
        header.append("    dropArea.addEventListener(eventName, highlight, false);");
        header.append("  });");
        header.append("  ['dragleave', 'drop'].forEach(eventName => {");
        header.append("    dropArea.addEventListener(eventName, unhighlight, false);");
        header.append("  });");
        header.append("  dropArea.addEventListener('drop', handleDrop, false);");
        header.append("  fileElem.addEventListener('change', function(e) { handleFiles(this.files); }, false);");
        header.append("});");
        header.append("function preventDefaults(e) { e.preventDefault(); e.stopPropagation(); }");
        header.append("function highlight(e) { dropArea.classList.add('highlight'); }");
        header.append("function unhighlight(e) { dropArea.classList.remove('highlight'); }");
        header.append("function handleDrop(e) { const dt = e.dataTransfer; const files = dt.files; handleFiles(files); }");
        header.append("function handleFiles(files) { if (files.length > 0) {");
        header.append("  document.getElementById('fileElem').files = files;");
        header.append("  document.getElementById('file-name').textContent = 'Selected file: ' + files[0].name;");
        header.append("  document.getElementById('import-btn').disabled = false;");
        header.append("  document.getElementById('import-btn').style.opacity = '1';");
        header.append("}}");
        header.append("</script>");
        header.append("</head><body>");
        header.append("<nav><ul>");
        header.append("<li><a href='").append(request.getContextPath()).append("'>Home</a></li>");
        header.append("<li><a href='").append(request.getContextPath()).append("/records'>Database Records</a></li>");
        header.append("<li><a href='").append(request.getContextPath()).append("/maintenance'>Maintenance</a></li>");
        header.append("</ul></nav>");
        return header.toString();
    }

    public static String generateFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("<footer>");
        footer.append("<p>&copy; 2023 Database Web App. All rights reserved.</p>");
        footer.append("<p><a href='#'>Privacy Policy</a> | <a href='#'>Terms of Service</a> | <a href='#'>Contact Us</a></p>");
        footer.append("</footer>");
        footer.append("</body></html>");
        return footer.toString();
    }

    public static String generateRecordsTable(List<Record> records, HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<table>");
        html.append("<tr><th>ID</th><th>Name</th><th>Surname</th><th>Action</th></tr>");
        for (Record record : records) {
            html.append("<tr><td>").append(record.getId()).append("</td>");
            html.append("<td>").append(record.getName()).append("</td>");
            html.append("<td>").append(record.getSurname()).append("</td>");
            html.append("<td><form method='post' action='").append(request.getContextPath()).append("/delete' style='display:inline;'>");
            html.append("<input type='hidden' name='id' value='").append(record.getId()).append("'>");
            html.append("<input type='submit' value='Delete' class='delete-btn'></form></td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    public static String generateInsertForm(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>Insert New Record</h2>");
        html.append("<form method='post' action='").append(request.getContextPath()).append("/insert'>");
        html.append("<input type='text' name='name' placeholder='Name' required>");
        html.append("<input type='text' name='surname' placeholder='Surname' required>");
        html.append("<input type='submit' value='Insert' class='action-btn'>");
        html.append("</form>");
        return html.toString();
    }

    public static String generateImportForm(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>Import CSV</h2>");
        html.append("<form id='upload-form' method='post' action='").append(request.getContextPath()).append("/import' enctype='multipart/form-data'>");
        html.append("<div id='drop-area'>");
        html.append("<p>Drag and drop a CSV file here or click to select a file</p>");
        html.append("<input type='file' id='fileElem' name='csvFile' accept='.csv'>");
        html.append("<label class='button' for='fileElem'>Select a file</label>");
        html.append("</div>");
        html.append("<p id='file-name'></p>");
        html.append("<input type='submit' value='Import CSV' class='action-btn' id='import-btn' disabled style='opacity: 0.5;'>");
        html.append("</form>");
        return html.toString();
    }

    public static String generateTruncateForm(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<form method='post' action='").append(request.getContextPath()).append("/truncate' style='margin-top:10px;'>");
        html.append("<input type='submit' value='Truncate Table' class='action-btn'>");
        html.append("</form>");
        return html.toString();
    }

    package com.dbmodwebapp.util;

public class StyleGenerator {
    public static String generateStyles() {
        StringBuilder styles = new StringBuilder();
        styles.append("<style>");
        styles.append("body{font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;font-size:14px;line-height:1.4;color:#333;max-width:900px;margin:0 auto;padding:10px;background-color:#f9f9f9;}");
        styles.append("h1{font-size:24px;margin:20px 0 10px;font-weight:normal;}");
        styles.append("h2{font-size:20px;margin:15px 0 5px;font-weight:normal;}");
        styles.append("table{width:100%;border-collapse:collapse;margin-bottom:10px;background-color:#fff;border:1px solid #ddd;}");
        styles.append("th,td{padding:7px 9px;text-align:left;border-bottom:1px solid #ddd;font-size:16px;line-height:1.4;}");
        styles.append("th{background-color:#f8f8f8;font-weight:bold;}");
        styles.append("tr:hover{background-color:#f5f5f5;}");
        styles.append("form{background-color:#f8f8f8;padding:10px;border-radius:3px;border:1px solid #ddd;}");
        styles.append("input[type='text']{width:calc(50% - 5px);padding:6px;margin:2px 0;border:1px solid #ccc;border-radius:2px;box-sizing:border-box;font-family:inherit;font-size:16px;}");
        styles.append("input[type='submit'], .action-btn{background-color:#0D3C61;color:white;padding:6px 10px;border:none;border-radius:2px;cursor:pointer;font-size:16px;font-family:inherit;margin-right:5px;}");
        styles.append("input[type='submit']:hover, .action-btn:hover{background-color:#0A2F4D;}");
        styles.append("input[type='submit']:disabled, .action-btn:disabled{background-color:#cccccc;color:#666666;cursor:not-allowed;}");
        styles.append(".delete-btn{background-color:#6E7780;color:white;padding:4px 8px;border:none;border-radius:2px;cursor:pointer;font-size:14px;font-family:inherit;}");
        styles.append(".delete-btn:hover{background-color:#5A636B;}");
        styles.append("nav{background-color:#0D3C61;padding:10px 0;}");
        styles.append("nav ul{list-style-type:none;padding:0;margin:0;display:flex;justify-content:center;}");
        styles.append("nav ul li{margin:0 10px;}");
        styles.append("nav ul li a{color:white;text-decoration:none;font-size:16px;}");
        styles.append("nav ul li a:hover{text-decoration:underline;}");
        styles.append("footer{margin-top:20px;padding:10px;background-color:#f8f8f8;border-top:1px solid #ddd;text-align:center;font-size:12px;}");
        styles.append("#drop-area{border:2px dashed #ccc;border-radius:20px;width:480px;font-family:sans-serif;margin:20px auto;padding:20px;}");
        styles.append("#drop-area.highlight{border-color:purple;}");
        styles.append("p{margin-top:0;}");
        styles.append(".button{display:inline-block;padding:10px;background:#ccc;cursor:pointer;border-radius:5px;border:1px solid #ccc;}");
        styles.append(".button:hover{background:#ddd;}");
        styles.append("#fileElem{display:none;}");
        styles.append("</style>");
        return styles.toString();
    }
}

<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
         version="5.0">

    <display-name>DB Mod Web App</display-name>

    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>

</web-app>

