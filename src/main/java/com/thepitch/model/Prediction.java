package com.thepitch.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Prediction JavaBean class representing a match outcome prediction.
 * Contains probabilities for home win, draw, and away win.
 * 
 * @author ThePitch Team
 * @version 1.0
 */
public class Prediction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    
    // Core properties
    private int predictionId;      // Auto-generated unique ID
    private int matchId;           // Match this prediction is for
    private double homeWinProb;    // Probability home team wins (0.0 to 1.0)
    private double drawProb;       // Probability of draw (0.0 to 1.0)
    private double awayWinProb;    // Probability away team wins (0.0 to 1.0)
    
    // Metadata
    private String confidence;     // HIGH, MEDIUM, LOW
    private String predictedWinner; // HOME, AWAY, DRAW
    private String actualResult;    // Actual result after match (for accuracy tracking)
    private boolean accuracyVerified; // Whether prediction accuracy has been checked
    private Date createdAt;         // When prediction was made
    
    /**
     * Default constructor
     */
    public Prediction() {
        this.createdAt = new Date();
        this.accuracyVerified = false;
    }
    
    /**
     * Parameterized constructor that auto-calculates confidence and predicted winner
     * 
     * @param matchId      Match ID this prediction is for
     * @param homeWinProb  Probability of home win (0.0-1.0)
     * @param drawProb     Probability of draw (0.0-1.0)
     * @param awayWinProb  Probability of away win (0.0-1.0)
     */
    public Prediction(int matchId, double homeWinProb, double drawProb, double awayWinProb) {
        this();
        this.matchId = matchId;
        this.homeWinProb = homeWinProb;
        this.drawProb = drawProb;
        this.awayWinProb = awayWinProb;
        this.confidence = calculateConfidence();
        this.predictedWinner = determinePredictedWinner();
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public int getPredictionId() {
        return predictionId;
    }
    
    public void setPredictionId(int predictionId) {
        this.predictionId = predictionId;
    }
    
    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public double getHomeWinProb() {
        return homeWinProb;
    }
    
    public void setHomeWinProb(double homeWinProb) {
        this.homeWinProb = homeWinProb;
    }
    
    public double getDrawProb() {
        return drawProb;
    }
    
    public void setDrawProb(double drawProb) {
        this.drawProb = drawProb;
    }
    
    public double getAwayWinProb() {
        return awayWinProb;
    }
    
    public void setAwayWinProb(double awayWinProb) {
        this.awayWinProb = awayWinProb;
    }
    
    public String getConfidence() {
        return confidence;
    }
    
    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
    
    public String getPredictedWinner() {
        return predictedWinner;
    }
    
    public void setPredictedWinner(String predictedWinner) {
        this.predictedWinner = predictedWinner;
    }
    
    public String getActualResult() {
        return actualResult;
    }
    
    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }
    
    public boolean isAccuracyVerified() {
        return accuracyVerified;
    }
    
    public void setAccuracyVerified(boolean accuracyVerified) {
        this.accuracyVerified = accuracyVerified;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    // ========== BUSINESS LOGIC METHODS ==========
    
    /**
     * Calculates confidence level based on highest probability
     * - HIGH: Max probability >= 70%
     * - MEDIUM: Max probability between 50% and 70%
     * - LOW: Max probability < 50%
     * 
     * @return Confidence level string
     */
    private String calculateConfidence() {
        double maxProb = Math.max(homeWinProb, Math.max(drawProb, awayWinProb));
        if (maxProb >= 0.70) {
            return "HIGH";
        } else if (maxProb >= 0.50) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Determines the predicted winner based on highest probability
     * 
     * @return "HOME", "AWAY", or "DRAW"
     */
    private String determinePredictedWinner() {
        if (homeWinProb > drawProb && homeWinProb > awayWinProb) {
            return "HOME";
        } else if (awayWinProb > homeWinProb && awayWinProb > drawProb) {
            return "AWAY";
        } else {
            return "DRAW";
        }
    }
    
    /**
     * Gets the highest probability value
     * 
     * @return Maximum probability (0.0-1.0)
     */
    public double getMaxProbability() {
        return Math.max(homeWinProb, Math.max(drawProb, awayWinProb));
    }
    
    /**
     * Gets the highest probability as a percentage
     * 
     * @return Percentage string (e.g., "72.5%")
     */
    public String getMaxProbabilityPercent() {
        return String.format("%.1f%%", getMaxProbability() * 100);
    }
    
    /**
     * Verifies if the prediction was correct against actual result
     * 
     * @param actualResult Actual match result ("HOME_WIN", "AWAY_WIN", "DRAW")
     * @return true if prediction was correct
     */
    public boolean verifyAccuracy(String actualResult) {
        this.actualResult = actualResult;
        this.accuracyVerified = true;
        return this.predictedWinner.equals(actualResult);
    }
    
    /**
     * Gets formatted probability string for display
     * 
     * @return String like "🏠 45.0% | 🤝 25.0% | ✈️ 30.0%"
     */
    public String getFormattedProbabilities() {
        return String.format("🏠 %.1f%% | 🤝 %.1f%% | ✈️ %.1f%%", 
            homeWinProb * 100, 
            drawProb * 100, 
            awayWinProb * 100);
    }
    
    /**
     * Gets probability bar representation (for simple visualization)
     * 
     * @return String like "[████████░░] 80%"
     */
    public String getProbabilityBar(double probability) {
        int barLength = 10;
        int filled = (int) Math.round(probability * barLength);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }
    
    /**
     * Gets detailed prediction summary
     * 
     * @return Multi-line formatted prediction
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("📊 PREDICTION SUMMARY\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(String.format("Home Win:  %s %.1f%%\n", 
            getProbabilityBar(homeWinProb), homeWinProb * 100));
        sb.append(String.format("Draw:      %s %.1f%%\n", 
            getProbabilityBar(drawProb), drawProb * 100));
        sb.append(String.format("Away Win:  %s %.1f%%\n", 
            getProbabilityBar(awayWinProb), awayWinProb * 100));
        sb.append("\n");
        sb.append(String.format("Predicted Winner: %s\n", predictedWinner));
        sb.append(String.format("Confidence:       %s (%s)\n", confidence, getMaxProbabilityPercent()));
        sb.append(String.format("Generated:        %s\n", DATE_FORMAT.format(createdAt)));
        sb.append("════════════════════════════════════════");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("Prediction [Match ID: %d] %s | Winner: %s | Confidence: %s", 
            matchId, getFormattedProbabilities(), predictedWinner, confidence);
    }
}