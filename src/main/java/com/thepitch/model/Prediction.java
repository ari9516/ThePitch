package com.thepitch.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Prediction JavaBean class representing a match outcome prediction.
 * Contains probabilities for home win, draw, and away win.
 * 
 * @author ThePitch Team
 * @version 2.0
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
    
    // Enhanced features
    private List<String> insights;  // Key insights for this prediction
    private double confidenceScore; // Numeric confidence score (0-100)
    
    /**
     * Default constructor
     */
    public Prediction() {
        this.createdAt = new Date();
        this.accuracyVerified = false;
        this.insights = new ArrayList<>();
        this.confidenceScore = 0;
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
        this.confidenceScore = getMaxProbability() * 100;
    }
    
    /**
     * Enhanced constructor with insights
     * 
     * @param matchId      Match ID this prediction is for
     * @param homeWinProb  Probability of home win (0.0-1.0)
     * @param drawProb     Probability of draw (0.0-1.0)
     * @param awayWinProb  Probability of away win (0.0-1.0)
     * @param insights     List of key insights for the prediction
     */
    public Prediction(int matchId, double homeWinProb, double drawProb, double awayWinProb, List<String> insights) {
        this(matchId, homeWinProb, drawProb, awayWinProb);
        this.insights = insights != null ? insights : new ArrayList<>();
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
        this.confidence = calculateConfidence();
        this.predictedWinner = determinePredictedWinner();
        this.confidenceScore = getMaxProbability() * 100;
    }
    
    public double getDrawProb() {
        return drawProb;
    }
    
    public void setDrawProb(double drawProb) {
        this.drawProb = drawProb;
        this.confidence = calculateConfidence();
        this.predictedWinner = determinePredictedWinner();
        this.confidenceScore = getMaxProbability() * 100;
    }
    
    public double getAwayWinProb() {
        return awayWinProb;
    }
    
    public void setAwayWinProb(double awayWinProb) {
        this.awayWinProb = awayWinProb;
        this.confidence = calculateConfidence();
        this.predictedWinner = determinePredictedWinner();
        this.confidenceScore = getMaxProbability() * 100;
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
    
    public List<String> getInsights() {
        return insights;
    }
    
    public void setInsights(List<String> insights) {
        this.insights = insights;
    }
    
    public void addInsight(String insight) {
        if (this.insights == null) {
            this.insights = new ArrayList<>();
        }
        this.insights.add(insight);
    }
    
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
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
     * Gets the predicted winner as a readable string
     * 
     * @return Readable winner string (e.g., "Arsenal", "Liverpool", "Draw")
     */
    public String getPredictedWinnerName(String homeTeamName, String awayTeamName) {
        if ("HOME".equals(predictedWinner)) {
            return homeTeamName;
        } else if ("AWAY".equals(predictedWinner)) {
            return awayTeamName;
        } else {
            return "Draw";
        }
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
     * @param probability Probability value (0.0-1.0)
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
     * Gets probability bars for all three outcomes
     * 
     * @return String with all three probability bars
     */
    public String getAllProbabilityBars() {
        return String.format("Home: %s %.1f%%\nDraw: %s %.1f%%\nAway: %s %.1f%%",
            getProbabilityBar(homeWinProb), homeWinProb * 100,
            getProbabilityBar(drawProb), drawProb * 100,
            getProbabilityBar(awayWinProb), awayWinProb * 100);
    }
    
    /**
     * Gets detailed prediction summary
     * 
     * @return Multi-line formatted prediction
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("📊 PREDICTION SUMMARY\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append(getAllProbabilityBars());
        sb.append("\n");
        sb.append(String.format("Predicted Winner: %s\n", predictedWinner));
        sb.append(String.format("Confidence:       %s (%.1f%%)\n", confidence, confidenceScore));
        sb.append(String.format("Generated:        %s\n", DATE_FORMAT.format(createdAt)));
        
        if (insights != null && !insights.isEmpty()) {
            sb.append("\n📋 KEY INSIGHTS:\n");
            for (String insight : insights) {
                sb.append("   • ").append(insight).append("\n");
            }
        }
        
        sb.append("═══════════════════════════════════════════════════════════════");
        return sb.toString();
    }
    
    /**
     * Gets a concise one-line summary
     * 
     * @return Concise summary string
     */
    public String getConciseSummary() {
        return String.format("%s | Winner: %s | Confidence: %s (%.1f%%)",
            getFormattedProbabilities(), predictedWinner, confidence, confidenceScore);
    }
    
    /**
     * Gets HTML formatted summary for potential web display
     * 
     * @return HTML formatted string
     */
    public String getHtmlSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='prediction'>");
        sb.append("<h3>Match Prediction</h3>");
        sb.append("<table border='1'>");
        sb.append("<tr><th>Outcome</th><th>Probability</th><th>Bar</th></tr>");
        sb.append(String.format("<tr><td>🏠 Home Win</td><td>%.1f%%</td><td>%s</td></tr>", 
            homeWinProb * 100, getProbabilityBar(homeWinProb)));
        sb.append(String.format("<tr><td>🤝 Draw</td><td>%.1f%%</td><td>%s</td></tr>", 
            drawProb * 100, getProbabilityBar(drawProb)));
        sb.append(String.format("<tr><td>✈️ Away Win</td><td>%.1f%%</td><td>%s</td></tr>", 
            awayWinProb * 100, getProbabilityBar(awayWinProb)));
        sb.append("</table>");
        sb.append(String.format("<p><strong>Predicted Winner:</strong> %s</p>", predictedWinner));
        sb.append(String.format("<p><strong>Confidence:</strong> %s (%.1f%%)</p>", confidence, confidenceScore));
        
        if (insights != null && !insights.isEmpty()) {
            sb.append("<h4>Key Insights:</h4><ul>");
            for (String insight : insights) {
                sb.append("<li>").append(insight).append("</li>");
            }
            sb.append("</ul>");
        }
        
        sb.append("</div>");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("Prediction [Match ID: %d] %s | Winner: %s | Confidence: %s (%.1f%%)", 
            matchId, getFormattedProbabilities(), predictedWinner, confidence, confidenceScore);
    }
}
