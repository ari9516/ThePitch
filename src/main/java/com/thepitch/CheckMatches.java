package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.service.DataSyncService;
import com.thepitch.model.Match;
import java.text.SimpleDateFormat;
import java.util.*;

public class CheckMatches {
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("         THEPITCH - MATCH CHECKER");
        System.out.println("=".repeat(60));
        
        // Initialize database
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        
        // Show current IST time
        System.out.println("\n📍 Current IST Time: " + syncService.getCurrentISTTime());
        
        // Print database stats
        syncService.printStats();
        
        // Print today's matches
        System.out.println("\n" + "=".repeat(60));
        syncService.printTodayMatches();
        
        // Print tomorrow's matches
        System.out.println("\n" + "=".repeat(60));
        System.out.println("   📅 TOMORROW'S MATCHES");
        System.out.println("   " + "-".repeat(50));
        
        List<Match> tomorrowMatches = syncService.getTomorrowMatches();
        if (tomorrowMatches.isEmpty()) {
            System.out.println("   No matches scheduled for tomorrow");
        } else {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            
            for (Match match : tomorrowMatches) {
                String time = timeFormat.format(match.getMatchDate());
                System.out.printf("   • %-25s vs %-25s [%s]%n", 
                    truncate(match.getHomeTeam().getTeamName(), 25),
                    truncate(match.getAwayTeam().getTeamName(), 25),
                    time);
            }
        }
        
        // Print matches for next 3 days
        System.out.println("\n" + "=".repeat(60));
        syncService.printUpcomingMatches(3);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Check complete!");
        System.out.println("=".repeat(60));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
    
    private static String truncate(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }
}
