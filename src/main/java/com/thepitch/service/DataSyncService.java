package com.thepitch.service;

import com.google.gson.JsonObject;
import com.thepitch.dao.TeamDAO;
import com.thepitch.dao.MatchDAO;
import com.thepitch.model.Team;
import com.thepitch.model.Match;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * DataSyncService - Synchronizes real data from API to local database
 * 
 * @author ThePitch Team
 * @version 2.0
 */
public class DataSyncService {
    
    private APIClient apiClient;
    private TeamDAO teamDAO;
    private MatchDAO matchDAO;
    
    // League ID to competition code mapping
    private static final Map<Integer, String> LEAGUE_MAP = new LinkedHashMap<>();
    
    static {
        LEAGUE_MAP.put(2021, "PL");   // Premier League (England)
        LEAGUE_MAP.put(2014, "PD");   // La Liga (Spain)
        LEAGUE_MAP.put(2019, "SA");   // Serie A (Italy)
        LEAGUE_MAP.put(2002, "BL1");  // Bundesliga (Germany)
        LEAGUE_MAP.put(2015, "FL1");  // Ligue 1 (France)
    }
    
    // League names for display
    private static final Map<Integer, String> LEAGUE_NAMES = new HashMap<>();
    
    static {
        LEAGUE_NAMES.put(2021, "Premier League");
        LEAGUE_NAMES.put(2014, "La Liga");
        LEAGUE_NAMES.put(2019, "Serie A");
        LEAGUE_NAMES.put(2002, "Bundesliga");
        LEAGUE_NAMES.put(2015, "Ligue 1");
    }
    
    // Competition code to league ID mapping
    private static final Map<String, Integer> CODE_TO_LEAGUE = new HashMap<>();
    
    static {
        CODE_TO_LEAGUE.put("PL", 2021);
        CODE_TO_LEAGUE.put("PD", 2014);
        CODE_TO_LEAGUE.put("SA", 2019);
        CODE_TO_LEAGUE.put("BL1", 2002);
        CODE_TO_LEAGUE.put("FL1", 2015);
    }
    
    // Date formatter for IST
    private SimpleDateFormat istDateFormat;
    private SimpleDateFormat istDisplayFormat;
    
    public DataSyncService() {
        this.apiClient = new APIClient();
        this.teamDAO = new TeamDAO();
        this.matchDAO = new MatchDAO();
        
        // Setup IST timezone for display
        TimeZone istTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
        this.istDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.istDateFormat.setTimeZone(istTimeZone);
        
        this.istDisplayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a");
        this.istDisplayFormat.setTimeZone(istTimeZone);
    }
    
    /**
     * Sync all configured leagues
     */
    public void syncAllLeagues() {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("🔄 STARTING DATA SYNC FROM FOOTBALL-DATA.ORG");
        System.out.println("=".repeat(55));
        
        int totalTeams = 0;
        int totalMatches = 0;
        int successfulLeagues = 0;
        
        for (Map.Entry<Integer, String> entry : LEAGUE_MAP.entrySet()) {
            int leagueId = entry.getKey();
            String competitionCode = entry.getValue();
            String leagueName = LEAGUE_NAMES.get(leagueId);
            
            System.out.println("\n📊 " + leagueName + " (" + competitionCode + ")");
            System.out.println("   ───────────────────────────────────────");
            
            SyncResult result = syncLeague(leagueId, competitionCode);
            if (result.success) {
                successfulLeagues++;
                totalTeams += result.teamsCount;
                totalMatches += result.matchesCount;
                System.out.println("   ✅ Synced: " + result.teamsCount + " teams, " + result.matchesCount + " matches");
            } else {
                System.out.println("   ❌ Failed: " + result.errorMessage);
            }
        }
        
        System.out.println("\n" + "=".repeat(55));
        System.out.println("✅ DATA SYNC COMPLETE!");
        System.out.println("   📋 Total: " + successfulLeagues + "/5 leagues synced");
        System.out.println("   🏆 Teams: " + totalTeams);
        System.out.println("   ⚽ Matches: " + totalMatches);
        System.out.println("=".repeat(55));
    }
    
    /**
     * Sync a single league
     */
    private SyncResult syncLeague(int leagueId, String competitionCode) {
        SyncResult result = new SyncResult();
        
        try {
            // Step 1: Fetch and save teams
            System.out.print("   ├─ Fetching teams... ");
            JsonObject teamsResponse = apiClient.fetchCompetitionTeams(competitionCode);
            List<Team> teams = apiClient.parseTeams(teamsResponse, leagueId);
            
            int teamCount = 0;
            for (Team team : teams) {
                teamDAO.saveTeam(team);
                teamCount++;
            }
            result.teamsCount = teamCount;
            System.out.println("✅ " + teamCount + " teams saved");
            
            // Small delay to respect rate limits
            TimeUnit.MILLISECONDS.sleep(500);
            
            // Step 2: Fetch and save matches
            System.out.print("   ├─ Fetching matches... ");
            JsonObject matchesResponse = apiClient.fetchCompetitionMatches(competitionCode);
            List<Match> matches = apiClient.parseMatches(matchesResponse, leagueId);
            
            int matchCount = 0;
            int skippedCount = 0;
            for (Match match : matches) {
                // Ensure teams exist in database before saving match
                Team homeTeam = teamDAO.getTeamById(match.getHomeTeam().getTeamId());
                Team awayTeam = teamDAO.getTeamById(match.getAwayTeam().getTeamId());
                
                if (homeTeam != null && awayTeam != null) {
                    match.setHomeTeam(homeTeam);
                    match.setAwayTeam(awayTeam);
                    matchDAO.saveMatch(match);
                    matchCount++;
                } else {
                    skippedCount++;
                }
            }
            result.matchesCount = matchCount;
            System.out.println("✅ " + matchCount + " matches saved" + 
                (skippedCount > 0 ? " (" + skippedCount + " skipped due to missing teams)" : ""));
            
            result.success = true;
            
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            System.err.println("   Error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sync only a specific league by ID
     */
    public boolean syncLeagueById(int leagueId) {
        String competitionCode = LEAGUE_MAP.get(leagueId);
        if (competitionCode == null) {
            System.err.println("Invalid league ID: " + leagueId);
            return false;
        }
        
        String leagueName = LEAGUE_NAMES.get(leagueId);
        System.out.println("\n📊 Syncing " + leagueName + "...");
        
        SyncResult result = syncLeague(leagueId, competitionCode);
        return result.success;
    }
    
    /**
     * Sync only a specific league by competition code
     */
    public boolean syncLeagueByCode(String competitionCode) {
        Integer leagueId = CODE_TO_LEAGUE.get(competitionCode.toUpperCase());
        if (leagueId == null) {
            System.err.println("Invalid competition code: " + competitionCode);
            System.out.println("Valid codes: PL, PD, SA, BL1, FL1");
            return false;
        }
        return syncLeagueById(leagueId);
    }
    
    /**
     * Get today's matches (based on IST timezone) - FIXED to use safe method
     */
    public List<Match> getTodayMatches() {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> todayMatches = new ArrayList<>();
        
        String todayIST = istDateFormat.format(new Date());
        
        for (Match match : allMatches) {
            if (match.getMatchDate() != null) {
                String matchDateIST = istDateFormat.format(match.getMatchDate());
                if (matchDateIST.equals(todayIST) && match.isScheduled()) {
                    todayMatches.add(match);
                }
            }
        }
        
        // Sort by match time
        todayMatches.sort(Comparator.comparing(Match::getMatchDate));
        return todayMatches;
    }
    
    /**
     * Get tomorrow's matches
     */
    public List<Match> getTomorrowMatches() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return getMatchesByDate(cal.getTime());
    }
    
    /**
     * Get matches for a specific date
     */
    public List<Match> getMatchesByDate(Date date) {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> matches = new ArrayList<>();
        
        String targetDateIST = istDateFormat.format(date);
        
        for (Match match : allMatches) {
            if (match.getMatchDate() != null) {
                String matchDateIST = istDateFormat.format(match.getMatchDate());
                if (matchDateIST.equals(targetDateIST) && match.isScheduled()) {
                    matches.add(match);
                }
            }
        }
        
        matches.sort(Comparator.comparing(Match::getMatchDate));
        return matches;
    }
    
    /**
     * Get matches for the next N days
     */
    public List<Match> getMatchesForNextDays(int days) {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> upcomingMatches = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, days);
        Date endDate = cal.getTime();
        
        String todayIST = istDateFormat.format(new Date());
        String endDateIST = istDateFormat.format(endDate);
        
        for (Match match : allMatches) {
            if (match.getMatchDate() != null) {
                String matchDateIST = istDateFormat.format(match.getMatchDate());
                if (matchDateIST.compareTo(todayIST) >= 0 && 
                    matchDateIST.compareTo(endDateIST) <= 0 && 
                    match.isScheduled()) {
                    upcomingMatches.add(match);
                }
            }
        }
        
        upcomingMatches.sort(Comparator.comparing(Match::getMatchDate));
        return upcomingMatches;
    }
    
    /**
     * Print upcoming matches in a formatted way
     */
    public void printUpcomingMatches(int days) {
        List<Match> matches = getMatchesForNextDays(days);
        
        if (matches.isEmpty()) {
            System.out.println("\n   📅 No matches found in the next " + days + " days");
            return;
        }
        
        System.out.println("\n   📅 UPCOMING MATCHES (" + days + " days)");
        System.out.println("   ─────────────────────────────────────────────────");
        
        String currentDate = "";
        for (Match match : matches) {
            String matchDateIST = istDisplayFormat.format(match.getMatchDate());
            
            // Print date header only when date changes
            String dateOnly = matchDateIST.split(" - ")[0];
            if (!dateOnly.equals(currentDate)) {
                currentDate = dateOnly;
                System.out.println("\n   📍 " + currentDate);
                System.out.println("   ─────────────────────────────────────────────");
            }
            
            System.out.printf("      • %-22s vs %-22s%n", 
                truncateString(match.getHomeTeam().getTeamName(), 22),
                truncateString(match.getAwayTeam().getTeamName(), 22));
        }
        System.out.println();
    }
    
    /**
     * Print today's matches in a formatted way
     */
    public void printTodayMatches() {
        List<Match> matches = getTodayMatches();
        
        if (matches.isEmpty()) {
            System.out.println("\n   ⚽ No matches scheduled for today (IST)");
            System.out.println("   Check tomorrow's matches with: printUpcomingMatches(1)");
            return;
        }
        
        System.out.println("\n   ⚽ TODAY'S MATCHES (" + istDisplayFormat.format(new Date()).split(" - ")[0] + ")");
        System.out.println("   ─────────────────────────────────────────────────");
        
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            String time = istDisplayFormat.format(match.getMatchDate()).split(" - ")[1];
            System.out.printf("   %2d. %-22s vs %-22s [%s]%n", 
                i + 1,
                truncateString(match.getHomeTeam().getTeamName(), 22),
                truncateString(match.getAwayTeam().getTeamName(), 22),
                time);
        }
        System.out.println();
    }
    
    /**
     * Get upcoming matches for the next 7 days (original method)
     */
    public List<Match> getUpcomingMatches() {
        return getMatchesForNextDays(7);
    }
    
    /**
     * Get matches by league
     */
    public List<Match> getMatchesByLeague(int leagueId) {
        return matchDAO.getMatchesByLeague(leagueId);
    }
    
    /**
     * Get teams by league
     */
    public List<Team> getTeamsByLeague(int leagueId) {
        return teamDAO.getTeamsByLeague(leagueId);
    }
    
    /**
     * Get all teams
     */
    public List<Team> getAllTeams() {
        return teamDAO.getAllTeams();
    }
    
    /**
     * Get match by ID
     */
    public Match getMatchById(int matchId) {
        return matchDAO.getMatchById(matchId);
    }
    
    /**
     * Get team by ID
     */
    public Team getTeamById(int teamId) {
        return teamDAO.getTeamById(teamId);
    }
    
    /**
     * Get match count by league
     */
    public int getMatchCountByLeague(int leagueId) {
        return matchDAO.getMatchCountByLeague(leagueId);
    }
    
    /**
     * Test API connection
     */
    public boolean testAPI() {
        System.out.println("\n🔌 Testing API connection...");
        boolean connected = apiClient.testConnection();
        if (connected) {
            System.out.println("✅ API connection successful!");
            System.out.println("   Your API key is valid and working.");
        } else {
            System.out.println("❌ API connection failed!");
            System.out.println("   Please check:");
            System.out.println("   1. Your API key in config.properties");
            System.out.println("   2. Your internet connection");
            System.out.println("   3. Email verification (check your inbox)");
            System.out.println("\n   Get a free key at: https://www.football-data.org/register");
        }
        return connected;
    }
    
    /**
     * Get sync statistics
     */
    public void printStats() {
        System.out.println("\n📊 DATABASE STATISTICS");
        System.out.println("=".repeat(50));
        
        for (Map.Entry<Integer, String> entry : LEAGUE_MAP.entrySet()) {
            int leagueId = entry.getKey();
            String leagueName = LEAGUE_NAMES.get(leagueId);
            
            int teamCount = teamDAO.getTeamsByLeague(leagueId).size();
            int matchCount = matchDAO.getMatchCountByLeague(leagueId);
            
            System.out.printf("   %-20s: %2d teams, %4d matches%n", leagueName, teamCount, matchCount);
        }
        
        System.out.println("=".repeat(50));
        System.out.println("   Total teams   : " + teamDAO.getTeamCount());
        System.out.println("   Total matches : " + matchDAO.getMatchCount());
        System.out.println("=".repeat(50));
    }
    
    /**
     * Get current IST time
     */
    public String getCurrentISTTime() {
        return istDisplayFormat.format(new Date());
    }
    
    /**
     * Helper method to truncate long strings
     */
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Inner class to hold sync result
     */
    private static class SyncResult {
        boolean success = false;
        int teamsCount = 0;
        int matchesCount = 0;
        String errorMessage = "";
    }
}
