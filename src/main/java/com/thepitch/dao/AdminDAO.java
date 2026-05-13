package com.thepitch.dao;

import java.sql.*;
import java.util.*;

/**
 * AdminDAO — handles all admin-managed data in SQLite:
 *   1. team_xg table          — manual xGF/xGA per team per competition/season
 *   2. injury_list table      — persistent injury list per team
 *   3. tactical_analysis table — AI + admin tactical scores per fixture
 */
public class AdminDAO {

    private final Connection conn;

    public AdminDAO() throws SQLException {
        this.conn = DatabaseConnection.getInstance().getConnection();
        ensureTables();
    }

    // ── Table setup ───────────────────────────────────────────────────────────
    private void ensureTables() throws SQLException {
        // xG table
        conn.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS team_xg (" +
            "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  team_name   TEXT NOT NULL," +
            "  competition TEXT NOT NULL DEFAULT 'Premier League'," +
            "  season      TEXT NOT NULL DEFAULT '2024-25'," +
            "  xgf         REAL NOT NULL DEFAULT 0.0," +
            "  xga         REAL NOT NULL DEFAULT 0.0," +
            "  matches     INTEGER DEFAULT 0," +
            "  updated_at  TEXT DEFAULT (datetime('now'))," +
            "  UNIQUE(team_name, competition, season)" +
            ")"
        );

        // Injury table
        conn.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS injury_list (" +
            "  id                   INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  team_name            TEXT NOT NULL," +
            "  player_name          TEXT NOT NULL," +
            "  tier                 TEXT NOT NULL DEFAULT 'regular'," +
            "  position_criticality REAL NOT NULL DEFAULT 1.0," +
            "  position_label       TEXT DEFAULT ''," +
            "  added_date           TEXT DEFAULT (date('now'))," +
            "  notes                TEXT DEFAULT ''," +
            "  UNIQUE(team_name, player_name)" +
            ")"
        );

        // Tactical analysis table
        ensureTacticalTable();
    }

    // ════════════════════════════════════════════════════════════════════════
    // xG METHODS
    // ════════════════════════════════════════════════════════════════════════

    public void saveTeamXG(String teamName, String competition, String season,
                            double xgf, double xga, int matches) throws SQLException {
        String sql =
            "INSERT INTO team_xg (team_name, competition, season, xgf, xga, matches, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, datetime('now')) " +
            "ON CONFLICT(team_name, competition, season) DO UPDATE SET " +
            "  xgf=excluded.xgf, xga=excluded.xga, " +
            "  matches=excluded.matches, updated_at=datetime('now')";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, teamName); ps.setString(2, competition); ps.setString(3, season);
        ps.setDouble(4, xgf);     ps.setDouble(5, xga);         ps.setInt(6, matches);
        ps.executeUpdate();
    }

    /** Returns double[]{xgf, xga} or null if not set. */
    public double[] getTeamXG(String teamName, String competition, String season) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT xgf, xga FROM team_xg " +
            "WHERE LOWER(team_name) LIKE LOWER(?) AND competition=? AND season=?"
        );
        ps.setString(1, "%" + teamName + "%");
        ps.setString(2, competition);
        ps.setString(3, season);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return new double[]{rs.getDouble("xgf"), rs.getDouble("xga")};
        return null;
    }

    public List<Map<String, Object>> getAllXGEntries() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT * FROM team_xg ORDER BY team_name, competition, season"
        );
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("team_name",   rs.getString("team_name"));
            row.put("competition", rs.getString("competition"));
            row.put("season",      rs.getString("season"));
            row.put("xgf",         rs.getDouble("xgf"));
            row.put("xga",         rs.getDouble("xga"));
            row.put("matches",     rs.getInt("matches"));
            row.put("updated_at",  rs.getString("updated_at"));
            result.add(row);
        }
        return result;
    }

    public void deleteTeamXG(String teamName, String competition, String season) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM team_xg WHERE team_name=? AND competition=? AND season=?"
        );
        ps.setString(1, teamName); ps.setString(2, competition); ps.setString(3, season);
        ps.executeUpdate();
    }

    // ════════════════════════════════════════════════════════════════════════
    // INJURY LIST METHODS
    // ════════════════════════════════════════════════════════════════════════

    public void addInjury(String teamName, String playerName, String tier,
                          double criticality, String positionLabel, String notes) throws SQLException {
        String sql =
            "INSERT INTO injury_list " +
            "  (team_name, player_name, tier, position_criticality, position_label, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(team_name, player_name) DO UPDATE SET " +
            "  tier=excluded.tier, position_criticality=excluded.position_criticality," +
            "  position_label=excluded.position_label, notes=excluded.notes, added_date=date('now')";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, teamName);   ps.setString(2, playerName);
        ps.setString(3, tier);       ps.setDouble(4, criticality);
        ps.setString(5, positionLabel); ps.setString(6, notes);
        ps.executeUpdate();
    }

    public void removeInjury(String teamName, String playerName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM injury_list " +
            "WHERE LOWER(team_name) LIKE LOWER(?) AND LOWER(player_name) LIKE LOWER(?)"
        );
        ps.setString(1, "%" + teamName + "%");
        ps.setString(2, "%" + playerName + "%");
        ps.executeUpdate();
    }

    public List<Map<String, Object>> getTeamInjuries(String teamName) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM injury_list WHERE LOWER(team_name) LIKE LOWER(?) " +
            "ORDER BY tier DESC, player_name"
        );
        ps.setString(1, "%" + teamName + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("player_name",          rs.getString("player_name"));
            row.put("tier",                 rs.getString("tier"));
            row.put("position_criticality", rs.getDouble("position_criticality"));
            row.put("position_label",       rs.getString("position_label"));
            row.put("notes",                rs.getString("notes"));
            row.put("added_date",           rs.getString("added_date"));
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getAllInjuries() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT * FROM injury_list ORDER BY team_name, tier DESC, player_name"
        );
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("team_name",            rs.getString("team_name"));
            row.put("player_name",          rs.getString("player_name"));
            row.put("tier",                 rs.getString("tier"));
            row.put("position_criticality", rs.getDouble("position_criticality"));
            row.put("position_label",       rs.getString("position_label"));
            row.put("added_date",           rs.getString("added_date"));
            result.add(row);
        }
        return result;
    }

    public void clearTeamInjuries(String teamName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM injury_list WHERE LOWER(team_name) LIKE LOWER(?)"
        );
        ps.setString(1, "%" + teamName + "%");
        ps.executeUpdate();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TACTICAL ANALYSIS METHODS
    // ════════════════════════════════════════════════════════════════════════

    /** Called by TacticalAnalyzer on startup. */
    public void ensureTacticalTable() throws SQLException {
        conn.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS tactical_analysis (" +
            "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  home_team  TEXT NOT NULL," +
            "  away_team  TEXT NOT NULL," +
            "  home_score REAL NOT NULL DEFAULT 5.0," +
            "  away_score REAL NOT NULL DEFAULT 5.0," +
            "  admin_note TEXT DEFAULT ''," +
            "  created_at TEXT DEFAULT (datetime('now'))," +
            "  UNIQUE(home_team, away_team)" +
            ")"
        );
    }

    /** Save or update tactical scores for a fixture. */
    public void saveTacticalAnalysis(String homeTeam, String awayTeam,
                                      double homeScore, double awayScore,
                                      String adminNote) throws SQLException {
        String sql =
            "INSERT INTO tactical_analysis (home_team, away_team, home_score, away_score, admin_note, created_at) " +
            "VALUES (?, ?, ?, ?, ?, datetime('now')) " +
            "ON CONFLICT(home_team, away_team) DO UPDATE SET " +
            "  home_score=excluded.home_score, away_score=excluded.away_score," +
            "  admin_note=excluded.admin_note, created_at=datetime('now')";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, homeTeam);  ps.setString(2, awayTeam);
        ps.setDouble(3, homeScore); ps.setDouble(4, awayScore);
        ps.setString(5, adminNote != null ? adminNote : "");
        ps.executeUpdate();
    }

    /** Returns double[]{homeScore, awayScore} or null if not saved. */
    public double[] getTacticalScores(String homeTeam, String awayTeam) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT home_score, away_score FROM tactical_analysis " +
            "WHERE LOWER(home_team) LIKE LOWER(?) AND LOWER(away_team) LIKE LOWER(?)"
        );
        ps.setString(1, "%" + homeTeam + "%");
        ps.setString(2, "%" + awayTeam + "%");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new double[]{rs.getDouble("home_score"), rs.getDouble("away_score")};
        }
        return null;
    }
}
