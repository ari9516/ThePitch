-- =====================================================
-- THEPITCH DATABASE SCHEMA
-- Football Analytics Application
-- =====================================================

-- =====================================================
-- 1. LEAGUES TABLE
-- Stores information about football leagues
-- =====================================================
CREATE TABLE IF NOT EXISTS leagues (
    league_id INTEGER PRIMARY KEY,
    league_name TEXT NOT NULL,
    country TEXT NOT NULL,
    season INTEGER
);

-- =====================================================
-- 2. TEAMS TABLE
-- Stores team information and ELO ratings
-- =====================================================
CREATE TABLE IF NOT EXISTS teams (
    team_id INTEGER PRIMARY KEY,
    team_name TEXT NOT NULL,
    league_id INTEGER,
    elo_rating INTEGER DEFAULT 1500,
    last_updated TEXT,
    FOREIGN KEY (league_id) REFERENCES leagues(league_id)
);

-- =====================================================
-- 3. MATCHES TABLE
-- Stores all match information and results
-- =====================================================
CREATE TABLE IF NOT EXISTS matches (
    match_id INTEGER PRIMARY KEY,
    match_date TEXT NOT NULL,
    home_team_id INTEGER,
    away_team_id INTEGER,
    league_id INTEGER,
    home_score INTEGER,
    away_score INTEGER,
    status TEXT DEFAULT 'SCHEDULED',
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (home_team_id) REFERENCES teams(team_id),
    FOREIGN KEY (away_team_id) REFERENCES teams(team_id),
    FOREIGN KEY (league_id) REFERENCES leagues(league_id)
);

-- =====================================================
-- 4. PREDICTIONS TABLE
-- Stores all predictions made by the system
-- =====================================================
CREATE TABLE IF NOT EXISTS predictions (
    prediction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id INTEGER,
    home_win_prob REAL,
    draw_prob REAL,
    away_win_prob REAL,
    confidence TEXT,
    predicted_winner TEXT,
    actual_result TEXT,
    accuracy_verified INTEGER DEFAULT 0,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(match_id)
);

-- =====================================================
-- 5. INITIAL DATA
-- Insert the top 5 European leagues
-- =====================================================
INSERT OR IGNORE INTO leagues (league_id, league_name, country) VALUES
(2021, 'Premier League', 'England'),
(2014, 'La Liga', 'Spain'),
(2019, 'Serie A', 'Italy'),
(2002, 'Bundesliga', 'Germany'),
(2015, 'Ligue 1', 'France');

-- =====================================================
-- 6. INDEXES FOR PERFORMANCE
-- Improves query speed for common searches
-- =====================================================

-- Index for searching matches by date
CREATE INDEX IF NOT EXISTS idx_matches_date ON matches(match_date);

-- Index for searching matches by league
CREATE INDEX IF NOT EXISTS idx_matches_league ON matches(league_id);

-- Index for searching matches by status
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status);

-- Index for searching teams by league
CREATE INDEX IF NOT EXISTS idx_teams_league ON teams(league_id);

-- Index for searching predictions by match
CREATE INDEX IF NOT EXISTS idx_predictions_match ON predictions(match_id);

-- =====================================================
-- 7. HELPER VIEWS (Optional - for easier queries)
-- =====================================================

-- View: Upcoming matches with team names
CREATE VIEW IF NOT EXISTS upcoming_matches AS
SELECT 
    m.match_id,
    m.match_date,
    ht.team_name AS home_team,
    at.team_name AS away_team,
    l.league_name,
    m.status
FROM matches m
JOIN teams ht ON m.home_team_id = ht.team_id
JOIN teams at ON m.away_team_id = at.team_id
JOIN leagues l ON m.league_id = l.league_id
WHERE m.status = 'SCHEDULED' 
  AND date(m.match_date) >= date('now')
ORDER BY m.match_date;

-- View: Recent matches with results
CREATE VIEW IF NOT EXISTS recent_results AS
SELECT 
    m.match_id,
    m.match_date,
    ht.team_name AS home_team,
    at.team_name AS away_team,
    m.home_score,
    m.away_score,
    l.league_name,
    CASE 
        WHEN m.home_score > m.away_score THEN ht.team_name
        WHEN m.away_score > m.home_score THEN at.team_name
        ELSE 'Draw'
    END AS winner
FROM matches m
JOIN teams ht ON m.home_team_id = ht.team_id
JOIN teams at ON m.away_team_id = at.team_id
JOIN leagues l ON m.league_id = l.league_id
WHERE m.status = 'FINISHED'
ORDER BY m.match_date DESC
LIMIT 50;

-- View: Team ELO rankings
CREATE VIEW IF NOT EXISTS team_rankings AS
SELECT 
    team_id,
    team_name,
    league_id,
    elo_rating,
    RANK() OVER (PARTITION BY league_id ORDER BY elo_rating DESC) AS rank_in_league
FROM teams
ORDER BY league_id, elo_rating DESC;

-- =====================================================
-- 8. HELPER QUERIES (Examples - commented out)
-- =====================================================

-- Get all matches for today:
-- SELECT * FROM matches WHERE date(match_date) = date('now');

-- Get predictions with confidence HIGH:
-- SELECT * FROM predictions WHERE confidence = 'HIGH';

-- Get team with highest ELO rating:
-- SELECT * FROM teams ORDER BY elo_rating DESC LIMIT 1;

-- Get accuracy rate of predictions:
-- SELECT 
--     COUNT(*) as total_predictions,
--     SUM(CASE WHEN predicted_winner = actual_result THEN 1 ELSE 0 END) as correct,
--     ROUND(100.0 * SUM(CASE WHEN predicted_winner = actual_result THEN 1 ELSE 0 END) / COUNT(*), 2) as accuracy_percent
-- FROM predictions WHERE accuracy_verified = 1;

-- =====================================================
-- END OF SCHEMA
-- =====================================================