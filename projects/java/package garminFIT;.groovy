package garminFIT;


import com.garmin.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FitFileProcessor {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/fbreschi";
    private static final String DB_USER = "postgres;
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        // Select the .fit file
        String fitFilePath = selectFitFile();
        if (fitFilePath == null) {
            System.out.println("No file selected. Exiting.");
            return;
        }

        try {
            // Decode the .fit file
            decodeFitFile(fitFilePath);
        } catch (IOException e) {
            System.err.println("Error reading the .fit file: " + e.getMessage());
        }
    }

    private static String selectFitFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("FIT files", "fit");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private static void decodeFitFile(String fitFilePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(fitFilePath)) {
            FitDecoder fitDecoder = new FitDecoder();
            MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(fitDecoder);

            // Add listeners for the message types you're interested in
            mesgBroadcaster.addListener((RecordMesgListener) recordMesg -> processRecordMessage(recordMesg));
            mesgBroadcaster.addListener((SessionMesgListener) sessionMesg -> processSessionMessage(sessionMesg));

            mesgBroadcaster.run(fis);
        }
    }

    private static void processRecordMessage(RecordMesg recordMesg) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO record_data (timestamp, position_lat, position_long, altitude, heart_rate, cadence, speed) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, new Timestamp(recordMesg.getTimestamp().getDate().getTime()));
                pstmt.setDouble(2, recordMesg.getPositionLat() != null ? recordMesg.getPositionLat() : 0);
                pstmt.setDouble(3, recordMesg.getPositionLong() != null ? recordMesg.getPositionLong() : 0);
                pstmt.setFloat(4, recordMesg.getAltitude() != null ? recordMesg.getAltitude() : 0);
                pstmt.setShort(5, recordMesg.getHeartRate() != null ? recordMesg.getHeartRate() : 0);
                pstmt.setShort(6, recordMesg.getCadence() != null ? recordMesg.getCadence() : 0);
                pstmt.setFloat(7, recordMesg.getSpeed() != null ? recordMesg.getSpeed() : 0);

                pstmt.executeUpdate();ive me all the code again
            }
        } catch (SQLException e) {
            System.err.println("Error inserting record data: " + e.getMessage());
        }
    }

    private static void processSessionMessage(SessionMesg sessionMesg) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO session_data (start_time, total_elapsed_time, total_distance, avg_speed, max_speed, avg_heart_rate, max_heart_rate) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, new Timestamp(sessionMesg.getStartTime().getDate().getTime()));
                pstmt.setFloat(2, sessionMesg.getTotalElapsedTime() != null ? sessionMesg.getTotalElapsedTime() : 0);
                pstmt.setFloat(3, sessionMesg.getTotalDistance() != null ? sessionMesg.getTotalDistance() : 0);
                pstmt.setFloat(4, sessionMesg.getAvgSpeed() != null ? sessionMesg.getAvgSpeed() : 0);
                pstmt.setFloat(5, sessionMesg.getMaxSpeed() != null ? sessionMesg.getMaxSpeed() : 0);
                pstmt.setShort(6, sessionMesg.getAvgHeartRate() != null ? sessionMesg.getAvgHeartRate() : 0);
                pstmt.setShort(7, sessionMesg.getMaxHeartRate() != null ? sessionMesg.getMaxHeartRate() : 0);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error inserting session data: " + e.getMessage());
        }
    }
}