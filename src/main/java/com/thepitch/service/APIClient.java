package com.thepitch.service;

import com.google.gson.*;
import com.thepitch.model.Team;
import com.thepitch.model.Match;
import okhttp3.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * APIClient - Fetches real football data from football-data.org
 * Free tier: 10 requests/minute, current season only
 * 
 * @author ThePitch Team
 * @version 2.2
 */
public class APIClient {
    
    private static final String BASE_URL = "https://api.football-data.org/v4/";
    private String apiKey;
    private OkHttpClient client;
    private Gson gson;
    private SimpleDateFormat apiDateFormat;
    
    // Rate limiting tracking
    private int requestCount;
    private long lastRequestTime;
    private static final int MAX_REQUESTS_PER_MINUTE = 9; // Leave 1 buffer
    
    // Cache for rate limit headers
    private int remainingRequests = 10;
    private long resetTime = 0;
    
    // Track if we've printed scores already for a league
    private boolean scorePrintingEnabled = true;
    
    public APIClient() {
        // Configure HTTP client with timeouts
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        this.gson = new Gson();
        this.apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
        loadApiKey();
    }
    
    /**
     * Load API key from config.properties
     */
    private void loadApiKey() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                this.apiKey = props.getProperty("api.key");
                if (this.apiKey != null && !this.apiKey.equals("YOUR_API_KEY") && !this.apiKey.equals("YOUR_API_KEY_HERE")) {
                    System.out.println("✅ API key loaded from config");
                } else {
                    System.err.println("⚠️ No valid API key found in config.properties");
                    System.err.println("   Please add your API key from football-data.org");
                    this.apiKey = null;
                }
            } else {
                System.err.println("❌ config.properties not found in resources folder!");
                this.apiKey = null;
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to load config: " + e.getMessage());
            this.apiKey = null;
        }
    }
    
    /**
     * Check if API key is valid
     */
    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.equals("YOUR_API_KEY") && !apiKey.equals("YOUR_API_KEY_HERE");
    }
    
    /**
     * Enforce rate limiting based on API response headers
     */
    private void checkRateLimit() throws InterruptedException {
        // If we have rate limit info from API, respect it
        if (remainingRequests <= 1 && resetTime > System.currentTimeMillis()) {
            long waitTime = resetTime - System.currentTimeMillis() + 1000;
            if (waitTime > 0 && waitTime < 60000) {
                System.out.println("⏳ Rate limit: " + remainingRequests + " requests left. Waiting " + (waitTime/1000) + " seconds...");
                Thread.sleep(waitTime);
            }
        }
        
        // Fallback: simple counter-based rate limiting
        requestCount++;
        long now = System.currentTimeMillis();
        
        if (now - lastRequestTime > 60000) {
            requestCount = 1;
            lastRequestTime = now;
        }
        
        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            long waitTime = 60000 - (now - lastRequestTime);
            if (waitTime > 0) {
                System.out.println("⏳ Rate limit approaching. Waiting " + (waitTime/1000) + " seconds...");
                Thread.sleep(waitTime);
            }
            requestCount = 1;
            lastRequestTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Execute request and track rate limits from response headers
     */
    private Response executeRequest(Request request) throws IOException, InterruptedException {
        checkRateLimit();
        
        if (apiKey == null) {
            throw new IOException("API key not configured. Please add your API key to config.properties");
        }
        
        Response response = client.newCall(request).execute();
        
        // Parse rate limit headers
        String remainingHeader = response.header("X-Requests-Available-Minute");
        String resetHeader = response.header("X-RequestCounter-Reset");
        
        if (remainingHeader != null) {
            remainingRequests = Integer.parseInt(remainingHeader);
        }
        if (resetHeader != null) {
            resetTime = System.currentTimeMillis() + (Long.parseLong(resetHeader) * 1000);
        }
        
        return response;
    }
    
    /**
     * Fetch matches for a specific competition (e.g., PL = Premier League)
     */
    public JsonObject fetchCompetitionMatches(String competitionCode) throws Exception {
        String url = BASE_URL + "competitions/" + competitionCode + "/matches";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build();
        
        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                throw new Exception("API Error " + response.code() + ": " + errorBody);
            }
            
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
    
    /**
     * Fetch matches for a specific competition with season filter
     */
    public JsonObject fetchCompetitionMatches(String competitionCode, int season) throws Exception {
        String url = BASE_URL + "competitions/" + competitionCode + "/matches?season=" + season;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build();
        
        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new Exception("API Error: " + response.code());
            }
            
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
    
    /**
     * Fetch teams in a competition
     */
    public JsonObject fetchCompetitionTeams(String competitionCode) throws Exception {
        String url = BASE_URL + "competitions/" + competitionCode + "/teams";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build();
        
        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new Exception("API Error: " + response.code());
            }
            
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
    
    /**
     * Fetch standings/league table for a competition
     */
    public JsonObject fetchCompetitionStandings(String competitionCode) throws Exception {
        String url = BASE_URL + "competitions/" + competitionCode + "/standings";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build();
        
        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new Exception("API Error: " + response.code());
            }
            
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
    
    /**
     * Fetch a specific team by ID
     */
    public JsonObject fetchTeam(int teamId) throws Exception {
        String url = BASE_URL + "teams/" + teamId;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build();
        
        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new Exception("API Error: " + response.code());
            }
            
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        }
    }
    
    /**
     * Parse matches from API response into Match objects - FIXED to handle all status types
     */
    public List<Match> parseMatches(JsonObject response, int leagueId) {
        List<Match> matches = new ArrayList<>();
        
        JsonArray matchesArray = response.getAsJsonArray("matches");
        if (matchesArray == null || matchesArray.size() == 0) {
            return matches;
        }
        
        int scoreCount = 0;
        int scheduledCount = 0;
        
        for (JsonElement element : matchesArray) {
            JsonObject matchObj = element.getAsJsonObject();
            
            try {
                int matchId = matchObj.get("id").getAsInt();
                String dateStr = matchObj.get("utcDate").getAsString();
                Date matchDate = apiDateFormat.parse(dateStr);
                String apiStatus = matchObj.get("status").getAsString();
                
                // Parse teams
                JsonObject homeTeamObj = matchObj.getAsJsonObject("homeTeam");
                JsonObject awayTeamObj = matchObj.getAsJsonObject("awayTeam");
                
                if (homeTeamObj == null || awayTeamObj == null) {
                    continue;
                }
                
                Team homeTeam = new Team();
                homeTeam.setTeamId(homeTeamObj.get("id").getAsInt());
                homeTeam.setTeamName(homeTeamObj.get("name").getAsString());
                homeTeam.setLeagueId(leagueId);
                
                Team awayTeam = new Team();
                awayTeam.setTeamId(awayTeamObj.get("id").getAsInt());
                awayTeam.setTeamName(awayTeamObj.get("name").getAsString());
                awayTeam.setLeagueId(leagueId);
                
                Match match = new Match(matchId, matchDate, homeTeam, awayTeam, leagueId);
                
                // ========== FIX: Handle all status types properly ==========
                // Possible statuses from API:
                // - SCHEDULED: match scheduled for future
                // - TIMED: match scheduled with confirmed time
                // - LIVE / IN_PLAY: match in progress
                // - PAUSED: match temporarily stopped
                // - FINISHED: match completed
                // - POSTPONED: match postponed
                // - CANCELLED: match cancelled
                
                switch (apiStatus) {
                    case "FINISHED":
                        match.setStatus("FINISHED");
                        break;
                    case "LIVE":
                    case "IN_PLAY":
                        match.setStatus("LIVE");
                        break;
                    case "TIMED":
                    case "SCHEDULED":
                        match.setStatus("SCHEDULED");
                        scheduledCount++;
                        break;
                    case "POSTPONED":
                        match.setStatus("POSTPONED");
                        break;
                    case "CANCELLED":
                        match.setStatus("CANCELLED");
                        break;
                    default:
                        match.setStatus(apiStatus);
                }
                
                // Parse scores
                JsonObject scoreObj = matchObj.getAsJsonObject("score");
                if (scoreObj != null) {
                    // Try fullTime first (final score)
                    JsonObject fullTimeObj = scoreObj.getAsJsonObject("fullTime");
                    if (fullTimeObj != null) {
                        if (!fullTimeObj.get("home").isJsonNull()) {
                            match.setHomeScore(fullTimeObj.get("home").getAsInt());
                        }
                        if (!fullTimeObj.get("away").isJsonNull()) {
                            match.setAwayScore(fullTimeObj.get("away").getAsInt());
                        }
                    }
                    
                    // If fullTime is null/empty, try halfTime
                    if (match.getHomeScore() == null && match.getAwayScore() == null) {
                        JsonObject halfTimeObj = scoreObj.getAsJsonObject("halfTime");
                        if (halfTimeObj != null) {
                            if (!halfTimeObj.get("home").isJsonNull()) {
                                match.setHomeScore(halfTimeObj.get("home").getAsInt());
                            }
                            if (!halfTimeObj.get("away").isJsonNull()) {
                                match.setAwayScore(halfTimeObj.get("away").getAsInt());
                            }
                        }
                    }
                    
                    // If we have scores, mark as FINISHED (override status)
                    if (match.getHomeScore() != null && match.getAwayScore() != null) {
                        match.setStatus("FINISHED");
                        scoreCount++;
                        
                        // Print first 10 scores as sample
                        if (scoreCount <= 10 && scorePrintingEnabled) {
                            System.out.println("      📊 Score: " + homeTeam.getTeamName() + " " + 
                                match.getHomeScore() + " - " + match.getAwayScore() + " " + 
                                awayTeam.getTeamName());
                        }
                    }
                }
                
                matches.add(match);
                
            } catch (Exception e) {
                System.err.println("Warning: Error parsing match: " + e.getMessage());
            }
        }
        
        if (scoreCount > 0 && scorePrintingEnabled) {
            System.out.println("      📊 Total " + scoreCount + " matches with scores found");
        }
        if (scheduledCount > 0) {
            System.out.println("      ⏰ Total " + scheduledCount + " scheduled/upcoming matches found");
        }
        scorePrintingEnabled = false; // Only print once per sync
        
        return matches;
    }
    
    /**
     * Parse teams from API response
     */
    public List<Team> parseTeams(JsonObject response, int leagueId) {
        List<Team> teams = new ArrayList<>();
        
        JsonArray teamsArray = response.getAsJsonArray("teams");
        if (teamsArray == null || teamsArray.size() == 0) {
            return teams;
        }
        
        for (JsonElement element : teamsArray) {
            JsonObject teamObj = element.getAsJsonObject();
            
            try {
                Team team = new Team();
                team.setTeamId(teamObj.get("id").getAsInt());
                team.setTeamName(teamObj.get("name").getAsString());
                team.setLeagueId(leagueId);
                team.setEloRating(1500); // Starting ELO
                team.setLastUpdated(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                
                teams.add(team);
            } catch (Exception e) {
                System.err.println("Warning: Error parsing team: " + e.getMessage());
            }
        }
        
        return teams;
    }
    
    /**
     * Get the competition code for a league ID
     */
    public static String getCompetitionCode(int leagueId) {
        switch (leagueId) {
            case 2021: return "PL";      // Premier League
            case 2014: return "PD";      // La Liga
            case 2019: return "SA";      // Serie A
            case 2002: return "BL1";     // Bundesliga
            case 2015: return "FL1";     // Ligue 1
            default: return null;
        }
    }
    
    /**
     * Get league name from competition code
     */
    public static String getLeagueName(String competitionCode) {
        switch (competitionCode) {
            case "PL": return "Premier League";
            case "PD": return "La Liga";
            case "SA": return "Serie A";
            case "BL1": return "Bundesliga";
            case "FL1": return "Ligue 1";
            default: return "Unknown League";
        }
    }
    
    /**
     * Test the API connection with a simple request
     */
    public boolean testConnection() {
        if (!hasValidApiKey()) {
            System.err.println("❌ No valid API key configured");
            return false;
        }
        
        try {
            JsonObject response = fetchCompetitionMatches("PL");
            boolean success = response != null && response.has("matches");
            if (success) {
                System.out.println("   API responded with " + response.getAsJsonArray("matches").size() + " matches");
            }
            return success;
        } catch (Exception e) {
            System.err.println("   API test error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get remaining requests for current minute
     */
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    /**
     * Get rate limit status as string
     */
    public String getRateLimitStatus() {
        return remainingRequests + " requests remaining this minute";
    }
    
    /**
     * Reset score printing flag (for new sync sessions)
     */
    public void resetScorePrinting() {
        this.scorePrintingEnabled = true;
    }
}
