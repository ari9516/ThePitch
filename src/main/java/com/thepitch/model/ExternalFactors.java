package com.thepitch.model;

import java.util.ArrayList;
import java.util.List;

public class ExternalFactors {
    public List<String> homeTeamInjuries = new ArrayList<>();
    public List<String> awayTeamInjuries = new ArrayList<>();
    public List<String> homeTeamSuspensions = new ArrayList<>();
    public List<String> awayTeamSuspensions = new ArrayList<>();
    public String weatherCondition; // Sunny, Rain, Snow, Wind
    public int temperature;          // Celsius
    public int daysSinceLastMatch;
    public String refereeName;
    
    public double calculateInjuryImpact() {
        int keyPlayersOut = homeTeamInjuries.size() + awayTeamInjuries.size();
        return keyPlayersOut * 0.05;
    }
    
    public double calculateWeatherImpact() {
        switch(weatherCondition) {
            case "Heavy Rain": return -0.10;
            case "Snow": return -0.15;
            case "Strong Wind": return -0.08;
            default: return 0;
        }
    }
    
    public double calculateFatigueImpact() {
        if (daysSinceLastMatch < 3) return -0.10;
        if (daysSinceLastMatch > 10) return 0.05;
        return 0;
    }
}