package com.thepitch.service;

import com.google.gson.JsonObject;
import com.thepitch.dao.TeamDAO;
import com.thepitch.dao.MatchDAO;
import com.thepitch.model.Team;
import com.thepitch.model.Match;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DataSyncService {
    
    private APIClient apiClient;
    private TeamDAO teamDAO;
    private MatchDAO matchDAO;
    
    private static final int PREMIER_LEAGUE_ID = 2021;
    private static final String PREMIER_LEAGUE_CODE = "PL";
    
    private SimpleDateFormat istDateFormat;
    private SimpleDateFormat istDisplayFormat;
    private SimpleDateFormat utcDateFormat;
    private SimpleDateFormat timeFormat;
    
    public DataSyncService() {
        this.apiClient = new APIClient();
        this.teamDAO = new TeamDAO();
        this.matchDAO = new MatchDAO();
        
        TimeZone istTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
        this.istDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.istDateFormat.setTimeZone(istTimeZone);
        
        this.istDisplayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        this.istDisplayFormat.setTimeZone(istTimeZone);
        
        this.utcDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        this.timeFormat = new SimpleDateFormat("hh:mm a");
        this.timeFormat.setTimeZone(istTimeZone);
    }
    
    public void syncPremierLeague() {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("🔄 SYNCING PREMIER LEAGUE ONLY");
        System.out.println("=".repeat(55));
        
        try {
            System.out.print("   ├─ Fetching teams... ");
            JsonObject teamsResponse = apiClient.fetchCompetitionTeams(PREMIER_LEAGUE_CODE);
            List<Team> teams = apiClient.parseTeams(teamsResponse, PREMIER_LEAGUE_ID);
            
            int teamCount = 0;
            for (Team team : teams) {
                teamDAO.saveTeam(team);
                teamCount++;
            }
            System.out.println("✅ " + teamCount + " teams saved");
            
            TimeUnit.MILLISECONDS.sleep(500);
            
            System.out.print("   ├─ Fetching matches... ");
            JsonObject matchesResponse = apiClient.fetchCompetitionMatches(PREMIER_LEAGUE_CODE);
            List<Match> matches = apiClient.parseMatches(matchesResponse, PREMIER_LEAGUE_ID);
            
            int matchCount = 0;
            for (Match match : matches) {
                Team homeTeam = teamDAO.getTeamById(match.getHomeTeam().getTeamId());
                Team awayTeam = teamDAO.getTeamById(match.getAwayTeam().getTeamId());
                
                if (homeTeam != null && awayTeam != null) {
                    match.setHomeTeam(homeTeam);
                    match.setAwayTeam(awayTeam);
                    matchDAO.saveMatch(match);
                    matchCount++;
                }
            }
            System.out.println("✅ " + matchCount + " matches saved");
            
            System.out.println("\n✅ PREMIER LEAGUE SYNC COMPLETE!");
            System.out.println("   🏆 Teams: " + teamCount);
            System.out.println("   ⚽ Matches: " + matchCount);
            
        } catch (Exception e) {
            System.err.println("   ❌ Failed: " + e.getMessage());
        }
    }
    
    /**
     * Get upcoming matches (for Main.java prediction feature)
     */
    public List<Match> getUpcomingMatches() {
        return matchDAO.getUpcomingMatches();
    }
    
    /**
     * Get upcoming matches with custom limit
     */
    public List<Match> getUpcomingMatches(int limit) {
        return matchDAO.getUpcomingMatches(limit);
    }
    
    /**
     * Get matches for next N days
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
            if (match.getMatchDate() != null && match.isScheduled()) {
                String matchDateIST = istDateFormat.format(match.getMatchDate());
                if (matchDateIST.compareTo(todayIST) >= 0 && 
                    matchDateIST.compareTo(endDateIST) <= 0) {
                    upcomingMatches.add(match);
                }
            }
        }
        
        upcomingMatches.sort(Comparator.comparing(Match::getMatchDate));
        return upcomingMatches;
    }
    
    /**
     * Get today's matches
     */
    public List<Match> getTodayMatches() {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> todayMatches = new ArrayList<>();
        
        String todayIST = istDateFormat.format(new Date());
        
        for (Match match : allMatches) {
            if (match.getMatchDate() != null && match.isScheduled()) {
                String matchDateIST = istDateFormat.format(match.getMatchDate());
                if (matchDateIST.equals(todayIST)) {
                    todayMatches.add(match);
                }
            }
        }
        
        todayMatches.sort(Comparator.comparing(Match::getMatchDate));
        return todayMatches;
    }
    
    /**
     * Show recent matches with scores (last N matchweeks)
     */
    public void showRecentMatches(int numberOfMatchweeks) {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> finishedMatches = new ArrayList<>();
        
        // Get only finished matches with scores, sorted by date (newest first)
        for (Match match : allMatches) {
            if (match.isFinished() && match.getHomeScore() != null && match.getAwayScore() != null) {
                finishedMatches.add(match);
            }
        }
        
        finishedMatches.sort((a, b) -> b.getMatchDate().compareTo(a.getMatchDate()));
        
        if (finishedMatches.isEmpty()) {
            System.out.println("\n⚠️ No finished matches with scores found!");
            return;
        }
        
        System.out.println("\n" + "=".repeat(75));
        System.out.println("📊 LAST " + numberOfMatchweeks + " MATCHWEEKS - PREMIER LEAGUE RESULTS");
        System.out.println("=".repeat(75));
        
        // Group by actual matchweek (using date as key)
        Map<String, List<Match>> matchesByDate = new LinkedHashMap<>();
        
        for (Match match : finishedMatches) {
            String dateKey = istDateFormat.format(match.getMatchDate());
            matchesByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(match);
        }
        
        // Get the most recent N dates
        List<String> recentDates = new ArrayList<>(matchesByDate.keySet());
        int weeksToShow = Math.min(numberOfMatchweeks, recentDates.size());
        
        for (int i = 0; i < weeksToShow; i++) {
            String dateKey = recentDates.get(i);
            List<Match> matchesOnDate = matchesByDate.get(dateKey);
            
            // Sort matches by time on that date
            matchesOnDate.sort(Comparator.comparing(Match::getMatchDate));
            
            System.out.println("\n📅 " + istDisplayFormat.format(matchesOnDate.get(0).getMatchDate()));
            System.out.println("┌" + "─".repeat(73) + "┐");
            
            for (Match m : matchesOnDate) {
                String homeName = m.getHomeTeam().getTeamName();
                String awayName = m.getAwayTeam().getTeamName();
                int homeScore = m.getHomeScore();
                int awayScore = m.getAwayScore();
                
                // Determine result with proper emoji
                String result;
                if (homeScore > awayScore) {
                    result = "🏠 HOME WIN";
                } else if (awayScore > homeScore) {
                    result = "✈️ AWAY WIN";
                } else {
                    result = "🤝 DRAW";
                }
                
                System.out.printf("│ %-25s %2d - %-2d %-25s │ %-12s │%n", 
                    truncateString(homeName, 25), homeScore, awayScore, 
                    truncateString(awayName, 25), result);
            }
            System.out.println("└" + "─".repeat(73) + "┘");
        }
        
        System.out.println("\n✅ Showing " + finishedMatches.size() + " finished matches from " + weeksToShow + " matchweeks");
    }
    
    /**
     * Show upcoming matches - FIXED with debug output
     */
    public void showUpcomingMatches() {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        List<Match> upcoming = new ArrayList<>();
        
        // Get current date in UTC (matches are stored in UTC)
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 0);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        Date todayUTC = utcCalendar.getTime();
        
        System.out.println("\n📅 Debug: Today's UTC date = " + utcDateFormat.format(todayUTC));
        
        for (Match match : allMatches) {
            // Only include SCHEDULED matches with future dates
            if (match.isScheduled() && match.getMatchDate() != null) {
                // Compare dates (ignoring time)
                Calendar matchCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                matchCal.setTime(match.getMatchDate());
                matchCal.set(Calendar.HOUR_OF_DAY, 0);
                matchCal.set(Calendar.MINUTE, 0);
                matchCal.set(Calendar.SECOND, 0);
                matchCal.set(Calendar.MILLISECOND, 0);
                Date matchDateOnly = matchCal.getTime();
                
                if (matchDateOnly.compareTo(todayUTC) >= 0) {
                    upcoming.add(match);
                }
            }
        }
        
        // Sort by date
        upcoming.sort(Comparator.comparing(Match::getMatchDate));
        
        if (upcoming.isEmpty()) {
            System.out.println("\n📅 No upcoming matches found!");
            System.out.println("   Debug info:");
            System.out.println("   - Total matches in DB: " + allMatches.size());
            
            // Count scheduled matches for debugging
            int scheduledCount = 0;
            for (Match m : allMatches) {
                if (m.isScheduled()) scheduledCount++;
            }
            System.out.println("   - Scheduled matches: " + scheduledCount);
            System.out.println("   - Today's UTC date: " + utcDateFormat.format(todayUTC));
            
            // Show the latest scheduled match date for debugging
            Date latestScheduled = null;
            for (Match m : allMatches) {
                if (m.isScheduled() && (latestScheduled == null || m.getMatchDate().after(latestScheduled))) {
                    latestScheduled = m.getMatchDate();
                }
            }
            if (latestScheduled != null) {
                System.out.println("   - Latest scheduled match: " + utcDateFormat.format(latestScheduled));
                System.out.println("   - Latest scheduled match (IST): " + istDisplayFormat.format(latestScheduled));
            }
            return;
        }
        
        System.out.println("\n" + "=".repeat(75));
        System.out.println("📅 UPCOMING PREMIER LEAGUE FIXTURES");
        System.out.println("=".repeat(75));
        
        String currentDate = "";
        for (Match match : upcoming) {
            String dateStr = istDisplayFormat.format(match.getMatchDate());
            if (!dateStr.equals(currentDate)) {
                currentDate = dateStr;
                System.out.println("\n📍 " + currentDate);
                System.out.println("┌" + "─".repeat(73) + "┐");
            }
            
            String time = timeFormat.format(match.getMatchDate());
            System.out.printf("│ %-32s vs %-32s │ %-10s │%n", 
                truncateString(match.getHomeTeam().getTeamName(), 32),
                truncateString(match.getAwayTeam().getTeamName(), 32),
                time);
        }
        System.out.println("└" + "─".repeat(73) + "┘");
        System.out.println("\n✅ Total upcoming: " + upcoming.size() + " matches");
    }
    
    public void printStats() {
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        int finishedWithScores = 0;
        int upcomingCount = 0;
        
        // Get current date in UTC
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(Calendar.HOUR_OF_DAY, 0);
        utcCalendar.set(Calendar.MINUTE, 0);
        utcCalendar.set(Calendar.SECOND, 0);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        Date todayUTC = utcCalendar.getTime();
        
        for (Match m : allMatches) {
            if (m.isFinished() && m.getHomeScore() != null) {
                finishedWithScores++;
            }
            if (m.isScheduled() && m.getMatchDate() != null) {
                Calendar matchCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                matchCal.setTime(m.getMatchDate());
                matchCal.set(Calendar.HOUR_OF_DAY, 0);
                matchCal.set(Calendar.MINUTE, 0);
                matchCal.set(Calendar.SECOND, 0);
                matchCal.set(Calendar.MILLISECOND, 0);
                Date matchDateOnly = matchCal.getTime();
                if (matchDateOnly.compareTo(todayUTC) >= 0) {
                    upcomingCount++;
                }
            }
        }
        
        System.out.println("\n📊 PREMIER LEAGUE STATISTICS");
        System.out.println("=".repeat(40));
        System.out.println("   Teams : " + teamDAO.getTeamCount());
        System.out.println("   Matches: " + matchDAO.getMatchCount());
        System.out.println("   Finished with scores: " + finishedWithScores);
        System.out.println("   Upcoming matches: " + upcomingCount);
        System.out.println("=".repeat(40));
    }
    
    public String getCurrentISTTime() {
        return new SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a").format(new Date());
    }
    
    public String getCurrentUTCTime() {
        return utcDateFormat.format(new Date());
    }
    
    public boolean testAPI() {
        System.out.println("\n🔌 Testing API connection...");
        boolean connected = apiClient.testConnection();
        if (connected) {
            System.out.println("✅ API connection successful!");
        } else {
            System.out.println("❌ API connection failed!");
        }
        return connected;
    }
    
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
