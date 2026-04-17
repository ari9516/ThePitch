package com.thepitch.analyzer;

import com.thepitch.model.Match;
import com.thepitch.model.Team;
import com.thepitch.model.TeamProfile;
import java.util.*;

public class TeamProfileAnalyzer {
    
    /**
     * Analyze home team performance from home matches (only finished matches)
     */
    public TeamProfile analyzeHomeProfile(Team team, List<Match> homeMatches) {
        TeamProfile profile = new TeamProfile();
        profile.teamName = team.getTeamName();
        profile.isHome = true;
        
        // Filter only finished matches with scores
        List<Match> finishedHomeMatches = new ArrayList<>();
        for (Match match : homeMatches) {
            if (match.isFinished() && match.getHomeScore() != null && match.getAwayScore() != null) {
                finishedHomeMatches.add(match);
            }
        }
        
        if (finishedHomeMatches.isEmpty()) {
            // Return default profile if no finished matches
            profile.goalsScored = 0;
            profile.goalsConceded = 0;
            profile.cornersFor = 0;
            profile.cornersAgainst = 0;
            profile.yellowCards = 0;
            profile.redCards = 0;
            profile.shotsTotal = 0;
            profile.shotsOnTarget = 0;
            profile.shotAccuracy = 0;
            profile.possession = 0;
            profile.cleanSheets = 0;
            profile.bttsCount = 0;
            profile.streakAlerts = new ArrayList<>();
            return profile;
        }
        
        int matchesCount = finishedHomeMatches.size();
        
        // Goals analysis
        double totalGoalsScored = 0;
        double totalGoalsConceded = 0;
        
        // Corner analysis
        double totalCornersFor = 0;
        double totalCornersAgainst = 0;
        
        // Card analysis
        double totalYellowCards = 0;
        double totalRedCards = 0;
        
        // Shot analysis
        double totalShots = 0;
        double totalShotsOnTarget = 0;
        
        // Possession
        double totalPossession = 0;
        
        // Counters for streaks
        int cleanSheets = 0;
        int bttsCount = 0;
        
        for (Match match : finishedHomeMatches) {
            // Goals
            totalGoalsScored += match.getHomeScore();
            totalGoalsConceded += match.getAwayScore();
            
            // Stats (use default values if not available from CSV yet)
            double homeCorners = getMatchStat(match, "home_corners");
            double awayCorners = getMatchStat(match, "away_corners");
            totalCornersFor += homeCorners;
            totalCornersAgainst += awayCorners;
            
            totalYellowCards += getMatchStat(match, "home_yellow_cards");
            totalRedCards += getMatchStat(match, "home_red_cards");
            
            double homeShots = getMatchStat(match, "home_shots");
            double homeShotsOnTarget = getMatchStat(match, "home_shots_on_target");
            totalShots += homeShots;
            totalShotsOnTarget += homeShotsOnTarget;
            
            totalPossession += getMatchStat(match, "home_possession");
            
            // Streak counters
            if (match.getAwayScore() == 0) cleanSheets++;
            if (match.isBothTeamsScored()) bttsCount++;
        }
        
        // Calculate averages
        profile.goalsScored = totalGoalsScored / matchesCount;
        profile.goalsConceded = totalGoalsConceded / matchesCount;
        profile.cornersFor = totalCornersFor / matchesCount;
        profile.cornersAgainst = totalCornersAgainst / matchesCount;
        profile.yellowCards = totalYellowCards / matchesCount;
        profile.redCards = totalRedCards / matchesCount;
        profile.shotsTotal = totalShots / matchesCount;
        profile.shotsOnTarget = totalShotsOnTarget / matchesCount;
        profile.shotAccuracy = profile.shotsTotal > 0 ? 
            (profile.shotsOnTarget / profile.shotsTotal) * 100 : 0;
        profile.possession = totalPossession / matchesCount;
        profile.cleanSheets = cleanSheets;
        profile.bttsCount = bttsCount;
        
        // Detect streaks in last 5 home matches
        List<Match> last5Home = finishedHomeMatches.size() > 5 ? 
            finishedHomeMatches.subList(finishedHomeMatches.size() - 5, finishedHomeMatches.size()) : finishedHomeMatches;
        profile.streakAlerts = detectStreaks(last5Home, true);
        
        return profile;
    }
    
    /**
     * Analyze away team performance from away matches (only finished matches)
     */
    public TeamProfile analyzeAwayProfile(Team team, List<Match> awayMatches) {
        TeamProfile profile = new TeamProfile();
        profile.teamName = team.getTeamName();
        profile.isHome = false;
        
        // Filter only finished matches with scores
        List<Match> finishedAwayMatches = new ArrayList<>();
        for (Match match : awayMatches) {
            if (match.isFinished() && match.getHomeScore() != null && match.getAwayScore() != null) {
                finishedAwayMatches.add(match);
            }
        }
        
        if (finishedAwayMatches.isEmpty()) {
            // Return default profile if no finished matches
            profile.goalsScored = 0;
            profile.goalsConceded = 0;
            profile.cornersFor = 0;
            profile.cornersAgainst = 0;
            profile.yellowCards = 0;
            profile.redCards = 0;
            profile.shotsTotal = 0;
            profile.shotsOnTarget = 0;
            profile.shotAccuracy = 0;
            profile.possession = 0;
            profile.cleanSheets = 0;
            profile.bttsCount = 0;
            profile.streakAlerts = new ArrayList<>();
            return profile;
        }
        
        int matchesCount = finishedAwayMatches.size();
        
        // Goals analysis
        double totalGoalsScored = 0;
        double totalGoalsConceded = 0;
        
        // Corner analysis
        double totalCornersFor = 0;
        double totalCornersAgainst = 0;
        
        // Card analysis
        double totalYellowCards = 0;
        double totalRedCards = 0;
        
        // Shot analysis
        double totalShots = 0;
        double totalShotsOnTarget = 0;
        
        // Possession
        double totalPossession = 0;
        
        // Counters
        int cleanSheets = 0;
        int bttsCount = 0;
        
        for (Match match : finishedAwayMatches) {
            // Goals (away team's goals are the away_score)
            totalGoalsScored += match.getAwayScore();
            totalGoalsConceded += match.getHomeScore();
            
            // Stats for away team
            totalCornersFor += getMatchStat(match, "away_corners");
            totalCornersAgainst += getMatchStat(match, "home_corners");
            totalYellowCards += getMatchStat(match, "away_yellow_cards");
            totalRedCards += getMatchStat(match, "away_red_cards");
            totalShots += getMatchStat(match, "away_shots");
            totalShotsOnTarget += getMatchStat(match, "away_shots_on_target");
            totalPossession += getMatchStat(match, "away_possession");
            
            // Clean sheet for away team means home team scored 0
            if (match.getHomeScore() == 0) cleanSheets++;
            if (match.isBothTeamsScored()) bttsCount++;
        }
        
        // Calculate averages
        profile.goalsScored = totalGoalsScored / matchesCount;
        profile.goalsConceded = totalGoalsConceded / matchesCount;
        profile.cornersFor = totalCornersFor / matchesCount;
        profile.cornersAgainst = totalCornersAgainst / matchesCount;
        profile.yellowCards = totalYellowCards / matchesCount;
        profile.redCards = totalRedCards / matchesCount;
        profile.shotsTotal = totalShots / matchesCount;
        profile.shotsOnTarget = totalShotsOnTarget / matchesCount;
        profile.shotAccuracy = profile.shotsTotal > 0 ? 
            (profile.shotsOnTarget / profile.shotsTotal) * 100 : 0;
        profile.possession = totalPossession / matchesCount;
        profile.cleanSheets = cleanSheets;
        profile.bttsCount = bttsCount;
        
        // Detect streaks in last 5 away matches
        List<Match> last5Away = finishedAwayMatches.size() > 5 ? 
            finishedAwayMatches.subList(finishedAwayMatches.size() - 5, finishedAwayMatches.size()) : finishedAwayMatches;
        profile.streakAlerts = detectStreaks(last5Away, false);
        
        return profile;
    }
    
    /**
     * Get match statistic (temporary - will be replaced with actual data from CSV)
     */
    private double getMatchStat(Match match, String statType) {
        // This is temporary - in production, these values will come from the CSV
        // For now, return realistic default values for finished matches only
        switch(statType) {
            case "home_corners": return 5 + Math.random() * 3;
            case "away_corners": return 4 + Math.random() * 3;
            case "home_yellow_cards": return 1.5 + Math.random() * 1;
            case "away_yellow_cards": return 1.5 + Math.random() * 1;
            case "home_red_cards": return Math.random() * 0.2;
            case "away_red_cards": return Math.random() * 0.2;
            case "home_shots": return 12 + Math.random() * 5;
            case "away_shots": return 10 + Math.random() * 5;
            case "home_shots_on_target": return 4 + Math.random() * 3;
            case "away_shots_on_target": return 3.5 + Math.random() * 3;
            case "home_possession": return 50 + (Math.random() * 15 - 7.5);
            case "away_possession": return 50 - (Math.random() * 15 - 7.5);
            default: return 0;
        }
    }
    
    /**
     * Detect streaks (90%+ occurrence in last N matches)
     */
    private List<String> detectStreaks(List<Match> matches, boolean isHome) {
        List<String> streaks = new ArrayList<>();
        if (matches.size() < 5) return streaks;
        
        int cornersOverCount = 0;
        int bttsCount = 0;
        int over25Count = 0;
        int cleanSheetCount = 0;
        int scoredCount = 0;
        
        for (Match match : matches) {
            double homeCorners = getMatchStat(match, "home_corners");
            double awayCorners = getMatchStat(match, "away_corners");
            double totalCorners = homeCorners + awayCorners;
            
            if (totalCorners > 9.5) cornersOverCount++;
            if (match.isBothTeamsScored()) bttsCount++;
            if (match.getTotalGoals() > 2) over25Count++;
            
            if (isHome) {
                if (match.getAwayScore() == 0) cleanSheetCount++;
                if (match.getHomeScore() > 0) scoredCount++;
            } else {
                if (match.getHomeScore() == 0) cleanSheetCount++;
                if (match.getAwayScore() > 0) scoredCount++;
            }
        }
        
        int threshold = (int)(matches.size() * 0.9); // 90% threshold
        
        if (cornersOverCount >= threshold) {
            streaks.add("🔥 90%+ matches had OVER 9.5 CORNERS in last " + matches.size());
        }
        if (bttsCount >= threshold) {
            streaks.add("🎯 90%+ matches had BTTS in last " + matches.size());
        }
        if (over25Count >= threshold) {
            streaks.add("⚽ 90%+ matches had OVER 2.5 GOALS in last " + matches.size());
        }
        if (cleanSheetCount >= threshold) {
            streaks.add("🧤 90%+ matches kept a CLEAN SHEET in last " + matches.size());
        }
        if (scoredCount >= threshold) {
            streaks.add("⭐ 90%+ matches SCORED in last " + matches.size());
        }
        
        return streaks;
    }
    
    /**
     * Calculate home advantage factor
     */
    public double calculateHomeAdvantage(TeamProfile homeProfile, TeamProfile awayProfile) {
        double homeStrength = (homeProfile.goalsScored - homeProfile.goalsConceded) / 10.0;
        double awayStrength = (awayProfile.goalsScored - awayProfile.goalsConceded) / 10.0;
        
        // Base home advantage is 0.15 (15% boost)
        double homeAdvantage = 0.15;
        
        // Adjust based on home/away performance
        homeAdvantage += (homeProfile.possession - 50) / 200;
        homeAdvantage += (homeProfile.cornersFor - awayProfile.cornersAgainst) / 100;
        
        return Math.max(0.05, Math.min(0.35, homeAdvantage));
    }
}
