package com.thepitch.dao;

import java.sql.*;

/**
 * DatabaseConnection - Manages database connection
 * Fixed version - keeps connection alive
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private String dbUrl;
    
    private DatabaseConnection() {
        this.dbUrl = "jdbc:sqlite:thepitch.db";
        connect();
        createTables();
    }
    
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("✅ Database connected successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }
    
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }
    
    private void createTables() {
        String createLeaguesTable = 
            "CREATE TABLE IF NOT EXISTS leagues (" +
            "league_id INTEGER PRIMARY KEY," +
            "league_name TEXT NOT NULL," +
            "country TEXT NOT NULL" +
            ")";
        
        String createTeamsTable = 
            "CREATE TABLE IF NOT EXISTS teams (" +
            "team_id INTEGER PRIMARY KEY," +
            "team_name TEXT NOT NULL," +
            "league_id INTEGER," +
            "elo_rating INTEGER DEFAULT 1500," +
            "last_updated TEXT" +
            ")";
        
        String createMatchesTable = 
            "CREATE TABLE IF NOT EXISTS matches (" +
            "match_id INTEGER PRIMARY KEY," +
            "match_date TEXT NOT NULL," +
            "home_team_id INTEGER," +
            "away_team_id INTEGER," +
            "league_id INTEGER," +
            "home_score INTEGER," +
            "away_score INTEGER," +
            "status TEXT DEFAULT 'SCHEDULED'" +
            ")";
        
        String createPredictionsTable = 
            "CREATE TABLE IF NOT EXISTS predictions (" +
            "prediction_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "match_id INTEGER," +
            "home_win_prob REAL," +
            "draw_prob REAL," +
            "away_win_prob REAL," +
            "confidence TEXT," +
            "predicted_winner TEXT," +
            "actual_result TEXT," +
            "accuracy_verified INTEGER DEFAULT 0," +
            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        String insertLeagues = 
            "INSERT OR IGNORE INTO leagues (league_id, league_name, country) VALUES " +
            "(2021, 'Premier League', 'England'), " +
            "(2014, 'La Liga', 'Spain'), " +
            "(2019, 'Serie A', 'Italy'), " +
            "(2002, 'Bundesliga', 'Germany'), " +
            "(2015, 'Ligue 1', 'France')";
        
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createLeaguesTable);
            stmt.execute(createTeamsTable);
            stmt.execute(createMatchesTable);
            stmt.execute(createPredictionsTable);
            stmt.execute(insertLeagues);
            System.out.println("✅ Database tables ready!");
        } catch (SQLException e) {
            System.err.println("❌ Error creating tables: " + e.getMessage());
        }
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
