package com.thepitch.service;

import com.thepitch.model.Team;
import com.thepitch.model.TeamProfile;
import com.thepitch.model.RecentForm;
import com.thepitch.dao.AdminDAO;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import okhttp3.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * TacticalAnalyzer
 * ─────────────────
 * Uses Claude AI (Anthropic API) to generate a tactical matchup analysis
 * for a specific game, then lets the admin review and override it.
 *
 * Flow:
 *   1. Check DB for a saved analysis of this fixture → offer to reuse
 *   2. Build a rich data prompt from all Java match stats
 *   3. Call claude-sonnet-4-20250514 → parse HOME/AWAY tactical scores (0-10)
 *   4. Display full analysis to admin
 *   5. Admin accepts AI scores OR manually overrides them
 *   6. Save final scores to tactical_analysis table
 *   7. Return double[] {homeTacticalScore, awayTacticalScore}
 */
public class TacticalAnalyzer {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL             = "claude-sonnet-4-20250514";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String    claudeApiKey;
    private final OkHttpClient httpClient;
    private final Gson      gson;
    private final AdminDAO  adminDAO;

    public TacticalAnalyzer() throws SQLException {
        this.claudeApiKey = loadClaudeApiKey();
        this.httpClient   = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60,  TimeUnit.SECONDS)
            .build();
        this.gson    = new Gson();
        this.adminDAO = new AdminDAO();
        ensureTacticalTable();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAIN ENTRY POINT
    // ═════════════════════════════════════════════════════════════════════════

    public double[] runTacticalAnalysis(
            Team homeTeam, Team awayTeam,
            TeamProfile homeProfile, TeamProfile awayProfile,
            RecentForm homeForm, RecentForm awayForm,
            String matchStakes, Scanner scanner) {

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  AI TACTICAL ANALYSIS");
        System.out.println("  " + homeTeam.getTeamName() + " vs " + awayTeam.getTeamName());
        System.out.println("=".repeat(60));

        // Step 1: Check for saved analysis
        double[] saved = loadSavedTacticalScores(homeTeam.getTeamName(), awayTeam.getTeamName());
        if (saved != null) {
            System.out.printf("%n  Saved tactical scores found for this fixture:%n");
            System.out.printf("  %s: %.1f/10   %s: %.1f/10%n",
                homeTeam.getTeamName(), saved[0], awayTeam.getTeamName(), saved[1]);
            System.out.print("  Use saved? (y=use saved, n=run fresh AI analysis) [y]: ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("n")) {
                System.out.println("  Using saved scores.");
                return saved;
            }
        }

        // Step 2: Build prompt + call Claude
        System.out.println("\n  Sending match data to Claude AI...");
        System.out.println("  (Takes 5-15 seconds)\n");

        String aiResponse = null;
        if (apiKeyIsConfigured()) {
            try {
                String prompt = buildTacticalPrompt(homeTeam, awayTeam,
                    homeProfile, awayProfile, homeForm, awayForm, matchStakes);
                aiResponse = callClaudeAPI(prompt);
            } catch (Exception e) {
                System.out.println("  ⚠️  Claude API failed: " + e.getMessage());
                System.out.println("  Falling back to manual input.\n");
            }
        } else {
            System.out.println("  ⚠️  Claude API key not set in config.properties");
            System.out.println("  Add: claude.api.key=sk-ant-...");
            System.out.println("  Falling back to manual input.\n");
        }

        // Step 3: Parse + display AI result
        double[] aiScores = null;
        if (aiResponse != null) {
            aiScores = parseScores(aiResponse);
            displayAnalysis(aiResponse, aiScores,
                homeTeam.getTeamName(), awayTeam.getTeamName());
        }

        // Step 4: Admin override + save
        return adminOverrideFlow(aiScores,
            homeTeam.getTeamName(), awayTeam.getTeamName(), scanner);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PROMPT BUILDER
    // ═════════════════════════════════════════════════════════════════════════

    private String buildTacticalPrompt(
            Team homeTeam, Team awayTeam,
            TeamProfile homeProfile, TeamProfile awayProfile,
            RecentForm homeForm, RecentForm awayForm,
            String matchStakes) {

        StringBuilder p = new StringBuilder();
        p.append("You are a Premier League tactical analyst. Analyse the following match and provide ")
         .append("a tactical matchup score for EACH team (0-10), where:\n")
         .append("  10 = this team completely exploits the opponent tactically\n")
         .append("   5 = neutral / no clear advantage\n")
         .append("   0 = this team is at a severe tactical disadvantage\n\n");

        p.append("MATCH: ").append(homeTeam.getTeamName())
         .append(" (HOME) vs ").append(awayTeam.getTeamName()).append(" (AWAY)\n")
         .append("STAKES: ").append(matchStakes).append("\n\n");

        appendTeamBlock(p, "HOME", homeTeam, homeProfile, homeForm);
        appendTeamBlock(p, "AWAY", awayTeam, awayProfile, awayForm);

        p.append("Reply in this EXACT format (keep the labels exactly as shown):\n\n")
         .append("TACTICAL ANALYSIS:\n")
         .append("[4-6 sentences: formation compatibility, press vs possession, ")
         .append("pace matchups, set piece threat, key tactical battles, specific mismatches]\n\n")
         .append("KEY BATTLES:\n")
         .append("- [specific player/zone matchup 1]\n")
         .append("- [specific player/zone matchup 2]\n")
         .append("- [specific player/zone matchup 3]\n\n")
         .append("HOME ADVANTAGE FACTORS:\n")
         .append("- [factor 1]\n")
         .append("- [factor 2]\n\n")
         .append("TACTICAL SCORES:\n")
         .append("HOME_TACTICAL_SCORE: [0.0-10.0]\n")
         .append("AWAY_TACTICAL_SCORE: [0.0-10.0]\n\n")
         .append("REASONING: [One sentence explaining the split]\n");

        return p.toString();
    }

    private void appendTeamBlock(StringBuilder p, String label,
                                  Team team, TeamProfile profile, RecentForm form) {
        p.append(label).append(" TEAM - ").append(team.getTeamName()).append("\n")
         .append("  ELO: ").append(String.format("%.0f", team.getEloRating())).append("\n")
         .append("  Form (last 5): ").append(form.formString)
         .append("  (").append(String.format("%.0f", form.formPercentage)).append("%)\n")
         .append("  Trend: ").append(form.trend).append("\n")
         .append("  Goals scored/game: ").append(String.format("%.2f", profile.goalsScored)).append("\n")
         .append("  Goals conceded/game: ").append(String.format("%.2f", profile.goalsConceded)).append("\n")
         .append("  Shots/game: ").append(String.format("%.1f", profile.shotsTotal)).append("\n")
         .append("  Shots on target/game: ").append(String.format("%.1f", profile.shotsOnTarget)).append("\n")
         .append("  xG for/game: ").append(String.format("%.2f", profile.xG)).append("\n")
         .append("  Corners/game: ").append(String.format("%.1f", profile.cornersFor)).append("\n")
         .append("  Fouls/game: ").append(String.format("%.1f", profile.fouls)).append("\n")
         .append("  Clean sheets: ").append(profile.cleanSheets).append("\n");
        if (form.streak != null && !form.streak.isEmpty()) {
            p.append("  Streak: ").append(form.streak).append("\n");
        }
        p.append("\n");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CLAUDE API CALL
    // ═════════════════════════════════════════════════════════════════════════

    private String callClaudeAPI(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("max_tokens", 1000);

        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        JsonArray msgs = new JsonArray();
        msgs.add(msg);
        body.add("messages", msgs);

        Request req = new Request.Builder()
            .url(ANTHROPIC_API_URL)
            .addHeader("x-api-key", claudeApiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(RequestBody.create(
                gson.toJson(body),
                MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP " + resp.code() + ": " + resp.message());
            }
            JsonObject json = gson.fromJson(resp.body().string(), JsonObject.class);
            return json.getAsJsonArray("content")
                       .get(0).getAsJsonObject()
                       .get("text").getAsString();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PARSE SCORES
    // ═════════════════════════════════════════════════════════════════════════

    private double[] parseScores(String response) {
        double home = 5.0, away = 5.0;
        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.startsWith("HOME_TACTICAL_SCORE:")) {
                try { home = clamp(Double.parseDouble(line.replace("HOME_TACTICAL_SCORE:", "").trim())); }
                catch (NumberFormatException ignored) {}
            }
            if (line.startsWith("AWAY_TACTICAL_SCORE:")) {
                try { away = clamp(Double.parseDouble(line.replace("AWAY_TACTICAL_SCORE:", "").trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return new double[]{home, away};
    }

    private double clamp(double v) { return Math.max(0, Math.min(10, v)); }

    // ═════════════════════════════════════════════════════════════════════════
    // DISPLAY
    // ═════════════════════════════════════════════════════════════════════════

    private void displayAnalysis(String response, double[] scores,
                                  String home, String away) {
        System.out.println("─".repeat(60));
        System.out.println("  CLAUDE AI TACTICAL ANALYSIS");
        System.out.println("─".repeat(60));
        // Print everything except the raw score lines (they're shown formatted below)
        for (String line : response.split("\n")) {
            if (!line.startsWith("HOME_TACTICAL_SCORE:") && !line.startsWith("AWAY_TACTICAL_SCORE:")) {
                System.out.println("  " + line);
            }
        }
        System.out.println("─".repeat(60));
        if (scores != null) {
            System.out.printf("  AI SCORES → %s: %.1f/10   %s: %.1f/10%n",
                home, scores[0], away, scores[1]);
        }
        System.out.println("─".repeat(60));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN OVERRIDE
    // ═════════════════════════════════════════════════════════════════════════

    private double[] adminOverrideFlow(double[] aiScores, String homeName,
                                        String awayName, Scanner scanner) {
        double homeScore = (aiScores != null) ? aiScores[0] : 5.0;
        double awayScore = (aiScores != null) ? aiScores[1] : 5.0;

        System.out.println("\n  ADMIN OVERRIDE");
        System.out.println("  Accept AI scores or enter your own.");
        System.out.println("  Guide: 5=neutral  7=moderate edge  9=dominant edge");

        System.out.printf("%n  Home score (%s) [AI: %.1f] — press Enter to accept: ", homeName, homeScore);
        String hi = scanner.nextLine().trim();
        if (!hi.isEmpty()) {
            try { homeScore = clamp(Double.parseDouble(hi)); }
            catch (NumberFormatException e) { System.out.println("  Invalid — keeping AI score."); }
        }

        System.out.printf("  Away score (%s) [AI: %.1f] — press Enter to accept: ", awayName, awayScore);
        String ai = scanner.nextLine().trim();
        if (!ai.isEmpty()) {
            try { awayScore = clamp(Double.parseDouble(ai)); }
            catch (NumberFormatException e) { System.out.println("  Invalid — keeping AI score."); }
        }

        System.out.print("\n  Your tactical note (optional, press Enter to skip): ");
        String note = scanner.nextLine().trim();

        saveTacticalScores(homeName, awayName, homeScore, awayScore, note);

        System.out.printf("%n  FINAL → %s: %.1f/10   %s: %.1f/10%n",
            homeName, homeScore, awayName, awayScore);

        return new double[]{homeScore, awayScore};
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PERSISTENCE helpers (delegates to AdminDAO)
    // ═════════════════════════════════════════════════════════════════════════

    private void ensureTacticalTable() {
        try { adminDAO.ensureTacticalTable(); }
        catch (Exception e) { /* table creation failure is non-fatal */ }
    }

    private void saveTacticalScores(String home, String away,
                                     double hs, double as, String note) {
        try {
            adminDAO.saveTacticalAnalysis(home, away, hs, as, note);
            System.out.println("  Tactical scores saved.");
        } catch (Exception e) {
            System.err.println("  Could not save: " + e.getMessage());
        }
    }

    private double[] loadSavedTacticalScores(String home, String away) {
        try { return adminDAO.getTacticalScores(home, away); }
        catch (Exception e) { return null; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONFIG
    // ═════════════════════════════════════════════════════════════════════════

    private boolean apiKeyIsConfigured() {
        return claudeApiKey != null
            && !claudeApiKey.isEmpty()
            && !claudeApiKey.equals("YOUR_CLAUDE_API_KEY");
    }

    private String loadClaudeApiKey() {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config.properties");
            if (is != null) { props.load(is); }
            return props.getProperty("claude.api.key", "");
        } catch (Exception e) { return ""; }
    }
}
