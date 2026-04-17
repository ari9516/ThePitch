package com.thepitch.model;

public class RecentForm {
    public String teamName;
    public String formString;      // e.g., "WWDLW"
    public double pointsFromLast5; // 0-15
    public double formPercentage;   // 0-100
    public int goalsScored;
    public int goalsConceded;
    public int cleanSheets;
    public int failedToScore;
    public int bttsCount;
    public String streak;           // e.g., "🔥 3+ WINS IN A ROW"
    public String trend;            // IMPROVING, DECLINING, STABLE
}
