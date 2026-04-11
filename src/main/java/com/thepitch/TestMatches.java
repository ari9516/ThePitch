package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.service.DataSyncService;
import com.thepitch.model.Match;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestMatches {
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("     THEPITCH - QUICK MATCH TEST");
        System.out.println("=".repeat(60));
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        
        SimpleDateFormat istFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        
        System.out.println("\n📍 Current IST Date: " + istFormat.format(new Date()));
        System.out.println("📍 Current UTC Date: " + syncService.getCurrentUTCTime());
        System.out.println("=".repeat(60));
        
        // Show just the last 3 matchweeks (quick view)
        System.out.println("\n📊 QUICK VIEW - LAST 3 MATCHWEEKS");
        syncService.showRecentMatches(3);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("💡 To sync fresh data, run: mvn exec:java and choose option 1");
        System.out.println("=".repeat(60));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}
