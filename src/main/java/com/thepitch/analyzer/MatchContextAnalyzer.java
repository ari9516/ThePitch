package com.thepitch.analyzer;

import com.thepitch.model.Match;
import com.thepitch.model.Team;
import java.util.*;

public class MatchContextAnalyzer {
    
    // League standings positions (to be populated from API/CSV)
    private Map<Integer, Integer> teamPositions = new HashMap<>();
    private Map<Integer, Integer> teamPoints = new HashMap<>();
    
    public void updateStandings(Map<Integer, Integer> positions, Map<Integer, Integer> points) {
        this.teamPositions = positions;
        this.teamPoints = points;
    }
    
    /**
     * Determine what's at stake for this match
     */
    public String determineMatchStakes(Team homeTeam, Team awayTeam, List<Match> allMatches) {
        int homePosition = getTeamPosition(homeTeam.getTeamId());
        int awayPosition = getTeamPosition(awayTeam.getTeamId());
        int homePoints = getTeamPoints(homeTeam.getTeamId());
        int awayPoints = getTeamPoints(awayTeam.getTeamId());
        
        // Calculate points gap to different targets
        int top4Points = getTargetPoints(4);    // Champions League
        int top7Points = getTargetPoints(7);    // European spots
        int safePoints = getTargetPoints(17);   // Safety (above relegation)
        
        // Title/Champions League race (Top 4)
        if (homePosition <= 4 || awayPosition <= 4) {
            int pointsBehindHome = top4Points - homePoints;
            int pointsBehindAway = top4Points - awayPoints;
            if (pointsBehindHome <= 9 && pointsBehindHome > 0) {
                return String.format("🏆 TITLE/CHAMPIONS LEAGUE RACE - %s %d points behind 4th", 
                    homeTeam.getTeamName(), pointsBehindHome);
            }
            if (pointsBehindAway <= 9 && pointsBehindAway > 0) {
                return String.format("🏆 TITLE/CHAMPIONS LEAGUE RACE - %s %d points behind 4th", 
                    awayTeam.getTeamName(), pointsBehindAway);
            }
            if (homePosition <= 4 && awayPosition <= 4) {
                return "🏆 TOP 4 SHOWDOWN - Direct Champions League qualification battle";
            }
        }
        
        // European spots (5th-7th)
        if (homePosition <= 7 || awayPosition <= 7) {
            int pointsBehindHome = top7Points - homePoints;
            int pointsBehindAway = top7Points - awayPoints;
            if (pointsBehindHome <= 6 && pointsBehindHome > 0) {
                return String.format("🇪🇺 EUROPEAN SPOT RACE - %s %d points behind European places", 
                    homeTeam.getTeamName(), pointsBehindHome);
            }
            if (pointsBehindAway <= 6 && pointsBehindAway > 0) {
                return String.format("🇪🇺 EUROPEAN SPOT RACE - %s %d points behind European places", 
                    awayTeam.getTeamName(), pointsBehindAway);
            }
        }
        
        // Relegation battle (18th-20th)
        if (homePosition >= 17 || awayPosition >= 17) {
            int pointsAboveHome = homePoints - safePoints;
            int pointsAboveAway = awayPoints - safePoints;
            if (pointsAboveHome <= 6 && pointsAboveHome > 0) {
                return String.format("⚠️ RELEGATION BATTLE - %s only %d points above drop zone", 
                    homeTeam.getTeamName(), pointsAboveHome);
            }
            if (pointsAboveAway <= 6 && pointsAboveAway > 0) {
                return String.format("⚠️ RELEGATION BATTLE - %s only %d points above drop zone", 
                    awayTeam.getTeamName(), pointsAboveAway);
            }
            if (homePosition >= 17 && awayPosition >= 17) {
                return "⚠️ SIX-POINTER RELEGATION BATTLE - Loser faces severe consequences";
            }
        }
        
        // Derby matches
        if (isDerby(homeTeam.getTeamName(), awayTeam.getTeamName())) {
            return "💥 LOCAL DERBY - Form goes out the window";
        }
        
        // Mid-table (Dead rubber)
        if (homePosition >= 8 && homePosition <= 16 && awayPosition >= 8 && awayPosition <= 16) {
            int pointsDiff = Math.abs(homePoints - awayPoints);
            if (pointsDiff > 10) {
                return "📋 MID-TABLE CLASH - Little at stake, pride only";
            }
            return "⚔️ MID-TABLE BATTLE - Teams fighting for higher finish";
        }
        
        return "⚔️ STANDARD LEAGUE MATCH";
    }
    
    /**
     * Calculate weight based on match stakes (1.0 = normal, higher = more important)
     */
    public double calculateStakesWeight(String stakes) {
        if (stakes.contains("TITLE") || stakes.contains("CHAMPIONS LEAGUE")) return 1.5;
        if (stakes.contains("TOP 4 SHOWDOWN")) return 1.6;
        if (stakes.contains("EUROPEAN")) return 1.3;
        if (stakes.contains("RELEGATION")) return 1.4;
        if (stakes.contains("SIX-POINTER")) return 1.5;
        if (stakes.contains("DERBY")) return 1.2;
        if (stakes.contains("MID-TABLE BATTLE")) return 0.9;
        if (stakes.contains("Little at stake")) return 0.7;
        return 1.0;
    }
    
    /**
     * Get team's current position in league
     */
    private int getTeamPosition(int teamId) {
        return teamPositions.getOrDefault(teamId, 10); // Default to mid-table if unknown
    }
    
    /**
     * Get team's current points
     */
    private int getTeamPoints(int teamId) {
        return teamPoints.getOrDefault(teamId, 30); // Default points if unknown
    }
    
    /**
     * Get points needed for target position
     */
    private int getTargetPoints(int position) {
        // This is a simplified calculation - in reality, get from actual standings
        // Typical Premier League: 4th place ~70 points, 7th ~60 points, 17th ~35 points
        switch(position) {
            case 4: return 70;
            case 7: return 60;
            case 17: return 35;
            default: return 40;
        }
    }
    
    /**
     * Check if match is a local derby
     */
    private boolean isDerby(String team1, String team2) {
        String[] derbies = {
            "Arsenal vs Tottenham", "Tottenham vs Arsenal",
            "Liverpool vs Everton", "Everton vs Liverpool",
            "Manchester United vs Manchester City", "Manchester City vs Manchester United",
            "Chelsea vs Arsenal", "Arsenal vs Chelsea",
            "Newcastle vs Sunderland", "Sunderland vs Newcastle",
            "West Ham vs Millwall", "Millwall vs West Ham"
        };
        
        String matchup = team1 + " vs " + team2;
        for (String derby : derbies) {
            if (matchup.equals(derby)) return true;
        }
        return false;
    }
    
    /**
     * Calculate points gap between teams
     */
    public int getPointsGap(Team homeTeam, Team awayTeam) {
        int homePoints = getTeamPoints(homeTeam.getTeamId());
        int awayPoints = getTeamPoints(awayTeam.getTeamId());
        return Math.abs(homePoints - awayPoints);
    }
    
    /**
     * Get motivation factor based on stakes
     */
    public double getMotivationFactor(Team team, String stakes) {
        if (stakes.contains(team.getTeamName())) {
            if (stakes.contains("RELEGATION")) return 1.3;  // Extra motivated to avoid relegation
            if (stakes.contains("TITLE")) return 1.2;      // Pushing for title
            if (stakes.contains("EUROPEAN")) return 1.15;   // Fighting for Europe
        }
        return 1.0;
    }
}

