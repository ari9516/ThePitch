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
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        
        // Show current time
        System.out.println("\n📍 Current IST Time: " + syncService.getCurrentISTTime());
        System.out.println("📍 Current UTC Time: " + syncService.getCurrentUTCTime());
        
        // Show statistics
        syncService.printStats();
        
        // Show recent matches
        System.out.println("\n" + "=".repeat(60));
        syncService.showRecentMatches(5);
        
        // Show upcoming matches
        System.out.println("\n" + "=".repeat(60));
        syncService.showUpcomingMatches();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Check complete!");
        System.out.println("=".repeat(60));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}
