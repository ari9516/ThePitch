package com.thepitch.analyzer;

import com.thepitch.model.Match;
import com.thepitch.model.Team;
import com.thepitch.model.RecentForm;
import java.util.*;

public class RecentFormAnalyzer {
    
    /**
     * Analyze last 5 games for a team
     */
    public RecentForm analyzeLast5Games(Team team, List<Match> last5Matches) {
        RecentForm form = new RecentForm();
        form.teamName = team.getTeamName();
        
        if (last5Matches.isEmpty()) {
            form.formString = "-----";
            form.formPercentage = 0;
            form.streak = "❓ INSUFFICIENT DATA";
            form.trend = "➡️ UNKNOWN";
            return form;
        }
        
        // Build form string (W/D/L)
        StringBuilder formString = new StringBuilder();
        int totalPoints = 0;
        int goalsScored = 0;
        int goalsConceded = 0;
        int cleanSheets = 0;
        int failedToScore = 0;
        int bttsCount = 0;
        
        for (Match match : last5Matches) {
            boolean isHome = match.getHomeTeam().getTeamId() == team.getTeamId();
            int teamGoals = isHome ? match.getHomeScore() : match.getAwayScore();
            int opponentGoals = isHome ? match.getAwayScore() : match.getHomeScore();
            
            // Determine result
            if (teamGoals > opponentGoals) {
                formString.append("W");
                totalPoints += 3;
            } else if (teamGoals < opponentGoals) {
                formString.append("L");
                totalPoints += 0;
            } else {
                formString.append("D");
                totalPoints += 1;
            }
            
            goalsScored += teamGoals;
            goalsConceded += opponentGoals;
            
            if (opponentGoals == 0) cleanSheets++;
            if (teamGoals == 0) failedToScore++;
            if (teamGoals > 0 && opponentGoals > 0) bttsCount++;
        }
        
        form.formString = formString.toString();
        form.pointsFromLast5 = totalPoints;
        form.formPercentage = (totalPoints / 15.0) * 100;
        form.goalsScored = goalsScored;
        form.goalsConceded = goalsConceded;
        form.cleanSheets = cleanSheets;
        form.failedToScore = failedToScore;
        form.bttsCount = bttsCount;
        
        // Detect streak
        form.streak = detectStreak(formString.toString());
        
        // Calculate trend (improving/declining)
        form.trend = calculateTrend(last5Matches, team);
        
        return form;
    }
    
    /**
     * Detect streak from form string
     */
    private String detectStreak(String formString) {
        if (formString.length() < 3) return "📊 INCONCLUSIVE";
        
        if (formString.startsWith("WWW")) return "🔥 3+ WINS IN A ROW - MOMENTUM IS HIGH";
        if (formString.startsWith("LLL")) return "❌ 3+ LOSSES IN A ROW - CRISIS MODE";
        if (formString.startsWith("DDD")) return "🤝 3+ DRAWS IN A ROW - HARD TO BEAT BUT NOT WINNING";
        
        if (formString.contains("WW")) return "📈 WINNING MOMENTUM - CONFIDENCE HIGH";
        if (formString.contains("LL")) return "📉 LOSING MOMENTUM - CONFIDENCE LOW";
        
        if (formString.endsWith("WW")) return "⚡ STRONG FINISH - FINISHING SEASON WELL";
        if (formString.endsWith("LL")) return "⚠️ POOR FINISH - FADING AT CRITICAL TIME";
        
        return "⚖️ MIXED FORM - INCONSISTENT";
    }
    
    /**
     * Calculate form trend by comparing first 2 vs last 2 matches
     */
    private String calculateTrend(List<Match> last5Matches, Team team) {
        if (last5Matches.size() < 4) return "➡️ INSUFFICIENT DATA";
        
        // Calculate points from first 2 matches
        int first2Points = 0;
        for (int i = 0; i < 2 && i < last5Matches.size(); i++) {
            first2Points += getPointsFromMatch(last5Matches.get(i), team);
        }
        
        // Calculate points from last 2 matches
        int last2Points = 0;
        for (int i = last5Matches.size() - 2; i < last5Matches.size(); i++) {
            last2Points += getPointsFromMatch(last5Matches.get(i), team);
        }
        
        double improvement = (last2Points - first2Points) / 2.0; // Average points per game change
        
        if (improvement > 0.5) return "📈 STRONGLY IMPROVING";
        if (improvement > 0) return "📈 IMPROVING";
        if (improvement < -0.5) return "📉 STRONGLY DECLINING";
        if (improvement < 0) return "📉 DECLINING";
        return "➡️ STABLE";
    }
    
    /**
     * Get points from a single match
     */
    private int getPointsFromMatch(Match match, Team team) {
        boolean isHome = match.getHomeTeam().getTeamId() == team.getTeamId();
        int teamGoals = isHome ? match.getHomeScore() : match.getAwayScore();
        int opponentGoals = isHome ? match.getAwayScore() : match.getHomeScore();
        
        if (teamGoals > opponentGoals) return 3;
        if (teamGoals < opponentGoals) return 0;
        return 1;
    }
    
    /**
     * Calculate expected goals based on form
     */
    public double getExpectedGoals(RecentForm form, double seasonAverage) {
        // Adjust season average based on recent form
        double formMultiplier = form.formPercentage / 50.0; // 100% form = 2x, 50% = 1x
        return seasonAverage * formMultiplier;
    }
    
    /**
     * Get momentum factor (0.5 to 1.5)
     */
    public double getMomentumFactor(RecentForm form) {
        if (form.formPercentage >= 80) return 1.3;
        if (form.formPercentage >= 60) return 1.1;
        if (form.formPercentage >= 40) return 0.9;
        if (form.formPercentage >= 20) return 0.7;
        return 0.5;
    }
    
    /**
     * Compare two teams' recent forms
     */
    public String compareForm(RecentForm homeForm, RecentForm awayForm) {
        double formDiff = homeForm.formPercentage - awayForm.formPercentage;
        
        if (formDiff > 30) return "🏠 HOME TEAM IN MUCH BETTER FORM";
        if (formDiff > 15) return "🏠 HOME TEAM IN BETTER FORM";
        if (formDiff < -30) return "✈️ AWAY TEAM IN MUCH BETTER FORM";
        if (formDiff < -15) return "✈️ AWAY TEAM IN BETTER FORM";
        return "⚖️ SIMILAR FORM - FORM IS NOT A DIFFERENTIATOR";
    }
}
