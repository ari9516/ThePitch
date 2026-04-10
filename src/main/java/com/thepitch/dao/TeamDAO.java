package com.thepitch.dao;

import com.thepitch.model.Team;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamDAO {
    private DatabaseConnection db;
    
    public TeamDAO() {
        this.db = DatabaseConnection.getInstance();
    }
    
    // ========== SAVE METHODS ==========
    
    public void saveTeam(Team team) {
        String sql = "INSERT OR REPLACE INTO teams (team_id, team_name, league_id, elo_rating, last_updated) VALUES (?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, team.getTeamId());
            pstmt.setString(2, team.getTeamName());
            pstmt.setInt(3, team.getLeagueId());
            pstmt.setInt(4, team.getEloRating());
            pstmt.setString(5, team.getLastUpdated());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving team " + team.getTeamName() + ": " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    public void saveTeams(List<Team> teams) {
        for (Team team : teams) {
            saveTeam(team);
        }
    }
    
    // ========== GET METHODS ==========
    
    public Team getTeamById(int teamId) {
        String sql = "SELECT * FROM teams WHERE team_id = ?";
        Team team = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                team = buildTeamFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting team by ID: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return team;
    }
    
    public Team getTeamByName(String teamName) {
        String sql = "SELECT * FROM teams WHERE team_name = ?";
        Team team = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teamName);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                team = buildTeamFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting team by name: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return team;
    }
    
    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY elo_rating DESC";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                teams.add(buildTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all teams: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return teams;
    }
    
    // Safe version with proper resource management
    public List<Team> getAllTeamsSafe() {
        return getAllTeams();
    }
    
    public List<Team> getTeamsByLeague(int leagueId) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE league_id = ? ORDER BY elo_rating DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, leagueId);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                teams.add(buildTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting teams by league: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return teams;
    }
    
    public List<Team> getTopRatedTeams(int limit) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY elo_rating DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                teams.add(buildTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting top rated teams: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        return teams;
    }
    
    // ========== UPDATE METHODS ==========
    
    public void updateEloRating(int teamId, int newRating) {
        String sql = "UPDATE teams SET elo_rating = ?, last_updated = datetime('now') WHERE team_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, newRating);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating ELO rating: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    public void updateTeamName(int teamId, String newName) {
        String sql = "UPDATE teams SET team_name = ? WHERE team_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newName);
            pstmt.setInt(2, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating team name: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    // ========== DELETE METHODS ==========
    
    public void deleteTeam(int teamId) {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting team: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    public void deleteAllTeams() {
        String sql = "DELETE FROM teams";
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            int deleted = stmt.executeUpdate(sql);
            System.out.println("Deleted " + deleted + " teams");
        } catch (SQLException e) {
            System.err.println("Error deleting all teams: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
    
    // ========== COUNT METHODS ==========
    
    public int getTeamCount() {
        String sql = "SELECT COUNT(*) FROM teams";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting team count: " + e.getMessage());
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
    
    public int getTeamCountByLeague(int leagueId) {
        String sql = "SELECT COUNT(*) FROM teams WHERE league_id = ?";
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
            System.err.println("Error getting team count by league: " + e.getMessage());
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
    
    // ========== CHECK METHODS ==========
    
    public boolean teamExists(int teamId) {
        String sql = "SELECT 1 FROM teams WHERE team_id = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teamId);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking team existence: " + e.getMessage());
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    public boolean hasTeams() {
        return getTeamCount() > 0;
    }
    
    // ========== HELPER METHODS ==========
    
    private Team buildTeamFromResultSet(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setTeamId(rs.getInt("team_id"));
        team.setTeamName(rs.getString("team_name"));
        team.setLeagueId(rs.getInt("league_id"));
        team.setEloRating(rs.getInt("elo_rating"));
        team.setLastUpdated(rs.getString("last_updated"));
        return team;
    }
    
    // ========== BULK OPERATIONS ==========
    
    public void updateEloRatingsBulk(java.util.Map<Integer, Integer> ratingUpdates) {
        String sql = "UPDATE teams SET elo_rating = ?, last_updated = datetime('now') WHERE team_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = db.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            for (java.util.Map.Entry<Integer, Integer> entry : ratingUpdates.entrySet()) {
                pstmt.setInt(1, entry.getValue());
                pstmt.setInt(2, entry.getKey());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error bulk updating ELO ratings: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }
}