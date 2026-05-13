package com.thepitch.service;

import com.thepitch.dao.AdminDAO;
import com.thepitch.dao.TeamDAO;
import com.thepitch.model.Team;

import java.sql.SQLException;
import java.util.*;

/**
 * AdminPanel — console UI for admin-only features.
 * Called from Main.java option 8 (xG Admin) and option 9 (Injury Manager).
 *
 * Features:
 *   Option 8 — Set / view / delete xGF and xGA per team per competition
 *   Option 9 — Add injured players, mark players fit, view all injuries
 */
public class AdminPanel {

    private final AdminDAO adminDAO;
    private final TeamDAO teamDAO;

    // Default competition and season — change here for new seasons
    public static final String DEFAULT_COMPETITION = "Premier League";
    public static final String DEFAULT_SEASON      = "2024-25";

    public AdminPanel() throws SQLException {
        this.adminDAO = new AdminDAO();
        this.teamDAO  = new TeamDAO();
    }

    // ════════════════════════════════════════════════════════════════════════
    // OPTION 8 — xG Admin Panel
    // ════════════════════════════════════════════════════════════════════════

    public void runXGAdmin(Scanner scanner) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  ADMIN — xG MANAGER");
        System.out.println("  Competition: " + DEFAULT_COMPETITION + " | Season: " + DEFAULT_SEASON);
        System.out.println("=".repeat(60));

        boolean running = true;
        while (running) {
            System.out.println("\n  1. View all xG entries");
            System.out.println("  2. Set xG for a team");
            System.out.println("  3. Delete xG entry");
            System.out.println("  4. Back to main menu");
            System.out.print("\n  Choose (1-4): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1": viewAllXG(); break;
                case "2": setTeamXG(scanner); break;
                case "3": deleteXGEntry(scanner); break;
                case "4": running = false; break;
                default:  System.out.println("  Invalid option.");
            }
        }
    }

    private void viewAllXG() {
        try {
            List<Map<String, Object>> entries = adminDAO.getAllXGEntries();
            if (entries.isEmpty()) {
                System.out.println("\n  No xG data entered yet.");
                System.out.println("  Use option 2 to add xG for teams.");
                return;
            }

            System.out.println("\n  " + "─".repeat(70));
            System.out.printf("  %-28s %-18s %-6s %-6s %-7s%n",
                "Team", "Competition", "xGF", "xGA", "Matches");
            System.out.println("  " + "─".repeat(70));

            for (Map<String, Object> row : entries) {
                System.out.printf("  %-28s %-18s %-6.2f %-6.2f %-7d%n",
                    row.get("team_name"),
                    row.get("competition"),
                    row.get("xgf"),
                    row.get("xga"),
                    row.get("matches"));
            }
            System.out.println("  " + "─".repeat(70));
            System.out.println("  " + entries.size() + " entries total.");
        } catch (SQLException e) {
            System.err.println("  DB error: " + e.getMessage());
        }
    }

    private void setTeamXG(Scanner scanner) {
        System.out.println("\n  SET xG FOR A TEAM");
        System.out.println("  (xGF = expected goals scored per game, xGA = expected goals conceded)");

        // Show team list
        List<Team> teams = teamDAO.getAllTeams();
        System.out.println("\n  Teams in database:");
        for (int i = 0; i < teams.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, teams.get(i).getTeamName());
        }

        System.out.print("\n  Enter team number (or type name directly): ");
        String teamInput = scanner.nextLine().trim();
        String teamName;

        try {
            int idx = Integer.parseInt(teamInput) - 1;
            if (idx < 0 || idx >= teams.size()) {
                System.out.println("  Invalid number.");
                return;
            }
            teamName = teams.get(idx).getTeamName();
        } catch (NumberFormatException e) {
            teamName = teamInput; // typed name directly
        }

        System.out.print("  Competition [" + DEFAULT_COMPETITION + "]: ");
        String comp = scanner.nextLine().trim();
        if (comp.isEmpty()) comp = DEFAULT_COMPETITION;

        System.out.print("  Season [" + DEFAULT_SEASON + "]: ");
        String season = scanner.nextLine().trim();
        if (season.isEmpty()) season = DEFAULT_SEASON;

        double xgf = promptDouble(scanner, "  xGF (goals scored per game, e.g. 1.85): ", 0, 5);
        double xga = promptDouble(scanner, "  xGA (goals conceded per game, e.g. 1.10): ", 0, 5);

        System.out.print("  Number of matches this is based on (e.g. 28): ");
        int matches = 0;
        try { matches = Integer.parseInt(scanner.nextLine().trim()); } catch (Exception ignored) {}

        // Confirm
        System.out.printf("%n  Saving: %s | %s | %s%n", teamName, comp, season);
        System.out.printf("    xGF = %.2f  |  xGA = %.2f  |  over %d matches%n", xgf, xga, matches);
        System.out.print("  Confirm? (y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            try {
                adminDAO.saveTeamXG(teamName, comp, season, xgf, xga, matches);
                System.out.println("  ✅ Saved!");
            } catch (SQLException e) {
                System.err.println("  ❌ Save failed: " + e.getMessage());
            }
        } else {
            System.out.println("  Cancelled.");
        }
    }

    private void deleteXGEntry(Scanner scanner) {
        try {
            List<Map<String, Object>> entries = adminDAO.getAllXGEntries();
            if (entries.isEmpty()) {
                System.out.println("  No xG entries to delete.");
                return;
            }

            System.out.println("\n  Select entry to delete:");
            for (int i = 0; i < entries.size(); i++) {
                Map<String, Object> e = entries.get(i);
                System.out.printf("  %2d. %-28s %s / %s  (xGF=%.2f xGA=%.2f)%n",
                    i + 1,
                    e.get("team_name"), e.get("competition"), e.get("season"),
                    e.get("xgf"), e.get("xga"));
            }

            System.out.print("\n  Entry number to delete: ");
            int idx;
            try { idx = Integer.parseInt(scanner.nextLine().trim()) - 1; }
            catch (NumberFormatException e) { System.out.println("  Invalid."); return; }

            if (idx < 0 || idx >= entries.size()) {
                System.out.println("  Invalid number.");
                return;
            }

            Map<String, Object> target = entries.get(idx);
            adminDAO.deleteTeamXG(
                (String) target.get("team_name"),
                (String) target.get("competition"),
                (String) target.get("season")
            );
            System.out.println("  ✅ Deleted: " + target.get("team_name") + " / " + target.get("competition"));

        } catch (SQLException e) {
            System.err.println("  DB error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // OPTION 9 — Injury Manager
    // ════════════════════════════════════════════════════════════════════════

    public void runInjuryManager(Scanner scanner) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  ADMIN — INJURY MANAGER");
        System.out.println("=".repeat(60));

        boolean running = true;
        while (running) {
            System.out.println("\n  1. View all current injuries");
            System.out.println("  2. View injuries for a team");
            System.out.println("  3. Add injured player");
            System.out.println("  4. Mark player fit (remove from list)");
            System.out.println("  5. Back to main menu");
            System.out.print("\n  Choose (1-5): ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1": viewAllInjuries(); break;
                case "2": viewTeamInjuries(scanner); break;
                case "3": addInjury(scanner); break;
                case "4": markPlayerFit(scanner); break;
                case "5": running = false; break;
                default:  System.out.println("  Invalid option.");
            }
        }
    }

    private void viewAllInjuries() {
        try {
            List<Map<String, Object>> injuries = adminDAO.getAllInjuries();
            if (injuries.isEmpty()) {
                System.out.println("\n  No injuries recorded.");
                return;
            }

            System.out.println("\n  " + "─".repeat(75));
            System.out.printf("  %-24s %-22s %-8s %-8s %-12s%n",
                "Team", "Player", "Tier", "Crit.", "Position");
            System.out.println("  " + "─".repeat(75));

            String lastTeam = "";
            for (Map<String, Object> row : injuries) {
                String team = (String) row.get("team_name");
                if (!team.equals(lastTeam)) {
                    if (!lastTeam.isEmpty()) System.out.println();
                    lastTeam = team;
                }
                System.out.printf("  %-24s %-22s %-8s %-8.1f %-12s%n",
                    team,
                    row.get("player_name"),
                    row.get("tier"),
                    row.get("position_criticality"),
                    row.get("position_label"));
            }
            System.out.println("  " + "─".repeat(75));
            System.out.println("  " + injuries.size() + " players injured across all teams.");
        } catch (SQLException e) {
            System.err.println("  DB error: " + e.getMessage());
        }
    }

    private void viewTeamInjuries(Scanner scanner) {
        String teamName = selectTeam(scanner);
        if (teamName == null) return;
        printTeamInjuries(teamName);
    }

    public void printTeamInjuries(String teamName) {
        try {
            List<Map<String, Object>> injuries = adminDAO.getTeamInjuries(teamName);
            System.out.println("\n  Injury list for: " + teamName);
            if (injuries.isEmpty()) {
                System.out.println("  No injuries recorded.");
                return;
            }
            System.out.println("  " + "─".repeat(65));
            System.out.printf("  %-22s %-8s %-8s %-12s %-10s%n",
                "Player", "Tier", "Crit.", "Position", "Since");
            System.out.println("  " + "─".repeat(65));
            for (Map<String, Object> row : injuries) {
                System.out.printf("  %-22s %-8s %-8.1f %-12s %-10s%n",
                    row.get("player_name"),
                    row.get("tier"),
                    row.get("position_criticality"),
                    row.get("position_label"),
                    row.get("added_date"));
                if (!((String)row.get("notes")).isEmpty()) {
                    System.out.println("  " + "  Note: " + row.get("notes"));
                }
            }
            System.out.println("  " + "─".repeat(65));
        } catch (SQLException e) {
            System.err.println("  DB error: " + e.getMessage());
        }
    }

    private void addInjury(Scanner scanner) {
        System.out.println("\n  ADD INJURED PLAYER");

        String teamName = selectTeam(scanner);
        if (teamName == null) return;

        // Show existing injuries for context
        printTeamInjuries(teamName);

        System.out.print("\n  Player name: ");
        String playerName = scanner.nextLine().trim();
        if (playerName.isEmpty()) { System.out.println("  Cancelled."); return; }

        // Tier selection
        System.out.println("\n  Player tier:");
        System.out.println("  1. elite   — top 3 player in squad (captain, star player)");
        System.out.println("  2. regular — first team regular (starts most games)");
        System.out.println("  3. squad   — rotation/backup player");
        System.out.print("  Choose tier (1-3): ");
        String tier;
        switch (scanner.nextLine().trim()) {
            case "1":  tier = "elite";   break;
            case "3":  tier = "squad";   break;
            default:   tier = "regular"; break;
        }

        // Position criticality
        System.out.println("\n  Position criticality:");
        System.out.println("  1. 1.5 — GK, striker, key creator (hardest to replace)");
        System.out.println("  2. 1.0 — full-back, central midfielder");
        System.out.println("  3. 0.5 — backup / utility player");
        System.out.print("  Choose criticality (1-3): ");
        double criticality;
        switch (scanner.nextLine().trim()) {
            case "1":  criticality = 1.5; break;
            case "3":  criticality = 0.5; break;
            default:   criticality = 1.0; break;
        }

        System.out.print("  Position label (e.g. Striker, GK, RB — press Enter to skip): ");
        String posLabel = scanner.nextLine().trim();

        System.out.print("  Notes (e.g. 'hamstring, 3 weeks' — press Enter to skip): ");
        String notes = scanner.nextLine().trim();

        // Show deduction impact
        double deduction = Math.min(getTierBase(tier) * criticality, 9.0);
        System.out.printf("%n  Injury impact: -%.1f points from team total%n", deduction);
        System.out.printf("  (tier=%s × criticality=%.1f = %.1f, capped at 9.0)%n",
            tier, criticality, deduction);

        System.out.print("  Confirm? (y/n): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("  Cancelled.");
            return;
        }

        try {
            adminDAO.addInjury(teamName, playerName, tier, criticality, posLabel, notes);
            System.out.println("  ✅ " + playerName + " added to " + teamName + " injury list.");
        } catch (SQLException e) {
            System.err.println("  ❌ Save failed: " + e.getMessage());
        }
    }

    private void markPlayerFit(Scanner scanner) {
        System.out.println("\n  MARK PLAYER FIT (remove from injury list)");

        String teamName = selectTeam(scanner);
        if (teamName == null) return;

        try {
            List<Map<String, Object>> injuries = adminDAO.getTeamInjuries(teamName);
            if (injuries.isEmpty()) {
                System.out.println("  No injuries for " + teamName);
                return;
            }

            System.out.println("\n  Current injuries for " + teamName + ":");
            for (int i = 0; i < injuries.size(); i++) {
                System.out.printf("  %2d. %-22s [%s, crit=%.1f]%n",
                    i + 1,
                    injuries.get(i).get("player_name"),
                    injuries.get(i).get("tier"),
                    injuries.get(i).get("position_criticality"));
            }

            System.out.print("\n  Enter player number to mark fit: ");
            int idx;
            try { idx = Integer.parseInt(scanner.nextLine().trim()) - 1; }
            catch (NumberFormatException e) { System.out.println("  Invalid."); return; }

            if (idx < 0 || idx >= injuries.size()) {
                System.out.println("  Invalid number.");
                return;
            }

            String playerName = (String) injuries.get(idx).get("player_name");
            adminDAO.removeInjury(teamName, playerName);
            System.out.println("  ✅ " + playerName + " marked fit and removed from injury list.");

        } catch (SQLException e) {
            System.err.println("  DB error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC helper — called from option 5 (prediction flow)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Used in option 5: loads saved injuries for a team, shows them, then
     * lets the user add new ones or mark players fit before the prediction runs.
     * Returns the final list of injury maps for that team.
     */
    public List<Map<String, Object>> reviewAndUpdateInjuries(String teamName, Scanner scanner) {
        System.out.println("\n  📋 Injury list for " + teamName + ":");

        try {
            List<Map<String, Object>> injuries = adminDAO.getTeamInjuries(teamName);

            if (injuries.isEmpty()) {
                System.out.println("  (none on record)");
            } else {
                for (int i = 0; i < injuries.size(); i++) {
                    System.out.printf("  %d. %-22s [%s, crit=%.1f]%n",
                        i + 1,
                        injuries.get(i).get("player_name"),
                        injuries.get(i).get("tier"),
                        injuries.get(i).get("position_criticality"));
                }
            }

            System.out.println("\n  Options:");
            System.out.println("  a — add a newly injured player");
            System.out.println("  r — remove a player (fit again)");
            System.out.println("  Enter — keep as is and continue");
            System.out.print("  Choice: ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("a")) {
                addInjuryQuick(teamName, scanner);
            } else if (choice.equals("r")) {
                removeInjuryQuick(teamName, scanner, injuries);
            }

            // Return final list after any changes
            return adminDAO.getTeamInjuries(teamName);

        } catch (SQLException e) {
            System.err.println("  DB error loading injuries: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void addInjuryQuick(String teamName, Scanner scanner) {
        System.out.print("  Player name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) return;

        System.out.println("  Tier: 1=elite  2=regular  3=squad");
        System.out.print("  Tier: ");
        String tier;
        switch (scanner.nextLine().trim()) {
            case "1": tier = "elite";   break;
            case "3": tier = "squad";   break;
            default:  tier = "regular"; break;
        }

        System.out.println("  Criticality: 1=1.5(GK/striker)  2=1.0(mid/back)  3=0.5(backup)");
        System.out.print("  Criticality: ");
        double crit;
        switch (scanner.nextLine().trim()) {
            case "1": crit = 1.5; break;
            case "3": crit = 0.5; break;
            default:  crit = 1.0; break;
        }

        try {
            adminDAO.addInjury(teamName, name, tier, crit, "", "");
            System.out.println("  ✅ " + name + " added.");
        } catch (SQLException e) {
            System.err.println("  ❌ Failed: " + e.getMessage());
        }
    }

    private void removeInjuryQuick(String teamName, Scanner scanner, List<Map<String, Object>> injuries) {
        if (injuries.isEmpty()) { System.out.println("  No injuries to remove."); return; }
        System.out.print("  Enter number to remove: ");
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= injuries.size()) { System.out.println("  Invalid."); return; }
            String name = (String) injuries.get(idx).get("player_name");
            adminDAO.removeInjury(teamName, name);
            System.out.println("  ✅ " + name + " removed (fit).");
        } catch (Exception e) {
            System.out.println("  Invalid input.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Shared helpers
    // ════════════════════════════════════════════════════════════════════════

    private String selectTeam(Scanner scanner) {
        List<Team> teams = teamDAO.getAllTeams();
        System.out.println("\n  Teams:");
        for (int i = 0; i < teams.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, teams.get(i).getTeamName());
        }
        System.out.print("  Select team number: ");
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= teams.size()) {
                System.out.println("  Invalid."); return null;
            }
            return teams.get(idx).getTeamName();
        } catch (NumberFormatException e) {
            System.out.println("  Invalid."); return null;
        }
    }

    private double promptDouble(Scanner scanner, String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            try {
                double val = Double.parseDouble(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.println("  Please enter a value between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid number.");
            }
        }
    }

    private double getTierBase(String tier) {
        switch (tier.toLowerCase()) {
            case "elite":   return 6.0;
            case "regular": return 4.0;
            default:        return 2.0;
        }
    }
}
