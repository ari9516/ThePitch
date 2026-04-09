package com.thepitch.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Team JavaBean class representing a football team.
 * Implements Serializable for potential file storage.
 * 
 * @author ThePitch Team
 * @version 1.0
 */
public class Team implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Core properties
    private int teamId;           // Unique identifier (from API)
    private String teamName;      // Team name (e.g., "Arsenal FC")
    private int leagueId;         // Which league the team belongs to
    private int eloRating;        // Current ELO rating (1500 = average)
    private String lastUpdated;   // Timestamp of last rating update
    
    // Performance metrics
    private int[] last5Results;   // Last 5 match results (3=Win, 1=Draw, 0=Loss)
    private int goalsScored;      // Total goals scored in last 5 matches
    private int goalsConceded;    // Total goals conceded in last 5 matches
    
    /**
     * Default constructor - initializes default values
     */
    public Team() {
        this.eloRating = 1500;           // Starting ELO for all teams
        this.last5Results = new int[5];   // Array of 5 results (initially all 0)
        Arrays.fill(this.last5Results, 0); // Fill with losses initially
        this.goalsScored = 0;
        this.goalsConceded = 0;
    }
    
    /**
     * Parameterized constructor for creating a team with basic info
     * 
     * @param teamId    Unique team ID from API
     * @param teamName  Team display name
     * @param leagueId  League this team belongs to
     */
    public Team(int teamId, String teamName, int leagueId) {
        this();  // Call default constructor to initialize defaults
        this.teamId = teamId;
        this.teamName = teamName;
        this.leagueId = leagueId;
    }
    
    // ========== GETTERS AND SETTERS (JavaBean pattern) ==========
    
    public int getTeamId() {
        return teamId;
    }
    
    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
    public int getLeagueId() {
        return leagueId;
    }
    
    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }
    
    public int getEloRating() {
        return eloRating;
    }
    
    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }
    
    public String getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public int[] getLast5Results() {
        return last5Results;
    }
    
    /**
     * Sets the last 5 results and automatically updates goal statistics
     * 
     * @param last5Results Array of 5 integers (3=Win, 1=Draw, 0=Loss)
     */
    public void setLast5Results(int[] last5Results) {
        if (last5Results != null && last5Results.length == 5) {
            this.last5Results = last5Results;
        }
    }
    
    public int getGoalsScored() {
        return goalsScored;
    }
    
    public void setGoalsScored(int goalsScored) {
        this.goalsScored = goalsScored;
    }
    
    public int getGoalsConceded() {
        return goalsConceded;
    }
    
    public void setGoalsConceded(int goalsConceded) {
        this.goalsConceded = goalsConceded;
    }
    
    // ========== BUSINESS LOGIC METHODS ==========
    
    /**
     * Calculates team's form percentage based on last 5 matches.
     * Formula: (Total points from last 5 matches) / (Maximum possible points)
     * - Win = 3 points
     * - Draw = 1 point
     * - Loss = 0 points
     * - Maximum possible points from 5 matches = 15
     * 
     * @return Form percentage between 0.0 and 1.0
     */
    public double getFormPercentage() {
        double totalPoints = 0;
        for (int result : last5Results) {
            totalPoints += result;
        }
        return totalPoints / 15.0;
    }
    
    /**
     * Calculates goal difference (scored - conceded)
     * 
     * @return Goal difference (positive = good, negative = bad)
     */
    public int getGoalDifference() {
        return goalsScored - goalsConceded;
    }
    
    /**
     * Adds a new result to the form history (shifts array and updates goals)
     * 
     * @param result      Match result (3=Win, 1=Draw, 0=Loss)
     * @param goalsFor    Goals scored by this team
     * @param goalsAgainst Goals conceded by this team
     */
    public void addResult(int result, int goalsFor, int goalsAgainst) {
        // Shift results left (drop oldest, add newest)
        System.arraycopy(last5Results, 1, last5Results, 0, 4);
        last5Results[4] = result;
        
        // Update goal statistics
        this.goalsScored += goalsFor;
        this.goalsConceded += goalsAgainst;
    }
    
    /**
     * Checks if team is on a winning streak
     * 
     * @return true if last 3 matches are wins
     */
    public boolean isOnWinningStreak() {
        if (last5Results.length >= 3) {
            return last5Results[2] == 3 && last5Results[3] == 3 && last5Results[4] == 3;
        }
        return false;
    }
    
    /**
     * Checks if team is on a losing streak
     * 
     * @return true if last 3 matches are losses
     */
    public boolean isOnLosingStreak() {
        if (last5Results.length >= 3) {
            return last5Results[2] == 0 && last5Results[3] == 0 && last5Results[4] == 0;
        }
        return false;
    }
    
    /**
     * Returns a formatted string representation of the team's form
     * 
     * @return String like "W-W-D-L-W" for last 5 matches
     */
    public String getFormString() {
        StringBuilder sb = new StringBuilder();
        for (int result : last5Results) {
            if (result == 3) {
                sb.append("W");
            } else if (result == 1) {
                sb.append("D");
            } else {
                sb.append("L");
            }
            sb.append("-");
        }
        // Remove trailing dash
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s (ELO: %d) | Form: %s | GD: %d", 
            teamName, eloRating, getFormString(), getGoalDifference());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Team team = (Team) obj;
        return teamId == team.teamId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(teamId);
    }
}