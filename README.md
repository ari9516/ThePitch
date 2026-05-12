<div align="center">

<img src="https://img.shields.io/badge/⚽-ThePitch-1a1a2e?style=for-the-badge&labelColor=16213e" alt="ThePitch"/>

# ⚽ ThePitch

### *Intelligent Football Match Prediction Engine*

> Transform raw Premier League data into precise, data-driven match outcome predictions — powered by ELO ratings, AI tactical analysis, real-time statistics, and a 12-factor weighted prediction model.

<br/>

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://java.com)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?style=flat-square&logo=apache-maven&logoColor=white)](https://maven.apache.org)
[![SQLite](https://img.shields.io/badge/SQLite-Embedded-003B57?style=flat-square&logo=sqlite&logoColor=white)](https://sqlite.org)
[![Python](https://img.shields.io/badge/Python-3.10%2B-3776AB?style=flat-square&logo=python&logoColor=white)](https://python.org)
[![Claude AI](https://img.shields.io/badge/Claude_AI-Integrated-D97757?style=flat-square)](https://anthropic.com)
[![License](https://img.shields.io/badge/License-MIT-22c55e?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Active_Development-f59e0b?style=flat-square)]()

<br/>

[Features](#-key-features) • [Architecture](#-architecture) • [Prediction Model](#-prediction-model) • [Quick Start](#-quick-start) • [Usage](#-usage-guide) • [Admin Panel](#-admin-panel) • [Roadmap](#-roadmap)

---

</div>

## 🎯 What is ThePitch?

**ThePitch** is a desktop football analytics application built for the Premier League. It ingests live match data, runs it through a multi-layered statistical engine, and outputs calibrated win/draw/loss probabilities — complete with AI-generated tactical analysis, persistent injury tracking, and xG-based performance profiling.

It is not a betting bot. It is an **analyst's workstation** — designed to surface the signal hidden in football data.

```
Manchester City vs Arsenal — Matchday 35

🏠 HOME PROFILE (Man City)           ✈️  AWAY PROFILE (Arsenal)
   xG/game:    2.14                     xG/game:    1.87
   Form:       W W W D W (87%)          Form:       W D W W W (93%)
   Shots/game: 15.2                     Shots/game: 14.1
   Home xGA:   0.81/game                Away xGA:   0.94/game

🧠 AI TACTICAL ANALYSIS
   City's high press targets Arsenal's build-up from deep.
   Saka vs Gvardiol — pace advantage could decide the match.
   KEY FACTOR: Arsenal's counter-attack speed vs City's defensive line.

   Home Tactical Score: 6.8 / 10
   Away Tactical Score: 7.2 / 10

🔮 PREDICTION
┌────────────────────────────────────────────────────────────────────┐
│   HOME WIN      │       DRAW       │      AWAY WIN                 │
│    38.4%        │      21.3%       │       40.3%                   │
└────────────────────────────────────────────────────────────────────┘
✅ CONFIDENCE: HIGH (72%) — Driven by Arsenal's superior away xG record
```

---

## 📊 Key Features

### 🔮 12-Factor Prediction Engine
The core model scores 12 independent factors (0–10 per team), applies tier-weighted contributions, adds a calibrated home advantage bonus, and derives win probabilities using a Bradley-Terry pairwise model. Draws are modelled inversely to the score spread (10–35% range). Confidence is a function of the absolute home/away gap.

| Tier | Factor | Weight | Source |
|------|--------|--------|--------|
| T1 | Recent Form (decay-weighted) | 20 pts | Match results DB |
| T1 | Home/Away Specific Form | 18 pts | Match results DB |
| T1 | xG Quality (xGF vs xGA) | 18 pts | Admin-entered + CSV |
| T1 | Head-to-Head (same venue) | 15 pts | Match results DB |
| T2 | Injuries (per-player deduction) | 12 pts | Persistent injury list |
| T2 | Tactical Matchup | 10 pts | Claude AI + Admin |
| T2 | Match Stakes | 10 pts | MatchContextAnalyzer |
| T2 | Referee Tendency | 8 pts | Admin input |
| T2 | Fatigue / Schedule Load | 8 pts | ExternalFactors |
| T3 | Weather Conditions | 5 pts | Admin input |
| T3 | Venue / Travel | 4 pts | ELO home advantage |
| T3 | Derby / Rivalry Factor | 3 pts | MatchContextAnalyzer |

### 🧠 AI-Powered Tactical Analysis
- Calls **Claude AI (Anthropic API)** to generate a match-specific tactical breakdown
- AI receives real data: ELO ratings, xG, form strings, shots, goals, trends, and stakes
- Output includes: strengths/weaknesses, key mismatch factor, suggested tactical scores
- Admin reviews AI output → **accept / tweak / override** → score saved to SQLite
- Saved analyses are **auto-loaded** next time the same fixture appears

### 🏥 Persistent Injury Tracker
- Injuries stored per team in SQLite — **survive across sessions**
- Per-player configuration: tier (`elite` / `regular` / `squad`) + position criticality (0.5 / 1.0 / 1.5)
- Point deduction formula: `min(tier_base × criticality, 9.0)` applied directly to team total
- Before every prediction: saved list is displayed, admin can **add newly injured** or **mark players fit**

### 📈 Team Performance Profiling
- Home profile vs Away profile tracked separately
- Per-match stats from imported CSV: shots, shots on target, corners, fouls, yellow/red cards, xG
- Streak detection: BTTS, Over 2.5, clean sheets, scoring runs (≥80% threshold)
- Falls back to calibrated league-average constants (not random) when CSV not yet imported

### 🏆 Match Context Intelligence
- Auto-detects: title race, top-4 battle, European qualification, relegation six-pointer, derbies
- Stakes weight adjusts the prediction model's sensitivity to form vs ELO
- Derby flag boosts draw probability automatically

### 🗄️ Live Data + Local Storage
- Real-time Premier League data: 380 matches, 20 teams via **football-data.org API**
- SQLite embedded database — no server setup, zero configuration
- Tables: `teams`, `matches`, `match_stats`, `team_xg`, `injury_list`, `tactical_analysis`

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         THEPITCH APPLICATION                             │
├──────────────────────────┬──────────────────────────────────────────────┤
│     CONSOLE UI           │            ADMIN PANEL                       │
│  Main.java (9 options)   │  AdminPanel.java                             │
│                          │  ├── xG Manager (Option 8)                   │
│                          │  └── Injury Manager (Option 9)               │
├──────────────────────────┴──────────────────────────────────────────────┤
│                         SERVICE LAYER                                    │
│                                                                          │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐   │
│  │ DataSyncService │  │  TacticalAnalyzer │  │  CSVImporter         │   │
│  │ (API → SQLite)  │  │  (Claude AI API)  │  │  (football-data CSV) │   │
│  └─────────────────┘  └──────────────────┘  └──────────────────────┘   │
│                                                                          │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐   │
│  │  EloCalculator  │  │  PredictionEngine │  │ EnhancedPrediction   │   │
│  │  (K=32, home    │  │  (basic ELO+form) │  │ Engine (12-factor)   │   │
│  │   advantage)    │  └──────────────────┘  └──────────────────────┘   │
│  └─────────────────┘                                                    │
├──────────────────────────────────────────────────────────────────────────┤
│                         ANALYZER LAYER                                   │
│                                                                          │
│  MatchContextAnalyzer  │  TeamProfileAnalyzer  │  RecentFormAnalyzer    │
│  (stakes, derbies)     │  (real SQLite queries) │  (last 5, streaks)    │
├──────────────────────────────────────────────────────────────────────────┤
│                         DAO LAYER                                        │
│                                                                          │
│  TeamDAO  │  MatchDAO  │  AdminDAO  │  DatabaseConnection (singleton)    │
├──────────────────────────────────────────────────────────────────────────┤
│                         DATA LAYER                                       │
│                                                                          │
│  SQLite DB              │  football-data.org API  │  Claude AI API       │
│  (thepitch.db)          │  (10 req/min free)      │  (claude-sonnet-4)   │
│  teams, matches,        │  380 matches, 20 teams  │  tactical analysis   │
│  match_stats, xg,       │  live fixtures          │  per fixture         │
│  injuries, tactical     │                         │                      │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
ThePitch/
│
├── src/main/java/com/thepitch/
│   │
│   ├── Main.java                          # Entry point — 9-option console menu
│   │
│   ├── model/
│   │   ├── Team.java                      # Team entity (ELO, form, goals)
│   │   ├── Match.java                     # Match entity (teams, scores, status)
│   │   ├── Prediction.java                # Prediction output model
│   │   ├── TeamProfile.java               # Home/away stats (xG, shots, corners)
│   │   ├── RecentForm.java                # Last 5 matches analysis
│   │   └── ExternalFactors.java           # Weather, fatigue, tactical, referee scores
│   │
│   ├── dao/
│   │   ├── DatabaseConnection.java        # SQLite singleton connection pool
│   │   ├── TeamDAO.java                   # Team CRUD
│   │   ├── MatchDAO.java                  # Match CRUD
│   │   └── AdminDAO.java                  # xG, injury list, tactical analysis tables
│   │
│   ├── service/
│   │   ├── APIClient.java                 # football-data.org HTTP calls
│   │   ├── DataSyncService.java           # Sync API → SQLite
│   │   ├── EloCalculator.java             # ELO ratings (K=32, home advantage)
│   │   ├── PredictionEngine.java          # Basic ELO + form prediction
│   │   ├── CSVImporter.java               # Imports pl_match_stats.csv → SQLite
│   │   ├── AdminPanel.java                # Console UI for xG + injury management
│   │   └── TacticalAnalyzer.java          # Claude AI tactical analysis engine
│   │
│   └── analyzer/
│       ├── MatchContextAnalyzer.java      # Stakes, derby, title race detection
│       ├── TeamProfileAnalyzer.java       # Profile builder (real SQLite queries)
│       ├── RecentFormAnalyzer.java        # Form + streak detection
│       └── EnhancedPredictionEngine.java  # 12-factor weighted model
│
├── src/main/resources/
│   └── config.properties                  # API keys (football-data + Anthropic)
│
├── scripts/
│   ├── fetch_fbref_stats.py               # Stats downloader (no Selenium)
│   ├── match_prediction_engine.py         # Python 12-factor model reference
│   ├── sofascore_correct.py               # Team/player search
│   └── sofascore_match_data.py            # H2H data via SofaScore
│
├── database/
│   └── schema.sql                         # All table definitions
│
├── data/                                  # CSV files (generated by Python scripts)
│   ├── pl_match_stats.csv                 # Per-match per-team: shots, xG, corners, cards
│   └── pl_schedule.csv                    # Full fixture list
│
├── thepitch.db                            # SQLite database (auto-created on first run)
├── pom.xml                                # Maven config (Java 17, OkHttp, Gson, OpenCSV)
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17+ | Required |
| Maven | 3.9+ | Required |
| Python | 3.10+ | For stats fetching scripts |
| Chrome (optional) | Any | Not needed — scripts use direct HTTP |

### 1. Clone & Build

```bash
git clone https://github.com/ari9516/ThePitch.git
cd ThePitch
mvn clean compile
```

### 2. Configure API Keys

Edit `src/main/resources/config.properties`:

```properties
# football-data.org (free — register at football-data.org/register)
api.key=YOUR_FOOTBALL_DATA_KEY

# Anthropic Claude AI (for tactical analysis — claude.ai/settings/keys)
anthropic.api.key=sk-ant-YOUR_KEY_HERE
```

### 3. Run

```bash
mvn exec:java
```

### 4. First-time Setup (in order)

```
Option 1  →  Sync Premier League data (fetches 380 matches, 20 teams)
Option 7  →  Import match stats CSV (after running the Python script below)
Option 8  →  Enter xG data for teams you want to predict
Option 5  →  Generate your first prediction
```

### 5. Fetch Detailed Stats (Optional but recommended)

Download the Premier League stats CSV directly in your browser:
```
https://www.football-data.co.uk/mmz4281/2425/E0.csv
```
Save as `data/pl_match_stats_raw.csv`, then run:
```bash
python scripts/fetch_fbref_stats.py
```
Then choose **Option 7** in the app to import it.

---

## 📋 Usage Guide

### Main Menu

| Option | Name | Description |
|--------|------|-------------|
| `1` | Sync Data | Fetches latest PL matches, results, fixtures from API |
| `2` | Recent Results | Shows last 5 matchweeks with scores |
| `3` | Upcoming Fixtures | Scheduled matches with dates |
| `4` | Statistics | Database overview (teams, matches, sync status) |
| `5` | **Enhanced Prediction** | Full 12-factor prediction with AI tactical analysis |
| `6` | Team Profile | Home/away stats, form, streak alerts for any team |
| `7` | Import CSV | Loads shots/corners/cards/xG from downloaded CSV |
| `8` | **[ADMIN] xG Manager** | Set xGF/xGA per team per competition/season |
| `9` | **[ADMIN] Injury Manager** | Add/remove players from persistent injury list |

---

## 🛡️ Admin Panel

The Admin Panel is where you, as the analyst, feed the engine with knowledge it can't derive from the API alone.

### Option 8 — xG Manager

Set expected goals data manually per team, per competition, per season. This directly feeds the `xg_quality` factor (18pts weight — highest in Tier 1).

```
  SET xG FOR A TEAM
  ──────────────────────────────────────────────
  Team:         Arsenal
  Competition:  Premier League
  Season:       2024-25
  xGF:          1.92  (goals expected to score per game)
  xGA:          0.88  (goals expected to concede per game)
  Matches:      32

  ✅ Saved!
```

### Option 9 — Injury Manager

Full persistent injury tracking. Players stay on the list across sessions until you mark them fit.

```
  Current injuries for Arsenal:
  ─────────────────────────────────────────────────────────
  1. Bukayo Saka        [elite,   crit=1.5]  → -9.0 pts
  2. Martin Odegaard    [elite,   crit=1.5]  → -9.0 pts
  3. Jurrien Timber     [regular, crit=1.0]  → -4.0 pts
  ─────────────────────────────────────────────────────────

  Options:
  a — add a newly injured player
  r — remove a player (fit again)
  Enter — keep as is and continue
```

**Injury deduction formula:**

| Tier | Base Deduction | Criticality 0.5 | Criticality 1.0 | Criticality 1.5 |
|------|---------------|-----------------|-----------------|-----------------|
| `elite` | 6.0 | −3.0 pts | −6.0 pts | **−9.0 pts** |
| `regular` | 4.0 | −2.0 pts | −4.0 pts | **−6.0 pts** |
| `squad` | 2.0 | −1.0 pts | −2.0 pts | **−3.0 pts** |

### Option 5 — AI Tactical Analysis (inside prediction flow)

When running a prediction, you are prompted to run the AI tactical analysis:

```
   Run AI tactical analysis? (Claude AI analyses formations & style)
   (y/n) [y]: y

   Generating tactical analysis via Claude AI...
   (This takes ~5-10 seconds)

────────────────────────────────────────────────────────────
  AI ANALYSIS:
────────────────────────────────────────────────────────────
  TACTICAL ASSESSMENT:
  City's high block and transitions exploit Arsenal's
  tendency to over-commit fullbacks in attack. Haaland's
  movement against White and Timber creates width issues.

  Arsenal's press from the front targets City's slower
  CB distribution. Havertz's intelligent runs between
  the lines are their primary threat on transitions.

  KEY FACTOR: Arsenal's press success rate vs City's
  ability to play through it under pressure.

  HOME_TACTICAL_SCORE: 6.2
  AWAY_TACTICAL_SCORE: 7.1
  REASONING: Arsenal's pressing game is more suited to
  disrupting City's build-up than City's block is to
  neutralising Arsenal's counter-press.
────────────────────────────────────────────────────────────

  AI suggested scores → Home: 6.2  |  Away: 7.1

  Your options:
  a — accept AI scores as-is
  m — manually set scores (override AI)
  t — tweak (adjust AI scores slightly)
  Choice [a]:
```

---

## 🛠️ Technology Stack

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Language** | Java 17 | Core application |
| **Build** | Maven 3.9+ | Dependency management |
| **Database** | SQLite (JDBC) | Local persistent storage |
| **HTTP** | OkHttp 4.x | football-data.org API calls |
| **HTTP (Admin)** | Java HttpURLConnection | Anthropic API calls (no extra dependency) |
| **JSON** | Gson | API response parsing |
| **CSV** | OpenCSV | Match stats import |
| **AI** | Claude claude-sonnet-4 | Tactical match analysis |
| **Scripting** | Python 3.10+ | Stats fetching, data processing |
| **Football Data** | football-data.org | Live PL data (free tier) |

---

## 📈 Data Flow

```
football-data.org API
       │
       ▼
DataSyncService.java ──────────────────────────────────┐
       │                                                │
       ▼                                               ▼
  teams table                                    matches table
  (20 PL teams,                               (380 matches,
   ELO ratings)                                scores, dates)
       │                                                │
       │◄──── AdminDAO.java ◄──── Admin Panel           │
       │       (team_xg,                                │
       │        injury_list,                            │
       │        tactical_analysis)                      │
       │                                                │
       ├────────────────────────────────────────────────┤
       │                                                │
       ▼                                                ▼
TeamProfileAnalyzer              RecentFormAnalyzer
(real SQLite queries             (last 5 W/D/L,
 for xG, shots, corners)          streak detection)
       │                                                │
       └───────────────────┬────────────────────────────┘
                           │
                           ▼
              EnhancedPredictionEngine
              (12-factor weighted model)
                           │
                           ▼
                    Prediction output
              (home%, draw%, away%, confidence,
               scorelines, key insights)
```

---

## 🔧 Configuration Reference

`src/main/resources/config.properties`

```properties
# ── Football Data API ─────────────────────────────────────────
# Free tier: 10 requests/minute
# Register: https://www.football-data.org/register
api.key=YOUR_KEY_HERE

# ── Anthropic Claude AI ───────────────────────────────────────
# Used for tactical analysis in Option 5
# Get key: https://console.anthropic.com/settings/keys
# Leave blank to skip AI analysis (falls back to manual input)
anthropic.api.key=sk-ant-YOUR_KEY_HERE
```

---

## 🗺️ Roadmap

| Status | Feature |
|--------|---------|
| ✅ | Core Java app + SQLite |
| ✅ | football-data.org API sync |
| ✅ | ELO engine (K=32, home advantage) |
| ✅ | Enhanced 5-factor prediction |
| ✅ | CSV importer for detailed stats |
| ✅ | Admin xG manager (Option 8) |
| ✅ | Persistent injury tracker (Option 9) |
| ✅ | Claude AI tactical analysis |
| ✅ | 12-factor prediction model (Python) |
| 🔄 | Java bridge to Python 12-factor engine |
| 🔄 | Accuracy tracking (store + evaluate predictions) |
| 🔄 | Auto ELO recalculation after each matchweek |
| ⏳ | Swing GUI (desktop interface) |
| ⏳ | Scoreline probability estimator |
| ⏳ | Packaged Windows installer (.exe) |

---

## 🤝 Contributing

Contributions, issues and feature requests are welcome.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## ⚠️ Disclaimer

ThePitch is built for **educational and analytical purposes only**. Statistical models are tools for reasoning about uncertainty — they are not oracles. Match outcomes depend on countless unpredictable variables. Never use this or any prediction model as the sole basis for financial decisions.

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.

---

## 👤 Author

**Arnab Kumar**
GitHub: [@ari9516](https://github.com/ari9516)
Project: [github.com/ari9516/ThePitch](https://github.com/ari9516/ThePitch)

---

## 🙏 Acknowledgements

- [football-data.org](https://www.football-data.org) — live Premier League data API
- [Anthropic](https://anthropic.com) — Claude AI for tactical analysis
- [football-data.co.uk](https://www.football-data.co.uk) — historical match statistics CSV
- [SofaScore](https://www.sofascore.com) — team and player data
- [SQLite](https://www.sqlite.org) — embedded database engine
- [Apache Maven](https://maven.apache.org) — build and dependency management

---

<div align="center">

Made with ⚽ and Java by [@ari9516](https://github.com/ari9516)

*"Football is a simple game made complicated by people who should know better." — Bill Shankly*

</div>

