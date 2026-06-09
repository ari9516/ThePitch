package com.thepitch.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ExternalFactors — manually entered inputs for a specific match prediction.
 *
 * Two types of data:
 *   1. Session-only  — weather, tactical score, referee score (entered fresh each time)
 *   2. Loaded from DB — injury lists (persisted via AdminDAO / injury_list table)
 */
public class ExternalFactors {

    // ── Injury lists ──────────────────────────────────────────────────────────
    /** Player names only — used for display in the console output */
    public List<String> homeTeamInjuries = new ArrayList<>();
    public List<String> awayTeamInjuries = new ArrayList<>();

    /**
     * Full injury detail maps from AdminDAO — used by the prediction engine.
     * Each map has: player_name, tier, position_criticality, position_label, notes
     */
    public List<Map<String, Object>> homeInjuryDetails = new ArrayList<>();
    public List<Map<String, Object>> awayInjuryDetails  = new ArrayList<>();

    // ── Weather ───────────────────────────────────────────────────────────────
    /** Sunny / Rain / Snow / Wind */
    public String weatherCondition = "Sunny";

    // ── Fatigue ───────────────────────────────────────────────────────────────
    /** Days since home team's last match (both teams assumed same unless specified) */
    public int daysSinceLastMatch = 7;

    // ── Tactical matchup (0-10) ───────────────────────────────────────────────
    /**
     * How well the home team's style exploits the away team's weaknesses.
     * 5 = neutral, 8+ = home has clear edge, 2 = away has clear edge.
     * Away score is mirrored: awayTacticalScore = 10 - homeTacticalScore
     */
    public double homeTacticalScore = 5.0;
    public double awayTacticalScore = 5.0;

    // ── Referee bias (0-10) ───────────────────────────────────────────────────
    /**
     * Referee tendency score for the home team.
     * 5 = neutral/unknown, 7+ = ref tends to favour home team style.
     * Away score is mirrored: awayRefereeScore = 10 - homeRefereeScore
     */
    public double homeRefereeScore = 5.0;
    public double awayRefereeScore = 5.0;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Convert weather string to a 0-10 score for the home team.
     * Rain/Wind slightly favours physical/direct home teams.
     * Returns neutral 5.0 if unknown.
     */
    public double getWeatherScoreForHome() {
        if (weatherCondition == null) return 5.0;
        switch (weatherCondition.trim().toLowerCase()) {
            case "rain":  return 5.5;  // slight home advantage (familiarity)
            case "wind":  return 5.5;
            case "snow":  return 5.0;  // unpredictable, neutral
            case "sunny": return 5.0;
            default:      return 5.0;
        }
    }

    public double getWeatherScoreForAway() {
        return 10.0 - getWeatherScoreForHome();
    }

    /**
     * Convert days since last match into a fatigue score (0-10).
     * 10 = fully rested (7+ days), 0 = 3rd game in 7 days (3 days or less)
     */
    public double getFatigueScore() {
        if (daysSinceLastMatch >= 7)  return 10.0;
        if (daysSinceLastMatch >= 6)  return 8.5;
        if (daysSinceLastMatch >= 5)  return 7.0;
        if (daysSinceLastMatch >= 4)  return 5.5;
        if (daysSinceLastMatch >= 3)  return 3.0;
        return 1.0; // 2 days or less — severe fatigue
    }

    @Override
    public String toString() {
        return "Weather: " + weatherCondition +
               " | Days rest: " + daysSinceLastMatch +
               " | Home tactical: " + homeTacticalScore +
               " | Home referee: " + homeRefereeScore +
               " | Home injuries: " + homeTeamInjuries.size() +
               " | Away injuries: " + awayTeamInjuries.size();
    }

    // ── Methods called by EnhancedPredictionEngine ────────────────────────────

    /**
     * Returns net injury impact on home team (negative = home disadvantaged).
     * Each injury reduces score: elite=-0.04, regular=-0.025, squad=-0.01
     */
    public double calculateInjuryImpact() {
        double impact = 0.0;
        for (Map<String, Object> inj : homeInjuryDetails) {
            impact -= getTierImpact((String) inj.get("tier"),
                                    (Double) inj.get("position_criticality"));
        }
        for (Map<String, Object> inj : awayInjuryDetails) {
            impact += getTierImpact((String) inj.get("tier"),
                                    (Double) inj.get("position_criticality"));
        }
        return Math.max(-0.12, Math.min(0.12, impact));
    }

    private double getTierImpact(String tier, Double criticality) {
        if (tier == null || criticality == null) return 0.01;
        double base;
        switch (tier.toLowerCase()) {
            case "elite":   base = 0.04; break;
            case "regular": base = 0.025; break;
            default:        base = 0.01; break;
        }
        return base * criticality;
    }

    /**
     * Returns weather impact on home team probability.
     * Rain/Wind/Snow slightly favour home team familiarity.
     */
    public double calculateWeatherImpact() {
        if (weatherCondition == null) return 0.0;
        switch (weatherCondition.trim().toLowerCase()) {
            case "rain":  return 0.02;
            case "wind":  return 0.015;
            case "snow":  return 0.01;
            default:      return 0.0;
        }
    }

    /**
     * Returns fatigue impact on home team probability.
     * Short rest hurts both teams but home team slightly less.
     */
    public double calculateFatigueImpact() {
        if (daysSinceLastMatch >= 7) return 0.0;
        if (daysSinceLastMatch >= 5) return 0.01;
        if (daysSinceLastMatch >= 4) return 0.02;
        return 0.03; // 3 days or less — meaningful fatigue advantage for home
    }
}
