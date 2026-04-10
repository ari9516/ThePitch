package com.thepitch.service;

import com.thepitch.model.Match;
import com.thepitch.model.Prediction;
import com.thepitch.model.Team;

/**
 * PredictionEngine - Combines multiple factors to predict match outcomes
 * 
 * @author ThePitch Team
 * @version 1.0
 */
public class PredictionEngine {
    
    private EloCalculator eloCalculator;
    
    // Weight configuration for different prediction factors
    private double eloWeight = 0.50;      // ELO rating importance
    private double formWeight = 0.25;      // Recent form importance
    private double homeAdvantageWeight = 0.15; // Home advantage importance
    private double goalDiffWeight = 0.10;  // Goal difference importance
    
    public PredictionEngine() {
        this.eloCalculator = new EloCalculator();
    }
    
    /**
     * Generate complete prediction for a match
     * 
     * @param match The match to predict
     * @return Prediction object with probabilities
     */
    public Prediction predict(Match match) {
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();
        
        // 1. ELO-based probabilities
        double eloHomeWin = eloCalculator.getHomeWinProbability(homeTeam, awayTeam);
        double eloAwayWin = eloCalculator.getAwayWinProbability(homeTeam, awayTeam);
        double eloDraw = eloCalculator.getDrawProbability(homeTeam, awayTeam);
        
        // Normalize ELO probabilities (ensure they sum to 1.0)
        double eloTotal = eloHomeWin + eloAwayWin + eloDraw;
        eloHomeWin /= eloTotal;
        eloAwayWin /= eloTotal;
        eloDraw /= eloTotal;
        
        // 2. Form-based probabilities
        double formHomeWin = homeTeam.getFormPercentage();
        double formAwayWin = awayTeam.getFormPercentage();
        double formDraw = 1.0 - (formHomeWin + formAwayWin);
        
        // 3. Home advantage factor (standard 5-10% boost)
        double homeAdvantage = 0.075; // 7.5% boost
        
        // 4. Goal difference impact
        double homeGoalImpact = getGoalDifferenceImpact(homeTeam);
        double awayGoalImpact = getGoalDifferenceImpact(awayTeam);
        
        // Weighted combination of all factors
        double finalHomeWin = (eloHomeWin * eloWeight) +
                              (formHomeWin * formWeight) +
                              (homeAdvantage * homeAdvantageWeight) +
                              (homeGoalImpact * goalDiffWeight);
        
        double finalAwayWin = (eloAwayWin * eloWeight) +
                              (formAwayWin * formWeight) +
                              (awayGoalImpact * goalDiffWeight);
        
        double finalDraw = 1.0 - (finalHomeWin + finalAwayWin);
        
        // Ensure probabilities are within reasonable bounds
        finalHomeWin = Math.max(0.05, Math.min(0.85, finalHomeWin));
        finalAwayWin = Math.max(0.05, Math.min(0.85, finalAwayWin));
        finalDraw = Math.max(0.05, Math.min(0.40, finalDraw));
        
        // Re-normalize to ensure sum is 1.0
        double total = finalHomeWin + finalDraw + finalAwayWin;
        finalHomeWin /= total;
        finalDraw /= total;
        finalAwayWin /= total;
        
        return new Prediction(match.getMatchId(), finalHomeWin, finalDraw, finalAwayWin);
    }
    
    /**
     * Calculate goal difference impact on prediction
     * 
     * @param team Team to analyze
     * @return Impact factor between -0.1 and +0.1
     */
    private double getGoalDifferenceImpact(Team team) {
        int goalDiff = team.getGoalDifference();
        if (goalDiff > 0) {
            return Math.min(0.1, goalDiff / 50.0);
        } else if (goalDiff < 0) {
            return Math.max(-0.1, goalDiff / 50.0);
        }
        return 0;
    }
    
    /**
     * Generate a simple prediction summary for display
     * 
     * @param match The match
     * @param prediction The prediction
     * @return Formatted summary string
     */
    public String getPredictionSummary(Match match, Prediction prediction) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n┌─────────────────────────────────────────────────┐\n");
        sb.append(String.format("│ %-30s vs %-20s │\n", 
                  match.getHomeTeam().getTeamName(), 
                  match.getAwayTeam().getTeamName()));
        sb.append("├─────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ Home Win:  %-8s %5.1f%%                         │\n", 
                  getProbabilityBar(prediction.getHomeWinProb()), 
                  prediction.getHomeWinProb() * 100));
        sb.append(String.format("│ Draw:      %-8s %5.1f%%                         │\n", 
                  getProbabilityBar(prediction.getDrawProb()), 
                  prediction.getDrawProb() * 100));
        sb.append(String.format("│ Away Win:  %-8s %5.1f%%                         │\n", 
                  getProbabilityBar(prediction.getAwayWinProb()), 
                  prediction.getAwayWinProb() * 100));
        sb.append("├─────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ Confidence: %-35s │\n", prediction.getConfidence()));
        sb.append(String.format("│ Predicted:  %-35s │\n", prediction.getPredictedWinner()));
        sb.append("└─────────────────────────────────────────────────┘\n");
        return sb.toString();
    }
    
    /**
     * Create a simple probability bar for visualization
     * 
     * @param probability Probability value (0-1)
     * @return String like "██████▒▒▒▒"
     */
    private String getProbabilityBar(double probability) {
        int barLength = 10;
        int filled = (int) Math.round(probability * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "▒");
        }
        return bar.toString();
    }
    
    /**
     * Get confidence level based on probability spread
     * 
     * @param prediction The prediction
     * @return "HIGH", "MEDIUM", or "LOW"
     */
    public String getConfidenceLevel(Prediction prediction) {
        double maxProb = prediction.getMaxProbability();
        if (maxProb >= 0.70) return "HIGH 🔥";
        if (maxProb >= 0.55) return "MEDIUM 📊";
        return "LOW ❓";
    }
    
    // Getters for weights (for debugging/tuning)
    public double getEloWeight() { return eloWeight; }
    public double getFormWeight() { return formWeight; }
    public double getHomeAdvantageWeight() { return homeAdvantageWeight; }
    public double getGoalDiffWeight() { return goalDiffWeight; }
}