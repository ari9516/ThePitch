package com.thepitch.model;

import java.util.List;

public class TeamProfile {
    public String teamName;
    public boolean isHome;
    
    // Goals
    public double goalsScored;
    public double goalsConceded;
    
    // Corners
    public double cornersFor;
    public double cornersAgainst;
    
    // Cards
    public double yellowCards;
    public double redCards;
    
    // Shots
    public double shotsTotal;
    public double shotsOnTarget;
    public double shotAccuracy;
    
    // Possession
    public double possession;
    
    // Other
    public int cleanSheets;
    public int bttsCount;
    public List<String> streakAlerts;
    
    // Formatted output - Java 11 compatible (no text blocks)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Goals: %.1f scored/game | %.1f conceded/game%n", goalsScored, goalsConceded));
        sb.append(String.format("Corners: %.1f/game | Cards: %.1f/game%n", cornersFor, yellowCards));
        sb.append(String.format("Shots: %.1f/game | On target: %.1f (%.0f%%)%n", shotsTotal, shotsOnTarget, shotAccuracy));
        sb.append(String.format("Clean sheets: %d | BTTS: %d%n", cleanSheets, bttsCount));
        return sb.toString();
    }
    
    // Alternative formatted output for home/away distinction
    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append(String.format("🏠 %s - %s PROFILE%n", teamName, isHome ? "HOME" : "AWAY"));
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("Goals: %.1f scored/game | %.1f conceded/game%n", goalsScored, goalsConceded));
        sb.append(String.format("Corners: %.1f/game | Cards: %.1f/game%n", cornersFor, yellowCards));
        sb.append(String.format("Shots: %.1f/game | On target: %.1f (%.0f%%)%n", shotsTotal, shotsOnTarget, shotAccuracy));
        sb.append(String.format("Clean sheets: %d | BTTS: %d%n", cleanSheets, bttsCount));
        sb.append(String.format("Possession: %.1f%%%n", possession));
        
        if (streakAlerts != null && !streakAlerts.isEmpty()) {
            sb.append("\n🔥 STREAKS:\n");
            for (String streak : streakAlerts) {
                sb.append(String.format("   • %s%n", streak));
            }
        }
        sb.append("═══════════════════════════════════════════════════════════════");
        return sb.toString();
    }
}
