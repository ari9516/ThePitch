package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.service.DataSyncService;
import com.thepitch.model.Match;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("         THEPITCH - FOOTBALL ANALYTICS    ");
        System.out.println("========================================");
        
        // Initialize database
        DatabaseConnection db = DatabaseConnection.getInstance();
        
        // Initialize services
        DataSyncService syncService = new DataSyncService();
        
        // Show current time
        System.out.println("\n📍 Current IST Time: " + syncService.getCurrentISTTime());
        
        // Print database stats
        syncService.printStats();
        
        // Print upcoming matches for next 3 days
        syncService.printUpcomingMatches(3);
        
        System.out.println("\n✅ ThePitch is ready!");
        System.out.println("========================================");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}
