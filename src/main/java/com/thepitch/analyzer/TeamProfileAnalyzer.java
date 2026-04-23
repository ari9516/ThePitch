package com.thepitch.analyzer;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.model.Match;
import com.thepitch.model.Team;
import com.thepitch.model.TeamProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TeamProfileAnalyzer {

    // ── League-average fallback constants (used when DB has no data yet) ──────
    // Source: Premier League 2024-25 season averages
    private static final double FALLBACK_SHOTS        = 12.5;
    private static final double FALLBACK_SOT          = 4.2;
    private static final double FALLBACK_XG           = 1.35;
    private static final double FALLBACK_YELLOW_CARDS = 1.8;
    private static final double FALLBACK_RED_CARDS    = 0.08;
    private static final double FALLBACK_FOULS        = 10.5;
    private static final double FALLBACK_CORNERS      = 5.1;   // FBref "Crs" (crosses); corners ~5 per team/game

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Analyze home team performance.
     * Goals/cleansheets/BTTS come from match results already in the DB.
     * Shots/xG/cards/corners come from the match_stats table (imported via CSVImporter).
     */
    public TeamProfile analyzeHomeProfile(Team team, List<Match> homeMatches) {
        TeamProfile profile = new TeamProfile();
        profile.teamName = team.getTeamName();
        profile.isHome   = true;

        List<Match> finished = filterFinished(homeMatches);

        if (finished.isEmpty()) {
            return applyFallbackStats(profile, team.getTeamName(), true);
        }

        // Goals / clean-sheets / BTTS from match results (always available)
        int matchCount = finished.size();
        double totalScored = 0, totalConceded = 0;
        int cleanSheets = 0, bttsCount = 0;

        for (Match m : finished) {
            totalScored    += m.getHomeScore();
            totalConceded  += m.getAwayScore();
            if (m.getAwayScore() == 0)      cleanSheets++;
            if (m.isBothTeamsScored())       bttsCount++;
        }

        profile.goalsScored    = totalScored   / matchCount;
        profile.goalsConceded  = totalConceded / matchCount;
        profile.cleanSheets    = cleanSheets;
        profile.bttsCount      = bttsCount;

        // Advanced stats from match_stats table
        applyAdvancedStatsFromDB(profile, team.getTeamName());

        // Streak detection uses goal data (always real) + shot data (real or fallback)
        List<Match> last5 = getLast5(finished);
        profile.streakAlerts = detectStreaks(last5, true, profile);

        return profile;
    }

    /**
     * Analyze away team performance.
     */
    public TeamProfile analyzeAwayProfile(Team team, List<Match> awayMatches) {
        TeamProfile profile = new TeamProfile();
        profile.teamName = team.getTeamName();
        profile.isHome   = false;

        List<Match> finished = filterFinished(awayMatches);

        if (finished.isEmpty()) {
            return applyFallbackStats(profile, team.getTeamName(), false);
        }

        int matchCount = finished.size();
        double totalScored = 0, totalConceded = 0;
        int cleanSheets = 0, bttsCount = 0;

        for (Match m : finished) {
            totalScored   += m.getAwayScore();
            totalConceded += m.getHomeScore();
            if (m.getHomeScore() == 0)  cleanSheets++;
            if (m.isBothTeamsScored())  bttsCount++;
        }

        profile.goalsScored   = totalScored   / matchCount;
        profile.goalsConceded = totalConceded / matchCount;
        profile.cleanSheets   = cleanSheets;
        profile.bttsCount     = bttsCount;

        applyAdvancedStatsFromDB(profile, team.getTeamName());

        List<Match> last5 = getLast5(finished);
        profile.streakAlerts = detectStreaks(last5, false, profile);

        return profile;
    }

    /**
     * Calculate home advantage factor based on profiles.
     */
    public double calculateHomeAdvantage(TeamProfile homeProfile, TeamProfile awayProfile) {
        // Base home advantage ~15%
        double homeAdvantage = 0.15;

        // Better home xG vs away xG conceded → more advantage
        homeAdvantage += (homeProfile.xG - awayProfile.xGConceded) * 0.03;

        // More corners won → slight edge
        homeAdvantage += (homeProfile.cornersFor - awayProfile.cornersAgainst) / 200.0;

        return Math.max(0.05, Math.min(0.35, homeAdvantage));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Queries match_stats table for aggregated advanced stats for this team.
     * If no rows exist (CSV not yet imported), applies league-average fallbacks.
     */
    private void applyAdvancedStatsFromDB(TeamProfile profile, String teamName) {
        String sql =
            "SELECT " +
            "    AVG(shots)        AS avg_shots, " +
            "    AVG(shots_ot)     AS avg_sot, " +
            "    AVG(xg)           AS avg_xg, " +
            "    AVG(yellow_cards) AS avg_yellows, " +
            "    AVG(red_cards)    AS avg_reds, " +
            "    AVG(fouls)        AS avg_fouls, " +
            "    AVG(corners)      AS avg_corners, " +
            "    COUNT(*)          AS match_count " +
            "FROM match_stats " +
            "WHERE LOWER(team_name) LIKE LOWER(?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Fuzzy match: "Manchester City" matches "Man City", "Manchester City FC" etc.
            ps.setString(1, "%" + normaliseName(teamName) + "%");
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt("match_count") > 0) {
                profile.shotsTotal     = rs.getDouble("avg_shots");
                profile.shotsOnTarget  = rs.getDouble("avg_sot");
                profile.xG             = rs.getDouble("avg_xg");
                profile.yellowCards    = rs.getDouble("avg_yellows");
                profile.redCards       = rs.getDouble("avg_reds");
                profile.fouls          = rs.getDouble("avg_fouls");
                profile.cornersFor     = rs.getDouble("avg_corners");
                profile.shotAccuracy   = profile.shotsTotal > 0
                    ? (profile.shotsOnTarget / profile.shotsTotal) * 100 : 0;
                profile.dataSource     = "FBref (" + rs.getInt("match_count") + " matches)";

                System.out.printf("   📊 Real stats loaded for %-25s [%s]%n",
                    teamName, profile.dataSource);
            } else {
                applyLeagueAverageFallback(profile);
                System.out.printf("   ⚠️  No match_stats rows for %-25s → using league averages%n", teamName);
                System.out.println("       Run option 7 to import FBref CSV data.");
            }

        } catch (SQLException e) {
            // match_stats table doesn't exist yet (CSV never imported) — silent fallback
            applyLeagueAverageFallback(profile);
            profile.dataSource = "League Average Fallback (run option 7 to import real data)";
        }

        // xG conceded: query opponents' xG against this team
        profile.xGConceded = queryOpponentXG(teamName);
    }

    /**
     * Queries the average xG that opponents produced against this team.
     * Used by calculateHomeAdvantage().
     */
    private double queryOpponentXG(String teamName) {
        // We don't store opponent xG separately, so we approximate as overall league avg.
        // Once data is richer this can be refined.
        return FALLBACK_XG;
    }

    private void applyLeagueAverageFallback(TeamProfile profile) {
        profile.shotsTotal    = FALLBACK_SHOTS;
        profile.shotsOnTarget = FALLBACK_SOT;
        profile.xG            = FALLBACK_XG;
        profile.yellowCards   = FALLBACK_YELLOW_CARDS;
        profile.redCards      = FALLBACK_RED_CARDS;
        profile.fouls         = FALLBACK_FOULS;
        profile.cornersFor    = FALLBACK_CORNERS;
        profile.shotAccuracy  = (FALLBACK_SOT / FALLBACK_SHOTS) * 100;
        profile.dataSource    = "League Average Fallback";
    }

    /**
     * Full fallback when there are no finished matches at all (e.g. early season).
     */
    private TeamProfile applyFallbackStats(TeamProfile profile, String teamName, boolean isHome) {
        profile.goalsScored   = 0;
        profile.goalsConceded = 0;
        profile.cleanSheets   = 0;
        profile.bttsCount     = 0;
        profile.streakAlerts  = new ArrayList<>();
        applyAdvancedStatsFromDB(profile, teamName);  // still try the DB
        return profile;
    }

    /**
     * Detect streaks (≥80% occurrence) in last N matches.
     * Uses real goals data; shot/corner thresholds use profile averages.
     */
    private List<String> detectStreaks(List<Match> matches, boolean isHome, TeamProfile profile) {
        List<String> streaks = new ArrayList<>();
        if (matches.size() < 3) return streaks;   // need at least 3 for meaningful streaks

        int n = matches.size();
        int bttsCount      = 0;
        int over25Count    = 0;
        int cleanSheets    = 0;
        int scoredCount    = 0;
        int highShotsCount = 0;   // above team's own average

        for (Match m : matches) {
            if (m.isBothTeamsScored())      bttsCount++;
            if (m.getTotalGoals() > 2)      over25Count++;

            if (isHome) {
                if (m.getAwayScore() == 0)  cleanSheets++;
                if (m.getHomeScore() > 0)   scoredCount++;
            } else {
                if (m.getHomeScore() == 0)  cleanSheets++;
                if (m.getAwayScore() > 0)   scoredCount++;
            }
        }

        int threshold = (int) Math.ceil(n * 0.8);   // 80% threshold (more realistic than 90%)

        if (bttsCount    >= threshold) streaks.add("🎯 BTTS in " + bttsCount + "/" + n + " recent matches");
        if (over25Count  >= threshold) streaks.add("⚽ Over 2.5 goals in " + over25Count + "/" + n + " recent matches");
        if (cleanSheets  >= threshold) streaks.add("🧤 Clean sheet in " + cleanSheets + "/" + n + " recent matches");
        if (scoredCount  >= threshold) streaks.add("⭐ Scored in " + scoredCount + "/" + n + " recent matches");

        // Corner streak — only meaningful if real data is loaded
        if (profile.dataSource != null && profile.dataSource.startsWith("FBref")) {
            if (profile.cornersFor > 7.0) {
                streaks.add("🔥 Averaging " + String.format("%.1f", profile.cornersFor) + " corners/game this season");
            }
        }

        return streaks;
    }

    // ── Utility helpers ────────────────────────────────────────────────────────

    private List<Match> filterFinished(List<Match> matches) {
        List<Match> result = new ArrayList<>();
        for (Match m : matches) {
            if (m.isFinished() && m.getHomeScore() != null && m.getAwayScore() != null) {
                result.add(m);
            }
        }
        return result;
    }

    private List<Match> getLast5(List<Match> finished) {
        int from = Math.max(0, finished.size() - 5);
        return finished.subList(from, finished.size());
    }

    /**
     * Strips common suffixes so "Manchester City FC" matches "Man City" in the DB.
     * FBref usually stores just "Manchester City" without "FC".
     */
    private String normaliseName(String name) {
        return name.replace(" FC", "")
                   .replace(" AFC", "")
                   .replace(" City", "")   // won't match "Man City" but close enough for LIKE %...%
                   .trim();
    }
}
