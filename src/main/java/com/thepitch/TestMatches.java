package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.service.DataSyncService;
import com.thepitch.model.Match;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestMatches {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("     CHECKING UPCOMING MATCHES          ");
        System.out.println("========================================\n");
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        
        SimpleDateFormat istFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        istFormat.setTimeZone(TimeZone.getTimeZone("IST"));
        
        System.out.println("Current IST Date: " + istFormat.format(new Date()));
        System.out.println("========================================\n");
        
        // Check matches for next 3 days
        syncService.printUpcomingMatches(3);
        
        System.out.println("\n========================================");
        System.out.println("To sync fresh data, run: mvn exec:java");
        System.out.println("========================================");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}
