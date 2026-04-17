package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.dao.MatchDAO;
import com.thepitch.dao.TeamDAO;
import com.thepitch.service.DataSyncService;
import com.thepitch.service.EloCalculator;
import com.thepitch.model.Match;
import com.thepitch.model.Team;
import com.thepitch.model.TeamProfile;
import com.thepitch.model.RecentForm;
import com.thepitch.model.ExternalFactors;
import com.thepitch.model.Prediction;
import com.thepitch.analyzer.MatchContextAnalyzer;
import com.thepitch.analyzer.TeamProfileAnalyzer;
import com.thepitch.analyzer.RecentFormAnalyzer;
import com.thepitch.analyzer.EnhancedPredictionEngine;

import java.util.*;
import java.text.SimpleDateFormat;

public class Main {
    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("         THEPITCH - PREMIER LEAGUE        ");
        System.out.println("========================================\n");
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        MatchDAO matchDAO = new MatchDAO();
        TeamDAO teamDAO = new TeamDAO();
        
        System.out.println("📍 " + syncService.getCurrentISTTime());
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n📥 OPTIONS:");
        System.out.println("   1. Sync Premier League data from API");
        System.out.println("   2. Show LAST 5 MATCHWEEKS (with scores)");
        System.out.println("   3. Show UPCOMING MATCHES");
        System.out.println("   4. Show Statistics");
        System.out.println("   5. Generate ENHANCED PREDICTION (with stats & analysis)");
        System.out.println("   6. Show TEAM PROFILE (Home/Away Stats)");
        System.out.print("\nChoose option (1-6): ");
        
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
        
        if (choice == 1) {
            syncService.syncPremierLeague();
            
        } else if (choice == 2) {
            syncService.printStats();
            syncService.showRecentMatches(5);
            
        } else if (choice == 3) {
            syncService.showUpcomingMatches();
            
        } else if (choice == 4) {
            syncService.printStats();
            
        } else if (choice == 5) {
            generateEnhancedPrediction(syncService, matchDAO, teamDAO, scanner);
            
        } else if (choice == 6) {
            showTeamProfile(syncService, matchDAO, teamDAO, scanner);
            
        } else {
            System.out.println("❌ Invalid option!");
        }
        
        System.out.println("\n✅ ThePitch is ready!");
        System.out.println("========================================\n");
        
        scanner.close();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
    
    private static void generateEnhancedPrediction(DataSyncService syncService, 
                                                    MatchDAO matchDAO, 
                                                    TeamDAO teamDAO, 
                                                    Scanner scanner) {
        System.out.println("\n🔮 ENHANCED PREDICTION");
        System.out.println("=".repeat(60));
        
        // Get upcoming matches
        List<Match> upcomingMatches = syncService.getUpcomingMatches();
        
        if (upcomingMatches.isEmpty()) {
            System.out.println("No upcoming matches found. Please sync data first.");
            return;
        }
        
        // Display upcoming matches
        System.out.println("\n📋 UPCOMING MATCHES:");
        for (int i = 0; i < upcomingMatches.size(); i++) {
            Match m = upcomingMatches.get(i);
            System.out.printf("   %d. %s vs %s (%s)%n", 
                i + 1, 
                m.getHomeTeam().getTeamName(),
                m.getAwayTeam().getTeamName(),
                new SimpleDateFormat("EEE, MMM dd").format(m.getMatchDate()));
        }
        
        System.out.print("\nSelect match number to predict (1-" + upcomingMatches.size() + "): ");
        int matchChoice = scanner.nextInt();
        scanner.nextLine();
        
        if (matchChoice < 1 || matchChoice > upcomingMatches.size()) {
            System.out.println("Invalid selection!");
            return;
        }
        
        Match selectedMatch = upcomingMatches.get(matchChoice - 1);
        
        // Get full team data with ELO ratings
        Team homeTeam = teamDAO.getTeamById(selectedMatch.getHomeTeam().getTeamId());
        Team awayTeam = teamDAO.getTeamById(selectedMatch.getAwayTeam().getTeamId());
        
        if (homeTeam == null || awayTeam == null) {
            System.out.println("Team data not found. Please sync data first.");
            return;
        }
        
        selectedMatch.setHomeTeam(homeTeam);
        selectedMatch.setAwayTeam(awayTeam);
        
        // Get home and away matches for both teams
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> homeTeamHomeMatches = new ArrayList<>();
        List<Match> homeTeamAwayMatches = new ArrayList<>();
        List<Match> awayTeamHomeMatches = new ArrayList<>();
        List<Match> awayTeamAwayMatches = new ArrayList<>();
        List<Match> homeTeamLast5 = new ArrayList<>();
        List<Match> awayTeamLast5 = new ArrayList<>();
        
        for (Match m : allMatches) {
            if (m.getHomeTeam().getTeamId() == homeTeam.getTeamId()) {
                homeTeamHomeMatches.add(m);
                if (homeTeamLast5.size() < 5 && m.isFinished()) {
                    homeTeamLast5.add(m);
                }
            } else if (m.getAwayTeam().getTeamId() == homeTeam.getTeamId()) {
                homeTeamAwayMatches.add(m);
                if (homeTeamLast5.size() < 5 && m.isFinished()) {
                    homeTeamLast5.add(m);
                }
            }
            
            if (m.getHomeTeam().getTeamId() == awayTeam.getTeamId()) {
                awayTeamHomeMatches.add(m);
                if (awayTeamLast5.size() < 5 && m.isFinished()) {
                    awayTeamLast5.add(m);
                }
            } else if (m.getAwayTeam().getTeamId() == awayTeam.getTeamId()) {
                awayTeamAwayMatches.add(m);
                if (awayTeamLast5.size() < 5 && m.isFinished()) {
                    awayTeamLast5.add(m);
                }
            }
        }
        
        // Collect external factors (manual input)
        ExternalFactors factors = new ExternalFactors();
        
        System.out.println("\n📋 EXTERNAL FACTORS (Optional - press Enter to skip)");
        System.out.print("   Home team injuries (comma separated, e.g., Saka,Odegaard): ");
        String injuries = scanner.nextLine();
        if (!injuries.isEmpty()) {
            factors.homeTeamInjuries.addAll(Arrays.asList(injuries.split(",")));
        }
        
        System.out.print("   Away team injuries: ");
        injuries = scanner.nextLine();
        if (!injuries.isEmpty()) {
            factors.awayTeamInjuries.addAll(Arrays.asList(injuries.split(",")));
        }
        
        System.out.print("   Weather condition (Sunny/Rain/Snow/Wind): ");
        factors.weatherCondition = scanner.nextLine();
        if (factors.weatherCondition.isEmpty()) factors.weatherCondition = "Sunny";
        
        System.out.print("   Days since last match: ");
        String daysInput = scanner.nextLine();
        factors.daysSinceLastMatch = daysInput.isEmpty() ? 7 : Integer.parseInt(daysInput);
        
        // Initialize analyzers
        MatchContextAnalyzer contextAnalyzer = new MatchContextAnalyzer();
        TeamProfileAnalyzer profileAnalyzer = new TeamProfileAnalyzer();
        RecentFormAnalyzer formAnalyzer = new RecentFormAnalyzer();
        EnhancedPredictionEngine predictionEngine = new EnhancedPredictionEngine();
        EloCalculator eloCalculator = new EloCalculator();
        
        // Analyze match context
        String stakes = contextAnalyzer.determineMatchStakes(homeTeam, awayTeam, allMatches);
        double stakesWeight = contextAnalyzer.calculateStakesWeight(stakes);
        
        // Analyze team profiles
        TeamProfile homeProfile = profileAnalyzer.analyzeHomeProfile(homeTeam, homeTeamHomeMatches);
        TeamProfile awayProfile = profileAnalyzer.analyzeAwayProfile(awayTeam, awayTeamAwayMatches);
        
        // Analyze recent form
        RecentForm homeForm = formAnalyzer.analyzeLast5Games(homeTeam, homeTeamLast5);
        RecentForm awayForm = formAnalyzer.analyzeLast5Games(awayTeam, awayTeamLast5);
        
        // Generate prediction
        Prediction prediction = predictionEngine.generatePrediction(
            selectedMatch, stakes, stakesWeight,
            homeProfile, awayProfile,
            homeForm, awayForm,
            factors, eloCalculator
        );
        
        // Display enhanced prediction
        displayEnhancedPrediction(selectedMatch, stakes, homeProfile, awayProfile, 
                                   homeForm, awayForm, factors, prediction);
    }
    
    private static void showTeamProfile(DataSyncService syncService, 
                                         MatchDAO matchDAO, 
                                         TeamDAO teamDAO, 
                                         Scanner scanner) {
        System.out.println("\n📊 TEAM PROFILE ANALYZER");
        System.out.println("=".repeat(60));
        
        List<Team> allTeams = teamDAO.getAllTeams();
        
        System.out.println("\n📋 TEAMS:");
        for (int i = 0; i < allTeams.size(); i++) {
            System.out.printf("   %d. %s%n", i + 1, allTeams.get(i).getTeamName());
        }
        
        System.out.print("\nSelect team number (1-" + allTeams.size() + "): ");
        int teamChoice = scanner.nextInt();
        scanner.nextLine();
        
        if (teamChoice < 1 || teamChoice > allTeams.size()) {
            System.out.println("Invalid selection!");
            return;
        }
        
        Team selectedTeam = allTeams.get(teamChoice - 1);
        
        // Get home and away matches
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> homeMatches = new ArrayList<>();
        List<Match> awayMatches = new ArrayList<>();
        List<Match> last5Matches = new ArrayList<>();
        
        for (Match m : allMatches) {
            if (m.getHomeTeam().getTeamId() == selectedTeam.getTeamId()) {
                homeMatches.add(m);
                if (last5Matches.size() < 5 && m.isFinished()) {
                    last5Matches.add(m);
                }
            } else if (m.getAwayTeam().getTeamId() == selectedTeam.getTeamId()) {
                awayMatches.add(m);
                if (last5Matches.size() < 5 && m.isFinished()) {
                    last5Matches.add(m);
                }
            }
        }
        
        TeamProfileAnalyzer profileAnalyzer = new TeamProfileAnalyzer();
        RecentFormAnalyzer formAnalyzer = new RecentFormAnalyzer();
        
        TeamProfile homeProfile = profileAnalyzer.analyzeHomeProfile(selectedTeam, homeMatches);
        TeamProfile awayProfile = profileAnalyzer.analyzeAwayProfile(selectedTeam, awayMatches);
        RecentForm recentForm = formAnalyzer.analyzeLast5Games(selectedTeam, last5Matches);
        
        displayTeamProfile(selectedTeam, homeProfile, awayProfile, recentForm);
    }
    
    private static void displayEnhancedPrediction(Match match, String stakes,
                                                   TeamProfile homeProfile, TeamProfile awayProfile,
                                                   RecentForm homeForm, RecentForm awayForm,
                                                   ExternalFactors factors, Prediction prediction) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("                    MATCH PREDICTION: " + 
                           match.getHomeTeam().getTeamName() + " vs " + match.getAwayTeam().getTeamName());
        System.out.println("═".repeat(70));
        
        System.out.println("\n📊 MATCH CONTEXT: " + stakes);
        
        System.out.println("\n" + "═".repeat(70));
        System.out.println("🏠 " + match.getHomeTeam().getTeamName().toUpperCase() + " - HOME PROFILE");
        System.out.println("─".repeat(70));
        System.out.println(homeProfile);
        
        if (!homeProfile.streakAlerts.isEmpty()) {
            System.out.println("🔥 STREAK: " + homeProfile.streakAlerts.get(0));
        }
        
        System.out.println("\n" + "═".repeat(70));
        System.out.println("✈️ " + match.getAwayTeam().getTeamName().toUpperCase() + " - AWAY PROFILE");
        System.out.println("─".repeat(70));
        System.out.println(awayProfile);
        
        if (!awayProfile.streakAlerts.isEmpty()) {
            System.out.println("🔥 STREAK: " + awayProfile.streakAlerts.get(0));
        }
        
        System.out.println("\n" + "═".repeat(70));
        System.out.println("📈 RECENT FORM (LAST 5 GAMES)");
        System.out.println("─".repeat(70));
        System.out.printf("🏠 %s: %s (%.0f%%) %s%n", 
            homeForm.teamName, homeForm.formString, homeForm.formPercentage, homeForm.trend);
        System.out.printf("✈️ %s: %s (%.0f%%) %s%n", 
            awayForm.teamName, awayForm.formString, awayForm.formPercentage, awayForm.trend);
        
        System.out.println("\n" + "═".repeat(70));
        System.out.println("⚠️ EXTERNAL FACTORS");
        System.out.println("─".repeat(70));
        if (!factors.homeTeamInjuries.isEmpty()) {
            System.out.println("   Injuries (Home): " + String.join(", ", factors.homeTeamInjuries));
        }
        if (!factors.awayTeamInjuries.isEmpty()) {
            System.out.println("   Injuries (Away): " + String.join(", ", factors.awayTeamInjuries));
        }
        System.out.println("   Weather: " + factors.weatherCondition);
        System.out.println("   Days since last match: " + factors.daysSinceLastMatch);
        
        System.out.println("\n" + "═".repeat(70));
        System.out.println("🔮 PREDICTION");
        System.out.println("─".repeat(70));
        System.out.printf("┌%s┐%n", "─".repeat(68));
        System.out.printf("│  HOME WIN    │     DRAW     │    AWAY WIN    │%n");
        System.out.printf("│    %.1f%%     │    %.1f%%     │     %.1f%%     │%n", 
            prediction.getHomeWinProb() * 100, 
            prediction.getDrawProb() * 100, 
            prediction.getAwayWinProb() * 100);
        System.out.printf("└%s┘%n", "─".repeat(68));
        
        System.out.println("\n📋 KEY INSIGHTS:");
        if (prediction.getInsights() != null) {
            for (String insight : prediction.getInsights()) {
                System.out.println("   • " + insight);
            }
        }
        
        System.out.printf("%n✅ CONFIDENCE: %s (%.0f%%)%n", 
            prediction.getConfidence(), prediction.getMaxProbability() * 100);
        System.out.println("═".repeat(70));
    }
    
    private static void displayTeamProfile(Team team, TeamProfile homeProfile, 
                                            TeamProfile awayProfile, RecentForm recentForm) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("                    TEAM PROFILE: " + team.getTeamName());
        System.out.println("═".repeat(70));
        
        System.out.println("\n📈 RECENT FORM (LAST 5 GAMES)");
        System.out.println("─".repeat(50));
        System.out.printf("   Form: %s (%.0f%%)%n", recentForm.formString, recentForm.formPercentage);
        System.out.printf("   Goals: %d scored, %d conceded%n", recentForm.goalsScored, recentForm.goalsConceded);
        System.out.printf("   Clean sheets: %d | Failed to score: %d%n", recentForm.cleanSheets, recentForm.failedToScore);
        System.out.printf("   BTTS: %d/5 matches%n", recentForm.bttsCount);
        System.out.printf("   Trend: %s | Streak: %s%n", recentForm.trend, recentForm.streak);
        
        System.out.println("\n🏠 HOME RECORD");
        System.out.println("─".repeat(50));
        System.out.printf("   Played: %d matches%n", (int)(homeProfile.goalsScored > 0 ? 
            (homeProfile.goalsScored / (homeProfile.goalsScored > 0 ? 1 : 1)) : 0));
        System.out.printf("   Goals: %.1f scored/game | %.1f conceded/game%n", 
            homeProfile.goalsScored, homeProfile.goalsConceded);
        System.out.printf("   Corners: %.1f/game | Cards: %.1f/game%n", 
            homeProfile.cornersFor, homeProfile.yellowCards);
        System.out.printf("   Shots: %.1f/game | On target: %.1f (%.0f%%)%n", 
            homeProfile.shotsTotal, homeProfile.shotsOnTarget, homeProfile.shotAccuracy);
        System.out.printf("   Clean sheets: %d | BTTS: %d%n", homeProfile.cleanSheets, homeProfile.bttsCount);
        
        System.out.println("\n✈️ AWAY RECORD");
        System.out.println("─".repeat(50));
        System.out.printf("   Played: %d matches%n", (int)(awayProfile.goalsScored > 0 ? 
            (awayProfile.goalsScored / (awayProfile.goalsScored > 0 ? 1 : 1)) : 0));
        System.out.printf("   Goals: %.1f scored/game | %.1f conceded/game%n", 
            awayProfile.goalsScored, awayProfile.goalsConceded);
        System.out.printf("   Corners: %.1f/game | Cards: %.1f/game%n", 
            awayProfile.cornersFor, awayProfile.yellowCards);
        System.out.printf("   Shots: %.1f/game | On target: %.1f (%.0f%%)%n", 
            awayProfile.shotsTotal, awayProfile.shotsOnTarget, awayProfile.shotAccuracy);
        System.out.printf("   Clean sheets: %d | BTTS: %d%n", awayProfile.cleanSheets, awayProfile.bttsCount);
        
        System.out.println("═".repeat(70));
    }
}
