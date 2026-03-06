package com.writersassist.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    private static final String URL = "jdbc:sqlite:writersassist.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement()) {

            // Create table for dashboard stats
            String createTableScript = "CREATE TABLE IF NOT EXISTS project_stats (" +
                    "id INTEGER PRIMARY KEY, " +
                    "total_scripts INTEGER, " +
                    "word_count INTEGER, " +
                    "collaborators INTEGER)";
            stmt.execute(createTableScript);

            // Insert initial data if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM project_stats");
            if (rs.next() && rs.getInt(1) == 0) {
                String insertData = "INSERT INTO project_stats (id, total_scripts, word_count, collaborators) " +
                        "VALUES (1, 14, 122405, 4)";
                stmt.execute(insertData);
            }
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    public static int[] getDashboardStats() {
        int[] stats = { 0, 0, 0 };
        try (Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT total_scripts, word_count, collaborators FROM project_stats WHERE id = 1")) {

            if (rs.next()) {
                stats[0] = rs.getInt("total_scripts");
                stats[1] = rs.getInt("word_count");
                stats[2] = rs.getInt("collaborators");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching dashboard stats: " + e.getMessage());
        }
        return stats;
    }
}
