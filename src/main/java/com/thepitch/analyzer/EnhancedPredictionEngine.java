package com.thepitch.analyzer;

import com.thepitch.model.*;
import com.thepitch.service.EloCalculator;
import java.util.*;

public class EnhancedPredictionEngine {
    
    private Random random = new Random();
    
    /**
     * Generate enhanced prediction using all available factors
     */
    public Prediction generatePrediction(Match match, 
                                          String stakes,
                                          double stakesWeight,
                                          TeamProfile homeProfile,
                                          TeamProfile awayProfile,
                                          RecentForm homeForm,
                                          RecentForm awayForm,
                                          ExternalFactors factors,
                                          EloCalculator eloCalculator) {
        
        // Get teams from match
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();
        
        // 1. Base probability from ELO
        double baseHomeProb = eloCalculator.getHomeWinProbability(homeTeam, awayTeam);
        double baseDrawProb = eloCalculator.getDrawProbability(homeTeam, awayTeam);
        double baseAwayProb = eloCalculator.getAwayWinProbability(homeTeam, awayTeam);
        
        // Normalize base probabilities (ensure they sum to 1.0)
        double baseTotal = baseHomeProb + baseDrawProb + baseAwayProb;
        if (baseTotal > 0) {
            baseHomeProb /= baseTotal;
            baseDrawProb /= baseTotal;
            baseAwayProb /= baseTotal;
        } else {
            baseHomeProb = 0.40;
            baseDrawProb = 0.25;
            baseAwayProb = 0.35;
        }
        
        // 2. Apply stakes weight
        baseHomeProb *= stakesWeight;
        
        // 3. Apply Home/Away Profile adjustments
        double homeProfileAdjustment = calculateHomeProfileAdvantage(homeProfile, awayProfile);
        double awayProfileAdjustment = calculateAwayProfileAdvantage(awayProfile, homeProfile);
        
        // 4. Apply Recent Form adjustments
        double formAdjustment = (homeForm.formPercentage - awayForm.formPercentage) / 100;
        
        // 5. Apply External Factors
        double externalAdjustment = factors.calculateInjuryImpact() + 
                                     factors.calculateWeatherImpact() + 
                                     factors.calculateFatigueImpact();
        
        // 6. Apply home advantage boost
        double homeAdvantageBoost = 0.08; // Base 8% home advantage
        
        // Calculate final probabilities
        double finalHomeProb = baseHomeProb + homeProfileAdjustment + formAdjustment + 
                               externalAdjustment + homeAdvantageBoost;
        double finalAwayProb = baseAwayProb + awayProfileAdjustment - formAdjustment - 
                                externalAdjustment;
        double finalDrawProb = baseDrawProb;
        
        // Ensure probabilities are within reasonable bounds
        finalHomeProb = Math.max(0.15, Math.min(0.75, finalHomeProb));
        finalAwayProb = Math.max(0.10, Math.min(0.65, finalAwayProb));
        finalDrawProb = Math.max(0.15, Math.min(0.35, finalDrawProb));
        
        // Normalize to sum to 1.0
        double finalTotal = finalHomeProb + finalDrawProb + finalAwayProb;
        if (finalTotal > 0) {
            finalHomeProb /= finalTotal;
            finalDrawProb /= finalTotal;
            finalAwayProb /= finalTotal;
        }
        
        // Generate insights
        List<String> insights = generateInsights(match, homeProfile, awayProfile, 
                                                  homeForm, awayForm, factors);
        
        // Create prediction
        Prediction prediction = new Prediction(match.getMatchId(), finalHomeProb, finalDrawProb, finalAwayProb);
        prediction.setInsights(insights);
        prediction.setConfidence(calculateConfidence(finalHomeProb, finalDrawProb, finalAwayProb));
        prediction.setConfidenceScore(getConfidenceScore(finalHomeProb, finalDrawProb, finalAwayProb));
        
        return prediction;
    }
    
    /**
     * Calculate confidence score (0-100)
     */
    private double getConfidenceScore(double homeProb, double drawProb, double awayProb) {
        double maxProb = Math.max(homeProb, Math.max(drawProb, awayProb));
        double secondMax = getSecondMax(homeProb, drawProb, awayProb);
        double spread = maxProb - secondMax;
        
        // Base score on max probability and spread
        double score = maxProb * 100;
        score += spread * 50;
        
        return Math.min(100, Math.max(0, score));
    }
    
    /**
     * Calculate home profile advantage
     */
    private double calculateHomeProfileAdvantage(TeamProfile home, TeamProfile away) {
        double advantage = 0;
        
        // Goal difference advantage
        double homeGoalDiff = home.goalsScored - home.goalsConceded;
        double awayGoalDiff = away.goalsScored - away.goalsConceded;
        advantage += (homeGoalDiff - awayGoalDiff) / 50;
        
        // Corner advantage
        advantage += (home.cornersFor - away.cornersAgainst) / 100;
        
        // Shot accuracy advantage
        advantage += (home.shotAccuracy - away.shotAccuracy) / 200;
        
        // Clean sheet advantage
        advantage += (home.cleanSheets - away.cleanSheets) / 50;
        
        return Math.max(-0.15, Math.min(0.15, advantage));
    }
    
    /**
     * Calculate away profile advantage
     */
    private double calculateAwayProfileAdvantage(TeamProfile away, TeamProfile home) {
        double advantage = 0;
        
        // Goal difference advantage
        double awayGoalDiff = away.goalsScored - away.goalsConceded;
        double homeGoalDiff = home.goalsScored - home.goalsConceded;
        advantage += (awayGoalDiff - homeGoalDiff) / 50;
        
        // Counter-attacking advantage (away teams often counter)
        if (away.shotsTotal > 0) {
            advantage += (away.shotsOnTarget / away.shotsTotal) / 10;
        }
        
        return Math.max(-0.10, Math.min(0.10, advantage));
    }
    
    /**
     * Generate insights for the prediction
     */
    private List<String> generateInsights(Match match, TeamProfile homeProfile, TeamProfile awayProfile,
                                          RecentForm homeForm, RecentForm awayForm, ExternalFactors factors) {
        List<String> insights = new ArrayList<>();
        
        // Home team insights
        if (homeProfile.streakAlerts != null && !homeProfile.streakAlerts.isEmpty()) {
            insights.add("🏠 " + homeProfile.teamName + ": " + homeProfile.streakAlerts.get(0));
        }
        
        if (homeForm.formPercentage >= 70) {
            insights.add("🏠 " + homeProfile.teamName + " are in EXCELLENT form (" + 
                        String.format("%.0f", homeForm.formPercentage) + "% points in last 5)");
        } else if (homeForm.formPercentage <= 30) {
            insights.add("⚠️ " + homeProfile.teamName + " are in POOR form (" + 
                        String.format("%.0f", homeForm.formPercentage) + "% points in last 5)");
        }
        
        if (homeForm.streak != null && homeForm.streak.contains("WINNING")) {
            insights.add("🔥 " + homeProfile.teamName + " " + homeForm.streak);
        }
        
        // Away team insights
        if (awayProfile.streakAlerts != null && !awayProfile.streakAlerts.isEmpty()) {
            insights.add("✈️ " + awayProfile.teamName + ": " + awayProfile.streakAlerts.get(0));
        }
        
        if (awayForm.formPercentage >= 70) {
            insights.add("✈️ " + awayProfile.teamName + " are in EXCELLENT away form");
        }
        
        if (awayForm.streak != null && awayForm.streak.contains("WINNING")) {
            insights.add("🔥 " + awayProfile.teamName + " " + awayForm.streak);
        }
        
        // Key matchup insights
        if (homeProfile.cornersFor > 6.5 && awayProfile.cornersAgainst > 5.5) {
            insights.add("🏁 Expecting HIGH CORNER count (Home avg " + 
                        String.format("%.1f", homeProfile.cornersFor) + " corners/game)");
        }
        
        if (homeProfile.shotAccuracy > 45 && awayProfile.cleanSheets > 3) {
            insights.add("🎯 " + homeProfile.teamName + "'s shot accuracy vs " + 
                        awayProfile.teamName + "'s solid defense - key battle");
        }
        
        if (homeProfile.goalsScored > 2.0 && awayProfile.goalsScored > 1.5) {
            insights.add("⚽ Both teams have strong attacks - EXPECT GOALS");
        }
        
        if (homeProfile.cleanSheets > 8 && awayForm.failedToScore > 2) {
            insights.add("🧤 " + homeProfile.teamName + "'s strong defense vs " + 
                        awayProfile.teamName + "'s scoring struggles");
        }
        
        // BTTS insight
        if (homeProfile.bttsCount > 10 && awayProfile.bttsCount > 10) {
            insights.add("🎯 Both teams have high BTTS rates - 'Both Teams to Score' likely");
        }
        
        // External factors insights
        if (factors.homeTeamInjuries != null && !factors.homeTeamInjuries.isEmpty()) {
            insights.add("⚠️ " + homeProfile.teamName + " missing key players: " + 
                        String.join(", ", factors.homeTeamInjuries));
        }
        
        if (factors.awayTeamInjuries != null && !factors.awayTeamInjuries.isEmpty()) {
            insights.add("⚠️ " + awayProfile.teamName + " missing key players: " + 
                        String.join(", ", factors.awayTeamInjuries));
        }
        
        if ("Rain".equals(factors.weatherCondition) || "Snow".equals(factors.weatherCondition)) {
            insights.add("🌧️ " + factors.weatherCondition + " conditions - may favor defensive/counter-attacking style");
        }
        
        if (factors.daysSinceLastMatch < 4) {
            insights.add("⏰ Both teams playing on short rest - fitness could be a factor");
        }
        
        return insights;
    }
    
    /**
     * Calculate confidence level based on probability spread
     */
    private String calculateConfidence(double homeProb, double drawProb, double awayProb) {
        double maxProb = Math.max(homeProb, Math.max(drawProb, awayProb));
        double secondMax = getSecondMax(homeProb, drawProb, awayProb);
        double spread = maxProb - secondMax;
        
        if (maxProb >= 0.65 && spread >= 0.20) return "VERY HIGH 🔥";
        if (maxProb >= 0.55 && spread >= 0.15) return "HIGH ✅";
        if (maxProb >= 0.45 && spread >= 0.10) return "MEDIUM 📊";
        return "LOW ❓";
    }
    
    /**
     * Get second highest probability
     */
    private double getSecondMax(double a, double b, double c) {
        double[] arr = {a, b, c};
        Arrays.sort(arr);
        return arr[1];
    }
    
    /**
     * Simulate match outcome based on probabilities (for testing)
     */
    public String simulateMatch(Prediction prediction) {
        double random = Math.random();
        
        if (random < prediction.getHomeWinProb()) {
            return "HOME_WIN";
        } else if (random < prediction.getHomeWinProb() + prediction.getDrawProb()) {
            return "DRAW";
        } else {
            return "AWAY_WIN";
        }
    }
}
