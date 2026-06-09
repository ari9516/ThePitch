package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.dao.MatchDAO;
import com.thepitch.dao.TeamDAO;
import com.thepitch.dao.AdminDAO;
import com.thepitch.service.DataSyncService;
import com.thepitch.service.EloCalculator;
import com.thepitch.service.CSVImporter;
import com.thepitch.service.AdminPanel;
import com.thepitch.service.TacticalAnalyzer;
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

    // ── Season config — update here each season ───────────────────────────────
    static final String CURRENT_SEASON    = "2026-27";
    static final String COMPETITION       = "Premier League";

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("      THEPITCH - PREMIER LEAGUE         ");
        System.out.println("      Season: " + CURRENT_SEASON);
        System.out.println("========================================\n");

        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        MatchDAO matchDAO = new MatchDAO();
        TeamDAO teamDAO = new TeamDAO();

        System.out.println("📍 " + syncService.getCurrentISTTime());

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("\nChoose option (1-11): ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Enter a number 1-11.");
                continue;
            }

            switch (choice) {
                case 1:
                    syncService.syncPremierLeague();
                    recalculateEloAfterSync(matchDAO, teamDAO);
                    break;
                case 2:
                    syncService.printStats();
                    syncService.showRecentMatches(5);
                    break;
                case 3:
                    syncService.showUpcomingMatches();
                    break;
                case 4:
                    syncService.printStats();
                    break;
                case 5:
                    generateEnhancedPrediction(syncService, matchDAO, teamDAO, scanner);
                    break;
                case 6:
                    showTeamProfile(syncService, matchDAO, teamDAO, scanner);
                    break;
                case 7:
                    importStatsCSV();
                    break;
                case 8:
                    runAdminXG(scanner);
                    break;
                case 9:
                    runInjuryManager(scanner);
                    break;
                case 10:
                    archiveSeason(scanner);
                    break;
                case 11:
                    running = false;
                    break;
                default:
                    System.out.println("❌ Invalid option. Choose 1-11.");
            }
        }

        System.out.println("\n✅ ThePitch closed. See you next season!");
        System.out.println("========================================\n");
        scanner.close();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> db.closeConnection()));
    }

    // ── Menu ──────────────────────────────────────────────────────────────────
    private static void printMenu() {
        System.out.println("\n========================================");
        System.out.println("  📥 MAIN MENU — " + CURRENT_SEASON);
        System.out.println("========================================");
        System.out.println("   1.  Sync Premier League data from API");
        System.out.println("   2.  Show last 5 matchweeks (with scores)");
        System.out.println("   3.  Show upcoming matches");
        System.out.println("   4.  Show statistics");
        System.out.println("   5.  Generate ENHANCED PREDICTION (AI tactical analysis)");
        System.out.println("   6.  Show team profile (Home/Away stats)");
        System.out.println("   7.  Import stats CSV (shots/corners/cards/xG)");
        System.out.println("   ─────────────────────────────────────");
        System.out.println("   8.  [ADMIN] Manage xG data");
        System.out.println("   9.  [ADMIN] Manage injury list");
        System.out.println("   10. [ADMIN] Archive season / new season setup");
        System.out.println("   ─────────────────────────────────────");
        System.out.println("   11. Exit");
    }

    // ── Option 1: Auto ELO recalc after sync ─────────────────────────────────
    private static void recalculateEloAfterSync(MatchDAO matchDAO, TeamDAO teamDAO) {
        System.out.println("\n🔄 Recalculating ELO ratings from match results...");
        try {
            EloCalculator eloCalc = new EloCalculator();
            List<Match> allMatches = matchDAO.getAllMatchesSafe();

            // Sort by date ascending
            allMatches.sort((a, b) -> {
                if (a.getMatchDate() == null || b.getMatchDate() == null) return 0;
                return a.getMatchDate().compareTo(b.getMatchDate());
            });

            int updated = 0;
            for (Match m : allMatches) {
                if (!m.isFinished()) continue;
                if (m.getHomeScore() == null || m.getAwayScore() == null) continue;

                Team home = teamDAO.getTeamById(m.getHomeTeam().getTeamId());
                Team away = teamDAO.getTeamById(m.getAwayTeam().getTeamId());
                if (home == null || away == null) continue;

                // Temporarily set scores on match object for updateRatings()
                eloCalc.updateRatings(m);

                // Persist updated ELO to DB
                teamDAO.updateEloRating(home.getTeamId(), home.getEloRating());
                teamDAO.updateEloRating(away.getTeamId(), away.getEloRating());
                updated++;
            }
            System.out.println("   ✅ ELO recalculated from " + updated + " finished matches.");
        } catch (Exception e) {
            System.err.println("   ⚠️  ELO recalc failed: " + e.getMessage());
        }
    }

    // ── Option 5: Enhanced Prediction ────────────────────────────────────────
    private static void generateEnhancedPrediction(DataSyncService syncService,
                                                    MatchDAO matchDAO,
                                                    TeamDAO teamDAO,
                                                    Scanner scanner) {
        System.out.println("\n🔮 ENHANCED PREDICTION — " + CURRENT_SEASON);
        System.out.println("=".repeat(60));

        List<Match> upcomingMatches = syncService.getUpcomingMatches();

        if (upcomingMatches.isEmpty()) {
            System.out.println("No upcoming matches found. Run option 1 to sync first.");
            return;
        }

        System.out.println("\n📋 UPCOMING MATCHES:");
        for (int i = 0; i < upcomingMatches.size(); i++) {
            Match m = upcomingMatches.get(i);
            System.out.printf("   %d. %-25s vs %-25s (%s)%n",
                i + 1,
                m.getHomeTeam().getTeamName(),
                m.getAwayTeam().getTeamName(),
                new SimpleDateFormat("EEE, MMM dd").format(m.getMatchDate()));
        }

        System.out.print("\nSelect match number (1-" + upcomingMatches.size() + "): ");
        int matchChoice;
        try {
            matchChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input."); return;
        }

        if (matchChoice < 1 || matchChoice > upcomingMatches.size()) {
            System.out.println("Invalid selection."); return;
        }

        Match selectedMatch = upcomingMatches.get(matchChoice - 1);
        Team homeTeam = teamDAO.getTeamById(selectedMatch.getHomeTeam().getTeamId());
        Team awayTeam = teamDAO.getTeamById(selectedMatch.getAwayTeam().getTeamId());

        if (homeTeam == null || awayTeam == null) {
            System.out.println("Team data not found. Sync first."); return;
        }

        selectedMatch.setHomeTeam(homeTeam);
        selectedMatch.setAwayTeam(awayTeam);

        // Build match lists
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
                if (homeTeamLast5.size() < 5 && m.isFinished()) homeTeamLast5.add(m);
            } else if (m.getAwayTeam().getTeamId() == homeTeam.getTeamId()) {
                homeTeamAwayMatches.add(m);
                if (homeTeamLast5.size() < 5 && m.isFinished()) homeTeamLast5.add(m);
            }
            if (m.getHomeTeam().getTeamId() == awayTeam.getTeamId()) {
                awayTeamHomeMatches.add(m);
                if (awayTeamLast5.size() < 5 && m.isFinished()) awayTeamLast5.add(m);
            } else if (m.getAwayTeam().getTeamId() == awayTeam.getTeamId()) {
                awayTeamAwayMatches.add(m);
                if (awayTeamLast5.size() < 5 && m.isFinished()) awayTeamLast5.add(m);
            }
        }

        // Analyzers
        MatchContextAnalyzer contextAnalyzer = new MatchContextAnalyzer();
        TeamProfileAnalyzer  profileAnalyzer = new TeamProfileAnalyzer();
        RecentFormAnalyzer   formAnalyzer    = new RecentFormAnalyzer();
        EnhancedPredictionEngine predEngine  = new EnhancedPredictionEngine();
        EloCalculator eloCalc               = new EloCalculator();

        String stakes       = contextAnalyzer.determineMatchStakes(homeTeam, awayTeam, allMatches);
        double stakesWeight = contextAnalyzer.calculateStakesWeight(stakes);

        TeamProfile homeProfile = profileAnalyzer.analyzeHomeProfile(homeTeam, homeTeamHomeMatches);
        TeamProfile awayProfile = profileAnalyzer.analyzeAwayProfile(awayTeam, awayTeamAwayMatches);
        RecentForm  homeForm    = formAnalyzer.analyzeLast5Games(homeTeam, homeTeamLast5);
        RecentForm  awayForm    = formAnalyzer.analyzeLast5Games(awayTeam, awayTeamLast5);

        // ── Injury review (persistent DB) ─────────────────────────────────────
        ExternalFactors factors = new ExternalFactors();
        try {
            AdminPanel admin = new AdminPanel();
            System.out.println("\n📋 INJURY REVIEW");
            System.out.println("─".repeat(60));
            System.out.println("HOME: " + homeTeam.getTeamName());
            List<Map<String, Object>> homeInj = admin.reviewAndUpdateInjuries(homeTeam.getTeamName(), scanner);
            System.out.println("\nAWAY: " + awayTeam.getTeamName());
            List<Map<String, Object>> awayInj = admin.reviewAndUpdateInjuries(awayTeam.getTeamName(), scanner);

            for (Map<String, Object> inj : homeInj) factors.homeTeamInjuries.add((String) inj.get("player_name"));
            for (Map<String, Object> inj : awayInj) factors.awayTeamInjuries.add((String) inj.get("player_name"));
            factors.homeInjuryDetails = homeInj;
            factors.awayInjuryDetails  = awayInj;
        } catch (Exception e) {
            System.err.println("   ⚠️  Injury load failed: " + e.getMessage());
        }

        // ── External factors ──────────────────────────────────────────────────
        System.out.println("\n📋 MATCH CONDITIONS (press Enter to skip each)");
        System.out.print("   Weather (Sunny/Rain/Snow/Wind) [Sunny]: ");
        String w = scanner.nextLine().trim();
        factors.weatherCondition = w.isEmpty() ? "Sunny" : w;

        System.out.print("   Days since last match [7]: ");
        String d = scanner.nextLine().trim();
        factors.daysSinceLastMatch = d.isEmpty() ? 7 : Integer.parseInt(d);

        // ── AI Tactical Analysis ──────────────────────────────────────────────
        System.out.print("\n   Run AI tactical analysis? (y/n) [y]: ");
        String runAI = scanner.nextLine().trim().toLowerCase();
        if (!runAI.equals("n")) {
            try {
                TacticalAnalyzer tactical = new TacticalAnalyzer();
                double[] scores = tactical.runTacticalAnalysis(
                    homeTeam, awayTeam, homeProfile, awayProfile,
                    homeForm, awayForm, stakes, scanner
                );
                factors.homeTacticalScore = scores[0];
                factors.awayTacticalScore  = scores[1];
            } catch (Exception e) {
                System.err.println("   ⚠️  Tactical analysis failed: " + e.getMessage());
                factors.homeTacticalScore = 5.0;
                factors.awayTacticalScore  = 5.0;
            }
        } else {
            System.out.print("   Home tactical score (0-10) [5]: ");
            String ts = scanner.nextLine().trim();
            factors.homeTacticalScore = ts.isEmpty() ? 5.0 : Double.parseDouble(ts);
            factors.awayTacticalScore  = 10.0 - factors.homeTacticalScore;
        }

        System.out.print("   Referee home bias score (0-10) [5]: ");
        String rs = scanner.nextLine().trim();
        factors.homeRefereeScore = rs.isEmpty() ? 5.0 : Double.parseDouble(rs);
        factors.awayRefereeScore = 10.0 - factors.homeRefereeScore;

        // ── Generate prediction ───────────────────────────────────────────────
        Prediction prediction = predEngine.generatePrediction(
            selectedMatch, stakes, stakesWeight,
            homeProfile, awayProfile,
            homeForm, awayForm,
            factors, eloCalc
        );

        displayEnhancedPrediction(selectedMatch, stakes, homeProfile, awayProfile,
                                   homeForm, awayForm, factors, prediction);
    }

    // ── Option 6: Team Profile ────────────────────────────────────────────────
    private static void showTeamProfile(DataSyncService syncService,
                                         MatchDAO matchDAO,
                                         TeamDAO teamDAO,
                                         Scanner scanner) {
        System.out.println("\n📊 TEAM PROFILE — " + CURRENT_SEASON);
        System.out.println("=".repeat(60));

        List<Team> allTeams = teamDAO.getAllTeams();
        System.out.println("\n📋 TEAMS:");
        for (int i = 0; i < allTeams.size(); i++) {
            System.out.printf("   %2d. %s%n", i + 1, allTeams.get(i).getTeamName());
        }

        System.out.print("\nSelect team number: ");
        int teamChoice;
        try { teamChoice = Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid."); return; }

        if (teamChoice < 1 || teamChoice > allTeams.size()) {
            System.out.println("Invalid selection."); return;
        }

        Team selectedTeam = allTeams.get(teamChoice - 1);
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> homeMatches = new ArrayList<>();
        List<Match> awayMatches = new ArrayList<>();
        List<Match> last5 = new ArrayList<>();

        for (Match m : allMatches) {
            if (m.getHomeTeam().getTeamId() == selectedTeam.getTeamId()) {
                homeMatches.add(m);
                if (last5.size() < 5 && m.isFinished()) last5.add(m);
            } else if (m.getAwayTeam().getTeamId() == selectedTeam.getTeamId()) {
                awayMatches.add(m);
                if (last5.size() < 5 && m.isFinished()) last5.add(m);
            }
        }

        TeamProfileAnalyzer profileAnalyzer = new TeamProfileAnalyzer();
        RecentFormAnalyzer formAnalyzer = new RecentFormAnalyzer();

        TeamProfile homeProfile = profileAnalyzer.analyzeHomeProfile(selectedTeam, homeMatches);
        TeamProfile awayProfile = profileAnalyzer.analyzeAwayProfile(selectedTeam, awayMatches);
        RecentForm recentForm = formAnalyzer.analyzeLast5Games(selectedTeam, last5);

        displayTeamProfile(selectedTeam, homeProfile, awayProfile, recentForm);
    }

    // ── Option 7: Import CSV ──────────────────────────────────────────────────
    private static void importStatsCSV() {
        System.out.println("\n📥 IMPORT STATS CSV");
        System.out.println("=".repeat(60));
        System.out.println("   Looking for: data/pl_match_stats.csv");
        System.out.println("   If missing, open in browser and download:");
        System.out.println("   https://www.football-data.co.uk/mmz4281/2627/E0.csv");
        System.out.println("   Rename to pl_match_stats_raw.csv → put in data/ folder");
        System.out.println("   Then run: python scripts/fetch_fbref_stats.py");
        System.out.println();

        String dataDir = System.getProperty("user.dir") + java.io.File.separator + "data";
        try {
            CSVImporter importer = new CSVImporter();
            importer.importAll(dataDir);
            System.out.println("\n✅ Import done. Run option 5 or 6 for real stats.");
        } catch (Exception e) {
            System.err.println("❌ Import failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Option 8: Admin xG ───────────────────────────────────────────────────
    private static void runAdminXG(Scanner scanner) {
        try {
            new AdminPanel().runXGAdmin(scanner);
        } catch (Exception e) {
            System.err.println("❌ xG admin error: " + e.getMessage());
        }
    }

    // ── Option 9: Injury Manager ──────────────────────────────────────────────
    private static void runInjuryManager(Scanner scanner) {
        try {
            new AdminPanel().runInjuryManager(scanner);
        } catch (Exception e) {
            System.err.println("❌ Injury manager error: " + e.getMessage());
        }
    }

    // ── Option 10: Season Archive + New Season Setup ──────────────────────────
    private static void archiveSeason(Scanner scanner) {
        System.out.println("\n📦 SEASON ARCHIVE & NEW SEASON SETUP");
        System.out.println("=".repeat(60));
        System.out.println("  This will:");
        System.out.println("  1. Archive current match data to matches_2025_26 table");
        System.out.println("  2. Archive match_stats to match_stats_2025_26 table");
        System.out.println("  3. Clear matches + match_stats tables for new season");
        System.out.println("  4. Reset all team ELO ratings to 1500");
        System.out.println("  5. Clear tactical_analysis table (fixture-specific)");
        System.out.println("  NOTE: injury_list and team_xg are KEPT (you manage manually)");
        System.out.println();
        System.out.print("  Type YES to confirm archive (or press Enter to cancel): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equals("YES")) {
            System.out.println("  Cancelled.");
            return;
        }

        try {
            java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
            java.sql.Statement stmt = conn.createStatement();

            // 1. Archive matches
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS matches_2025_26 AS " +
                "SELECT * FROM matches"
            );
            System.out.println("  ✅ matches archived to matches_2025_26");

            // 2. Archive match_stats
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS match_stats_2025_26 AS " +
                "SELECT * FROM match_stats"
            );
            System.out.println("  ✅ match_stats archived to match_stats_2025_26");

            // 3. Clear current tables
            stmt.execute("DELETE FROM matches");
            stmt.execute("DELETE FROM match_stats");
            System.out.println("  ✅ matches and match_stats cleared for 2026-27");

            // 4. Reset ELO
            stmt.execute("UPDATE teams SET elo_rating = 1500");
            System.out.println("  ✅ ELO ratings reset to 1500");

            // 5. Clear tactical analysis (fixture-specific, not reusable)
            stmt.execute("DELETE FROM tactical_analysis");
            System.out.println("  ✅ Tactical analysis cache cleared");

            System.out.println();
            System.out.println("  ✅ SEASON ARCHIVE COMPLETE");
            System.out.println("  Run option 1 to sync 2026-27 season data.");
            System.out.println("  Update CSV URL in fetch_fbref_stats.py:");
            System.out.println("  Change 2526 → 2627 in the football-data.co.uk URL");

        } catch (Exception e) {
            System.err.println("  ❌ Archive failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Display: Enhanced Prediction ─────────────────────────────────────────
    private static void displayEnhancedPrediction(Match match, String stakes,
                                                   TeamProfile homeProfile, TeamProfile awayProfile,
                                                   RecentForm homeForm, RecentForm awayForm,
                                                   ExternalFactors factors, Prediction prediction) {
        System.out.println("\n" + "═".repeat(70));
        System.out.printf("  MATCH PREDICTION: %s vs %s%n",
            match.getHomeTeam().getTeamName(), match.getAwayTeam().getTeamName());
        System.out.println("═".repeat(70));

        System.out.println("\n📊 MATCH CONTEXT: " + stakes);

        System.out.println("\n🏠 " + match.getHomeTeam().getTeamName().toUpperCase() + " — HOME PROFILE");
        System.out.println("─".repeat(70));
        System.out.println(homeProfile);
        if (!homeProfile.streakAlerts.isEmpty())
            System.out.println("🔥 " + homeProfile.streakAlerts.get(0));

        System.out.println("\n✈️  " + match.getAwayTeam().getTeamName().toUpperCase() + " — AWAY PROFILE");
        System.out.println("─".repeat(70));
        System.out.println(awayProfile);
        if (!awayProfile.streakAlerts.isEmpty())
            System.out.println("🔥 " + awayProfile.streakAlerts.get(0));

        System.out.println("\n📈 RECENT FORM (LAST 5)");
        System.out.println("─".repeat(70));
        System.out.printf("🏠 %s: %s (%.0f%%) %s%n",
            homeForm.teamName, homeForm.formString, homeForm.formPercentage, homeForm.trend);
        System.out.printf("✈️  %s: %s (%.0f%%) %s%n",
            awayForm.teamName, awayForm.formString, awayForm.formPercentage, awayForm.trend);

        System.out.println("\n⚠️  MATCH CONDITIONS");
        System.out.println("─".repeat(70));
        if (!factors.homeTeamInjuries.isEmpty())
            System.out.println("   Injuries (Home): " + String.join(", ", factors.homeTeamInjuries));
        if (!factors.awayTeamInjuries.isEmpty())
            System.out.println("   Injuries (Away): " + String.join(", ", factors.awayTeamInjuries));
        System.out.println("   Weather: " + factors.weatherCondition);
        System.out.println("   Days rest: " + factors.daysSinceLastMatch);
        System.out.printf("   Tactical: Home=%.1f  Away=%.1f%n",
            factors.homeTacticalScore, factors.awayTacticalScore);
        System.out.printf("   Referee bias: Home=%.1f  Away=%.1f%n",
            factors.homeRefereeScore, factors.awayRefereeScore);

        System.out.println("\n🔮 PREDICTION");
        System.out.println("─".repeat(70));
        System.out.printf("┌%s┐%n", "─".repeat(68));
        System.out.printf("│   HOME WIN        DRAW        AWAY WIN          │%n");
        System.out.printf("│    %5.1f%%        %5.1f%%       %5.1f%%           │%n",
            prediction.getHomeWinProb() * 100,
            prediction.getDrawProb() * 100,
            prediction.getAwayWinProb() * 100);
        System.out.printf("└%s┘%n", "─".repeat(68));

        if (prediction.getInsights() != null && !prediction.getInsights().isEmpty()) {
            System.out.println("\n📋 KEY INSIGHTS:");
            for (String insight : prediction.getInsights())
                System.out.println("   • " + insight);
        }

        System.out.printf("%n✅ CONFIDENCE: %s (%.0f%%)%n",
            prediction.getConfidence(), prediction.getMaxProbability() * 100);
        System.out.println("═".repeat(70));
    }

    // ── Display: Team Profile ─────────────────────────────────────────────────
    private static void displayTeamProfile(Team team, TeamProfile homeProfile,
                                            TeamProfile awayProfile, RecentForm recentForm) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  TEAM PROFILE: " + team.getTeamName());
        System.out.printf("  ELO Rating: %.0f%n", team.getEloRating());
        System.out.println("═".repeat(70));

        System.out.println("\n📈 RECENT FORM (LAST 5)");
        System.out.println("─".repeat(50));
        System.out.printf("   Form:         %s (%.0f%%)%n", recentForm.formString, recentForm.formPercentage);
        System.out.printf("   Goals:        %d scored, %d conceded%n", recentForm.goalsScored, recentForm.goalsConceded);
        System.out.printf("   Clean sheets: %d | Failed to score: %d%n", recentForm.cleanSheets, recentForm.failedToScore);
        System.out.printf("   BTTS:         %d/5 matches%n", recentForm.bttsCount);
        System.out.printf("   Trend:        %s | Streak: %s%n", recentForm.trend, recentForm.streak);

        System.out.println("\n🏠 HOME RECORD");
        System.out.println("─".repeat(50));
        System.out.printf("   Source:       %s%n", homeProfile.dataSource);
        System.out.printf("   Goals:        %.2f scored | %.2f conceded per game%n", homeProfile.goalsScored, homeProfile.goalsConceded);
        System.out.printf("   Shots:        %.1f/game | On target: %.1f (%.0f%%)%n", homeProfile.shotsTotal, homeProfile.shotsOnTarget, homeProfile.shotAccuracy);
        System.out.printf("   xG:           %.2f/game%n", homeProfile.xG);
        System.out.printf("   Corners:      %.1f/game | Yellow cards: %.1f/game%n", homeProfile.cornersFor, homeProfile.yellowCards);
        System.out.printf("   Clean sheets: %d | BTTS: %d%n", homeProfile.cleanSheets, homeProfile.bttsCount);

        System.out.println("\n✈️  AWAY RECORD");
        System.out.println("─".repeat(50));
        System.out.printf("   Source:       %s%n", awayProfile.dataSource);
        System.out.printf("   Goals:        %.2f scored | %.2f conceded per game%n", awayProfile.goalsScored, awayProfile.goalsConceded);
        System.out.printf("   Shots:        %.1f/game | On target: %.1f (%.0f%%)%n", awayProfile.shotsTotal, awayProfile.shotsOnTarget, awayProfile.shotAccuracy);
        System.out.printf("   xG:           %.2f/game%n", awayProfile.xG);
        System.out.printf("   Corners:      %.1f/game | Yellow cards: %.1f/game%n", awayProfile.cornersFor, awayProfile.yellowCards);
        System.out.printf("   Clean sheets: %d | BTTS: %d%n", awayProfile.cleanSheets, awayProfile.bttsCount);

        System.out.println("═".repeat(70));
    }
}