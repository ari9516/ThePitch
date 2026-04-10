package com.thepitch.service;

import com.thepitch.model.Team;
import com.thepitch.model.Match;

/**
 * EloCalculator - Implements ELO rating system for football teams
 * ELO is a mathematical system used to calculate relative skill levels
 * 
 * @author ThePitch Team
 * @version 1.0
 */
public class EloCalculator {
    
    // K-Factor determines how much ratings change after a match
    // Higher K-Factor = more volatile ratings
    private static final int K_FACTOR = 32;
    
    // Home advantage in ELO points (typically 50-100 points)
    private static final int HOME_ADVANTAGE = 50;
    
    /**
     * Calculate expected score for a team
     * Formula: 1 / (1 + 10^((ratingB - ratingA)/400))
     * 
     * @param ratingA Rating of team A
     * @param ratingB Rating of team B
     * @return Expected score between 0 and 1
     */
    public double getExpectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }
    
    /**
     * Calculate expected score with home advantage
     * Home team gets a rating boost
     * 
     * @param homeRating Rating of home team
     * @param awayRating Rating of away team
     * @return Expected score for home team
     */
    public double getExpectedScoreWithHomeAdvantage(int homeRating, int awayRating) {
        return 1.0 / (1.0 + Math.pow(10, (awayRating - (homeRating + HOME_ADVANTAGE)) / 400.0));
    }
    
    /**
     * Calculate win probability for home team
     * 
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return Probability of home win (0.0 to 1.0)
     */
    public double getHomeWinProbability(Team homeTeam, Team awayTeam) {
        return getExpectedScoreWithHomeAdvantage(homeTeam.getEloRating(), awayTeam.getEloRating());
    }
    
    /**
     * Calculate win probability for away team
     * 
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return Probability of away win (0.0 to 1.0)
     */
    public double getAwayWinProbability(Team homeTeam, Team awayTeam) {
        return 1.0 - getExpectedScoreWithHomeAdvantage(homeTeam.getEloRating(), awayTeam.getEloRating());
    }
    
    /**
     * Calculate draw probability based on ELO difference
     * Draw probability decreases as rating difference increases
     * 
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return Probability of draw (0.0 to 1.0)
     */
    public double getDrawProbability(Team homeTeam, Team awayTeam) {
        double ratingDiff = Math.abs(homeTeam.getEloRating() - awayTeam.getEloRating());
        // Base draw probability is 25%
        double drawBase = 0.25;
        // Reduce draw probability as rating difference increases
        // Max reduction 15% when difference is 200+
        double reduction = Math.min(0.15, ratingDiff / 1300.0);
        return Math.max(0.10, drawBase - reduction);
    }
    
    /**
     * Update ELO ratings after a match result
     * 
     * @param match The completed match
     */
    public void updateRatings(Match match) {
        if (!match.isFinished()) {
            System.out.println("Match not finished yet, cannot update ratings");
            return;
        }
        
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();
        
        // Calculate expected scores
        double expectedHome = getExpectedScoreWithHomeAdvantage(homeTeam.getEloRating(), awayTeam.getEloRating());
        double expectedAway = 1.0 - expectedHome;
        
        // Calculate actual scores based on result
        double actualHome, actualAway;
        String result = match.getResult();
        
        if (result.equals("HOME_WIN")) {
            actualHome = 1.0;
            actualAway = 0.0;
        } else if (result.equals("AWAY_WIN")) {
            actualHome = 0.0;
            actualAway = 1.0;
        } else {
            actualHome = 0.5;
            actualAway = 0.5;
        }
        
        // Calculate rating changes
        int homeChange = (int) Math.round(K_FACTOR * (actualHome - expectedHome));
        int awayChange = (int) Math.round(K_FACTOR * (actualAway - expectedAway));
        
        // Apply changes
        homeTeam.setEloRating(homeTeam.getEloRating() + homeChange);
        awayTeam.setEloRating(awayTeam.getEloRating() + awayChange);
        
        System.out.println("ELO Updated: " + homeTeam.getTeamName() + " " + 
                          (homeChange > 0 ? "+" : "") + homeChange + 
                          " | " + awayTeam.getTeamName() + " " + 
                          (awayChange > 0 ? "+" : "") + awayChange);
    }
    
    /**
     * Get rating difference between two teams
     * 
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return Rating difference (positive if home team is stronger)
     */
    public int getRatingDifference(Team homeTeam, Team awayTeam) {
        return (homeTeam.getEloRating() + HOME_ADVANTAGE) - awayTeam.getEloRating();
    }
    
    /**
     * Get form-adjusted probability (combines ELO with recent form)
     * 
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return Adjusted home win probability
     */
    public double getFormAdjustedProbability(Team homeTeam, Team awayTeam) {
        double eloProb = getHomeWinProbability(homeTeam, awayTeam);
        double formProb = homeTeam.getFormPercentage();
        
        // 70% weight to ELO, 30% to form
        return (eloProb * 0.7) + (formProb * 0.3);
    }
}
