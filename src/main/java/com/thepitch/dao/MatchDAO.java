package com.thepitch.dao;

import com.thepitch.model.Match;
import com.thepitch.model.Team;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MatchDAO {
    private DatabaseConnection db;
    private TeamDAO teamDAO;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat sqlDateFormat;
    
    public MatchDAO() {
        this.db = DatabaseConnection.getInstance();
        this.teamDAO = new TeamDAO();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }
    
    // ========== SAVE METHODS ==========
    
    public void saveMatch(Match match) {
        String sql = "INSERT OR REPLACE INTO matches (match_id, match_date, home_team_id, away_team_id, league_id, home_score, away_score, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, match.getMatchId());
            pstmt.setString(2, dateFormat.format(match.getMatchDate()));
            pstmt.setInt(3, match.getHomeTeam().getTeamId());
            pstmt.setInt(4, match.getAwayTeam().getTeamId());
            pstmt.setInt(5, match.getLeagueId());
            
            if (match.getHomeScore() != null) {
                pstmt.setInt(6, match.getHomeScore());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            
            if (match.getAwayScore() != null) {
                pstmt.setInt(7, match.getAwayScore());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            
            pstmt.setString(8, match.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving match: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    // ========== GET METHODS ==========
    
    public Match getMatchById(int matchId) {
        String sql = "SELECT * FROM matches WHERE match_id = ?";
        Match match = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, matchId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                match = buildMatchFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting match by ID: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return match;
    }
    
    // SAFE VERSION - Properly handles resource closing
    public List<Match> getAllMatchesSafe() {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches ORDER BY match_date";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all matches: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                // DON'T close connection here - leave it for the pool
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    // Original getAllMatches - kept for compatibility but uses safe version internally
    public List<Match> getAllMatches() {
        return getAllMatchesSafe();
    }
    
    public List<Match> getMatchesByLeague(int leagueId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE league_id = ? ORDER BY match_date DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, leagueId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by league: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getMatchesByTeam(int teamId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE home_team_id = ? OR away_team_id = ? ORDER BY match_date DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            pstmt.setInt(2, teamId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by team: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    // ========== DATE-BASED QUERY METHODS ==========
    
    public List<Match> getTodayMatches() {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE date(match_date) = date('now') ORDER BY match_date";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting today's matches: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getMatchesByDate(Date date) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE date(match_date) = date(?) ORDER BY match_date";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, sqlDateFormat.format(date));
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by date: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getMatchesForNextDays(int days) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE date(match_date) BETWEEN date('now') AND date('now', ?) ORDER BY match_date";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "+" + days + " days");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches for next days: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getMatchesByDateRange(String startDate, String endDate) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE date(match_date) BETWEEN ? AND ? ORDER BY match_date";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by date range: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    // ========== STATUS-BASED QUERY METHODS ==========
    
    public List<Match> getUpcomingMatches() {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE date(match_date) >= date('now') AND status = 'SCHEDULED' ORDER BY match_date LIMIT 20";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting upcoming matches: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getUpcomingMatchesByLeague(int leagueId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE league_id = ? AND date(match_date) >= date('now') AND status = 'SCHEDULED' ORDER BY match_date";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, leagueId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting upcoming matches by league: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getRecentMatches(int limit) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE status = 'FINISHED' ORDER BY match_date DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent matches: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    public List<Match> getFinishedMatchesByLeague(int leagueId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE league_id = ? AND status = 'FINISHED' ORDER BY match_date DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, leagueId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Match match = buildMatchFromResultSet(rs);
                if (match != null) {
                    matches.add(match);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting finished matches: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return matches;
    }
    
    // ========== UPDATE METHODS ==========
    
    public void updateMatchResult(int matchId, int homeScore, int awayScore, String status) {
        String sql = "UPDATE matches SET home_score = ?, away_score = ?, status = ? WHERE match_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, homeScore);
            pstmt.setInt(2, awayScore);
            pstmt.setString(3, status);
            pstmt.setInt(4, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating match result: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    public void updateMatchStatus(int matchId, String status) {
        String sql = "UPDATE matches SET status = ? WHERE match_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, status);
            pstmt.setInt(2, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating match status: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    // ========== COUNT METHODS ==========
    
    public int getMatchCount() {
        String sql = "SELECT COUNT(*) FROM matches";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting match count: " + e.getMessage());
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    public int getMatchCountByLeague(int leagueId) {
        String sql = "SELECT COUNT(*) FROM matches WHERE league_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, leagueId);
            rs = pstmt.executeQuery();
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting match count by league: " + e.getMessage());
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    public int getUpcomingMatchCount() {
        String sql = "SELECT COUNT(*) FROM matches WHERE date(match_date) >= date('now') AND status = 'SCHEDULED'";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting upcoming match count: " + e.getMessage());
            return 0;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private Match buildMatchFromResultSet(ResultSet rs) throws SQLException {
        int homeTeamId = rs.getInt("home_team_id");
        int awayTeamId = rs.getInt("away_team_id");
        
        Team homeTeam = teamDAO.getTeamById(homeTeamId);
        Team awayTeam = teamDAO.getTeamById(awayTeamId);
        
        if (homeTeam == null || awayTeam == null) {
            return null;
        }
        
        Match match = new Match();
        match.setMatchId(rs.getInt("match_id"));
        match.setMatchDate(rs.getTimestamp("match_date"));
        match.setLeagueId(rs.getInt("league_id"));
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        
        int homeScore = rs.getInt("home_score");
        int awayScore = rs.getInt("away_score");
        match.setHomeScore(rs.wasNull() ? null : homeScore);
        match.setAwayScore(rs.wasNull() ? null : awayScore);
        match.setStatus(rs.getString("status"));
        
        return match;
    }
    
    public boolean hasMatches() {
        return getMatchCount() > 0;
    }
    
    public void deleteOldMatches(int daysOld) {
        String sql = "DELETE FROM matches WHERE date(match_date) < date('now', ?) AND status = 'FINISHED'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "-" + daysOld + " days");
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("Deleted " + deleted + " old matches");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting old matches: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
}
