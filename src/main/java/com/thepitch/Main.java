package com.thepitch;

import com.thepitch.dao.DatabaseConnection;
import com.thepitch.service.DataSyncService;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("         THEPITCH - PREMIER LEAGUE        ");
        System.out.println("========================================\n");
        
        DatabaseConnection db = DatabaseConnection.getInstance();
        DataSyncService syncService = new DataSyncService();
        
        System.out.println("📍 " + syncService.getCurrentISTTime());
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n📥 OPTIONS:");
        System.out.println("   1. Sync Premier League data from API");
        System.out.println("   2. Show LAST 5 MATCHWEEKS (with scores)");
        System.out.println("   3. Show UPCOMING MATCHES");
        System.out.println("   4. Show Statistics");
        System.out.print("\nChoose option (1-4): ");
        
        int choice = scanner.nextInt();
        
        if (choice == 1) {
            syncService.syncPremierLeague();
        } else if (choice == 2) {
            syncService.printStats();
            syncService.showRecentMatches(5);
        } else if (choice == 3) {
            syncService.showUpcomingMatches();
        } else if (choice == 4) {
            syncService.printStats();
        }
        
        System.out.println("\n✅ ThePitch is ready!");
        System.out.println("========================================\n");
        
        scanner.close();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.closeConnection();
        }));
    }
}
