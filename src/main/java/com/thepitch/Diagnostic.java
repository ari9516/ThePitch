package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.dao.MatchDAO;
import com.thepitch.dao.TeamDAO;
import com.thepitch.model.Match;
import com.thepitch.model.Team;

import java.text.SimpleDateFormat;
import java.util.*;

public class Diagnostic {
    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("     THEPITCH - DIAGNOSTIC TOOL");
        System.out.println("========================================\n");
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        MatchDAO matchDAO = new MatchDAO();
        TeamDAO teamDAO = new TeamDAO();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 1. Check teams
        System.out.println("📋 TEAMS IN DATABASE:");
        System.out.println("   Total teams: " + teamDAO.getTeamCount());
        
        List<Team> allTeams = teamDAO.getAllTeams();
        System.out.println("\n   First 10 teams:");
        for (int i = 0; i < Math.min(10, allTeams.size()); i++) {
            Team t = allTeams.get(i);
            System.out.println("   " + (i+1) + ". " + t.getTeamName() + " (ELO: " + t.getEloRating() + ")");
        }
        
        // 2. Check matches - date range
        System.out.println("\n\n⚽ MATCHES IN DATABASE:");
        System.out.println("   Total matches: " + matchDAO.getMatchCount());
        
        List<Match> allMatches = matchDAO.getAllMatchesSafe();
        
        if (allMatches.isEmpty()) {
            System.out.println("   ❌ No matches found in database!");
        } else {
            // Find date range
            Date earliest = allMatches.get(0).getMatchDate();
            Date latest = allMatches.get(0).getMatchDate();
            
            for (Match m : allMatches) {
                if (m.getMatchDate().before(earliest)) earliest = m.getMatchDate();
                if (m.getMatchDate().after(latest)) latest = m.getMatchDate();
            }
            
            System.out.println("   Earliest match date: " + sdf.format(earliest));
            System.out.println("   Latest match date:   " + sdf.format(latest));
            
            // 3. Show upcoming matches (future dates)
            System.out.println("\n\n📅 UPCOMING MATCHES (future dates):");
            Date now = new Date();
            List<Match> upcoming = new ArrayList<>();
            
            for (Match m : allMatches) {
                if (m.getMatchDate().after(now) && "SCHEDULED".equals(m.getStatus())) {
                    upcoming.add(m);
                }
            }
            
            if (upcoming.isEmpty()) {
                System.out.println("   ❌ No upcoming matches found!");
                System.out.println("\n   Possible reasons:");
                System.out.println("   1. The current football season has ended");
                System.out.println("   2. Next season fixtures not yet released");
                System.out.println("   3. API only provided historical data");
            } else {
                System.out.println("   Found " + upcoming.size() + " upcoming matches:");
                System.out.println("\n   First 10 upcoming matches:");
                for (int i = 0; i < Math.min(10, upcoming.size()); i++) {
                    Match m = upcoming.get(i);
                    System.out.println("   " + (i+1) + ". " + sdf.format(m.getMatchDate()));
                    System.out.println("      " + m.getHomeTeam().getTeamName() + " vs " + m.getAwayTeam().getTeamName());
                }
            }
            
            // 4. Show recent matches (past dates)
            System.out.println("\n\n📊 RECENT MATCHES (past dates):");
            List<Match> past = new ArrayList<>();
            for (Match m : allMatches) {
                if (m.getMatchDate().before(now) && "FINISHED".equals(m.getStatus())) {
                    past.add(m);
                }
            }
            
            if (past.isEmpty()) {
                System.out.println("   No finished matches found.");
            } else {
                System.out.println("   Found " + past.size() + " finished matches:");
                System.out.println("\n   Last 5 finished matches:");
                for (int i = Math.max(0, past.size() - 5); i < past.size(); i++) {
                    Match m = past.get(i);
                    System.out.println("   • " + sdf.format(m.getMatchDate()));
                    System.out.println("     " + m.getHomeTeam().getTeamName() + " " + 
                        (m.getHomeScore() != null ? m.getHomeScore() : "?") + " - " + 
                        (m.getAwayScore() != null ? m.getAwayScore() : "?") + " " + 
                        m.getAwayTeam().getTeamName());
                }
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("     DIAGNOSTIC COMPLETE");
        System.out.println("========================================\n");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}