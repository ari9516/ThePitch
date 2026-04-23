package com.thepitch.service;

import com.thepitch.dao.DatabaseConnection;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Imports FBref match stats CSVs into the SQLite database.
 * Compatible with Java 8+ (no text blocks used).
 *
 * Tables populated:
 *   match_stats  – per-match per-team: shots, xG, cards, fouls, corners
 */
public class CSVImporter {

    private final Connection conn;

    // ── Column name candidates (FBref sometimes renames columns) ─────────────
    private static final String[] SHOTS_COLS   = {"Sh", "Sh_shooting",  "shots"};
    private static final String[] SOT_COLS     = {"SoT","SoT_shooting", "shots_on_target"};
    private static final String[] XG_COLS      = {"xG", "xG_shooting",  "expected_goals"};
    private static final String[] YELLOW_COLS  = {"CrdY","CrdY_misc",   "yellow_cards"};
    private static final String[] RED_COLS     = {"CrdR","CrdR_misc",   "red_cards"};
    private static final String[] FOULS_COLS   = {"Fls", "Fls_misc",    "fouls"};
    private static final String[] CORNERS_COLS = {"Crs", "Crs_misc",    "corners"};
    private static final String[] TEAM_COLS    = {"team","home_team",   "away_team"};
    private static final String[] DATE_COLS    = {"date","Date"};
    private static final String[] GAME_COLS    = {"game","match_id",    "game_id"};

    // ── Constructor ───────────────────────────────────────────────────────────
    public CSVImporter() throws SQLException {
        // Use getInstance() pattern — adjust if your DatabaseConnection works differently
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public void importAll(String dataDir) {
        try {
            ensureMatchStatsTable();

            File statsFile    = new File(dataDir, "pl_match_stats.csv");
            File scheduleFile = new File(dataDir, "pl_schedule.csv");

            if (statsFile.exists()) {
                System.out.println("📥 Importing match stats: " + statsFile.getPath());
                int rows = importMatchStats(statsFile);
                System.out.println("   ✅ Imported " + rows + " team-match rows");
            } else {
                System.out.println("   ⚠️  pl_match_stats.csv not found at: " + statsFile.getPath());
                System.out.println("       Run scripts/fetch_fbref_stats.py first.");
            }

            if (scheduleFile.exists()) {
                System.out.println("📥 Importing schedule: " + scheduleFile.getPath());
                int rows = importSchedule(scheduleFile);
                System.out.println("   ✅ Processed " + rows + " fixture rows");
            }

        } catch (Exception e) {
            System.err.println("❌ CSV Import failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Create match_stats table ──────────────────────────────────────────────
    private void ensureMatchStatsTable() throws SQLException {
        String sql =
            "CREATE TABLE IF NOT EXISTS match_stats (" +
            "    id           INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    game_ref     TEXT," +
            "    team_name    TEXT," +
            "    match_date   TEXT," +
            "    shots        REAL    DEFAULT 0," +
            "    shots_ot     REAL    DEFAULT 0," +
            "    xg           REAL    DEFAULT 0," +
            "    yellow_cards INTEGER DEFAULT 0," +
            "    red_cards    INTEGER DEFAULT 0," +
            "    fouls        INTEGER DEFAULT 0," +
            "    corners      INTEGER DEFAULT 0," +
            "    UNIQUE(game_ref, team_name)" +
            ")";

        conn.createStatement().execute(sql);
        System.out.println("   ✅ match_stats table ready");
    }

    // ── Import pl_match_stats.csv ─────────────────────────────────────────────
    private int importMatchStats(File file) throws IOException, CsvValidationException, SQLException {
        int count = 0;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] header = reader.readNext();
            if (header == null) return 0;

            // Build column index map
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIdx.put(header[i].trim(), i);
            }

            // Print what columns we found (helpful for debugging)
            System.out.println("   📋 Columns found in CSV: " + colIdx.keySet());

            String upsertSQL =
                "INSERT OR REPLACE INTO match_stats " +
                "    (game_ref, team_name, match_date, shots, shots_ot, xg, yellow_cards, red_cards, fouls, corners) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(upsertSQL);
            conn.setAutoCommit(false);

            String[] row;
            while ((row = reader.readNext()) != null) {
                String gameRef   = getCol(row, colIdx, GAME_COLS,    "unknown");
                String teamName  = getCol(row, colIdx, TEAM_COLS,    "unknown");
                String matchDate = getCol(row, colIdx, DATE_COLS,    "");
                double shots     = getDouble(row, colIdx, SHOTS_COLS);
                double shotsOt   = getDouble(row, colIdx, SOT_COLS);
                double xg        = getDouble(row, colIdx, XG_COLS);
                int    yellows   = (int) getDouble(row, colIdx, YELLOW_COLS);
                int    reds      = (int) getDouble(row, colIdx, RED_COLS);
                int    fouls     = (int) getDouble(row, colIdx, FOULS_COLS);
                int    corners   = (int) getDouble(row, colIdx, CORNERS_COLS);

                // Skip rows with no useful identity
                if (teamName.equals("unknown") || gameRef.equals("unknown")) continue;

                ps.setString(1, gameRef);
                ps.setString(2, teamName);
                ps.setString(3, matchDate);
                ps.setDouble(4, shots);
                ps.setDouble(5, shotsOt);
                ps.setDouble(6, xg);
                ps.setInt(7, yellows);
                ps.setInt(8, reds);
                ps.setInt(9, fouls);
                ps.setInt(10, corners);
                ps.addBatch();
                count++;

                if (count % 100 == 0) {
                    ps.executeBatch();
                    conn.commit();
                    System.out.println("   ... " + count + " rows processed");
                }
            }

            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }

        return count;
    }

    // ── Import pl_schedule.csv (count rows for now) ───────────────────────────
    private int importSchedule(File file) throws IOException, CsvValidationException {
        int count = 0;
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] header = reader.readNext();
            if (header == null) return 0;
            while (reader.readNext() != null) count++;
        }
        return count;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the first non-blank value found from any of the candidate column names. */
    private String getCol(String[] row, Map<String, Integer> idx,
                           String[] candidates, String fallback) {
        for (String c : candidates) {
            Integer i = idx.get(c);
            if (i != null && i < row.length && !row[i].isBlank()) {
                return row[i].trim();
            }
        }
        return fallback;
    }

    /** Returns a double from the first matching candidate column, or 0.0 if none found. */
    private double getDouble(String[] row, Map<String, Integer> idx, String[] candidates) {
        for (String c : candidates) {
            Integer i = idx.get(c);
            if (i != null && i < row.length && !row[i].isBlank()) {
                try {
                    return Double.parseDouble(row[i].trim());
                } catch (NumberFormatException ignored) {
                    // try next candidate
                }
            }
        }
        return 0.0;
    }
}
