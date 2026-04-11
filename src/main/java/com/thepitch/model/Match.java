package com.thepitch.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Match JavaBean class representing a football match.
 * Contains information about teams, date, score, and status.
 * 
 * @author ThePitch Team
 * @version 1.1
 */
public class Match implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a");
    
    // Core properties
    private int matchId;           // Unique match identifier (from API)
    private Date matchDate;        // Date and time of the match
    private Team homeTeam;         // Home team object
    private Team awayTeam;         // Away team object
    private int leagueId;          // League this match belongs to
    
    // Score properties
    private Integer homeScore;     // Final home team score (null if not played)
    private Integer awayScore;     // Final away team score (null if not played)
    
    // Status properties
    private String status;         // SCHEDULED, LIVE, FINISHED, CANCELLED, POSTPONED
    private String createdAt;      // When this record was created
    
    /**
     * Default constructor
     */
    public Match() {
        this.status = "SCHEDULED";
    }
    
    /**
     * Parameterized constructor for creating a match
     * 
     * @param matchId    Unique match ID from API
     * @param matchDate  Date and time of the match
     * @param homeTeam   Home team
     * @param awayTeam   Away team
     * @param leagueId   League ID
     */
    public Match(int matchId, Date matchDate, Team homeTeam, Team awayTeam, int leagueId) {
        this();
        this.matchId = matchId;
        this.matchDate = matchDate;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.leagueId = leagueId;
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public Date getMatchDate() {
        return matchDate;
    }
    
    public void setMatchDate(Date matchDate) {
        this.matchDate = matchDate;
    }
    
    public Team getHomeTeam() {
        return homeTeam;
    }
    
    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }
    
    public Team getAwayTeam() {
        return awayTeam;
    }
    
    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }
    
    public int getLeagueId() {
        return leagueId;
    }
    
    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }
    
    public Integer getHomeScore() {
        return homeScore;
    }
    
    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }
    
    public Integer getAwayScore() {
        return awayScore;
    }
    
    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    // ========== BUSINESS LOGIC METHODS ==========
    
    /**
     * Checks if the match has been played
     * 
     * @return true if status is FINISHED
     */
    public boolean isFinished() {
        return "FINISHED".equals(status);
    }
    
    /**
     * Checks if the match is currently being played
     * 
     * @return true if status is LIVE
     */
    public boolean isLive() {
        return "LIVE".equals(status);
    }
    
    /**
     * Checks if the match is scheduled for the future
     * 
     * @return true if status is SCHEDULED
     */
    public boolean isScheduled() {
        return "SCHEDULED".equals(status);
    }
    
    /**
     * Gets the match result as a string
     * 
     * @return "HOME_WIN", "AWAY_WIN", "DRAW", or "PENDING"
     */
    public String getResult() {
        if (homeScore == null || awayScore == null) {
            return "PENDING";
        }
        if (homeScore > awayScore) {
            return "HOME_WIN";
        } else if (awayScore > homeScore) {
            return "AWAY_WIN";
        } else {
            return "DRAW";
        }
    }
    
    /**
     * Gets the winner of the match
     * 
     * @return The winning team, or null if draw/not played
     */
    public Team getWinner() {
        String result = getResult();
        if (result.equals("HOME_WIN")) {
            return homeTeam;
        } else if (result.equals("AWAY_WIN")) {
            return awayTeam;
        }
        return null;
    }
    
    /**
     * Gets formatted match date string
     * 
     * @return Date in format "dd/MM/yyyy HH:mm"
     */
    public String getFormattedDate() {
        if (matchDate == null) {
            return "TBD";
        }
        return DATE_FORMAT.format(matchDate);
    }
    
    /**
     * Gets formatted time only
     * 
     * @return Time in format "hh:mm a"
     */
    public String getFormattedTime() {
        if (matchDate == null) {
            return "TBD";
        }
        return TIME_FORMAT.format(matchDate);
    }
    
    /**
     * Gets formatted score string
     * 
     * @return String like "2-1" or "vs" if not played
     */
    public String getFormattedScore() {
        if (homeScore != null && awayScore != null) {
            return homeScore + " - " + awayScore;
        }
        return "vs";
    }
    
    /**
     * Gets formatted display string for match listing
     * Includes score for finished matches
     * 
     * @return Formatted match string with score if available
     */
    public String getDisplayString() {
        if (isFinished() && homeScore != null && awayScore != null) {
            return String.format("%s %d - %d %s", 
                homeTeam.getTeamName(), homeScore, awayScore, awayTeam.getTeamName());
        } else {
            return String.format("%s vs %s", homeTeam.getTeamName(), awayTeam.getTeamName());
        }
    }
    
    /**
     * Gets a status icon emoji for display
     * 
     * @return Emoji string representing match status
     */
    public String getStatusEmoji() {
        switch (status) {
            case "FINISHED":
                return "✅";
            case "LIVE":
                return "🟢 LIVE";
            case "SCHEDULED":
                return "⏰";
            case "POSTPONED":
                return "📅";
            case "CANCELLED":
                return "❌";
            default:
                return "❓";
        }
    }
    
    /**
     * Gets detailed status with score for finished matches
     */
    public String getDetailedStatus() {
        if (isFinished() && homeScore != null && awayScore != null) {
            return "✅ " + homeScore + "-" + awayScore;
        } else if (isLive()) {
            return "🟢 LIVE";
        } else if (isScheduled()) {
            return "⏰ " + getFormattedTime();
        }
        return getStatusEmoji();
    }
    
    /**
     * Calculates total goals in the match
     * 
     * @return Total goals, or 0 if match not played
     */
    public int getTotalGoals() {
        if (homeScore != null && awayScore != null) {
            return homeScore + awayScore;
        }
        return 0;
    }
    
    /**
     * Checks if both teams scored
     * 
     * @return true if both teams scored at least one goal
     */
    public boolean isBothTeamsScored() {
        if (homeScore != null && awayScore != null) {
            return homeScore > 0 && awayScore > 0;
        }
        return false;
    }
    
    /**
     * Gets a one-line summary for console display
     */
    public String getSummary() {
        if (isFinished() && homeScore != null && awayScore != null) {
            return String.format("[%s] %s %d-%d %s", 
                getFormattedDate(), 
                homeTeam.getTeamName(), 
                homeScore, 
                awayScore, 
                awayTeam.getTeamName());
        } else {
            return String.format("[%s] %s vs %s (%s)", 
                getFormattedDate(), 
                homeTeam.getTeamName(), 
                awayTeam.getTeamName(),
                getStatusEmoji());
        }
    }
    
    @Override
    public String toString() {
        if (isFinished() && homeScore != null && awayScore != null) {
            return String.format("%s %d - %d %s | %s", 
                homeTeam.getTeamName(), homeScore, awayScore, 
                awayTeam.getTeamName(), getFormattedDate());
        } else {
            return String.format("%s vs %s | %s | %s", 
                homeTeam.getTeamName(), 
                awayTeam.getTeamName(), 
                getFormattedDate(),
                getStatusEmoji());
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Match match = (Match) obj;
        return matchId == match.matchId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(matchId);
    }
}
