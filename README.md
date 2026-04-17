# ⚽ ThePitch

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://java.com)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-red.svg)](https://maven.apache.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Database](https://img.shields.io/badge/Database-SQLite-blue.svg)](https://sqlite.org)

> **A sophisticated football analytics engine that transforms raw match data into intelligent match outcome predictions.**

ThePitch is a desktop application that empowers sports analysts with data-driven insights. By aggregating live data from official football APIs and applying advanced statistical models (ELO ratings, form analysis, head-to-head records, and contextual factors), it delivers accurate match predictions with actionable insights.

---

## 📊 Key Features

### 🎯 **Enhanced Prediction Engine**
- Multi-factor prediction model combining ELO ratings, recent form, home/away statistics, and external factors
- Real-time probability calculations for Home Win, Draw, and Away Win
- Confidence scoring with detailed reasoning

### 📈 **Comprehensive Team Analysis**
- **Home/Away performance profiles** - Goals, corners, cards, shots, possession
- **Form tracking** - Last 5 matches with trend detection (improving/declining)
- **Streak alerts** - 90%+ occurrence detection for corners, BTTS, over 2.5 goals
- **Head-to-head history** - Historical matchup analysis

### 🏆 **Match Context Intelligence**
- Automatic stake detection (Title race, European qualification, Relegation battle)
- Derby match identification
- Weighted probability adjustments based on match importance

### 🌐 **Live Data Integration**
- Real-time Premier League data via football-data.org API
- Team/player search via SofaScore integration
- CSV import support for detailed match statistics

### 🗄️ **Persistent Storage**
- SQLite database for local data persistence
- 380+ matches, 20 teams per season
- Historical result tracking for accuracy verification

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        THEPITCH APPLICATION                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   UI Layer  │  │ Service Layer│  │   DAO Layer │             │
│  │   (Swing)   │→│ (Business    │→│  (Database) │             │
│  │             │  │   Logic)     │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│         │               │               │                       │
│         ▼               ▼               ▼                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    DATA SOURCES                          │   │
│  │  • football-data.org API  • SofaScore Wrapper           │   │
│  │  • CSV Imports            • SQLite Database             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧠 Prediction Model Factors

| Factor | Weight | Data Source |
|--------|--------|-------------|
| **ELO Rating** | 45% | Historical match results |
| **Recent Form** | 25% | Last 5 matches performance |
| **Home/Away Stats** | 15% | Season home/away records |
| **Head-to-Head** | 10% | Previous encounters |
| **External Factors** | 5% | Injuries, weather, fatigue |

---

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.9+**
- **SQLite** (embedded, no installation required)

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/ThePitch.git
cd ThePitch

# Build the project
mvn clean compile

# Run the application
mvn exec:java
```

### API Key Setup

1. Register for a free API key at [football-data.org](https://www.football-data.org/register)
2. Copy your API key to `src/main/resources/config.properties`:

```properties
api.key=YOUR_API_KEY_HERE
```

---

## 📋 Usage Guide

### Main Menu Options

| Option | Description |
|--------|-------------|
| **1. Sync Premier League Data** | Fetch latest matches, teams, and results |
| **2. Last 5 Matchweeks** | View recent results with scores |
| **3. Upcoming Fixtures** | See scheduled matches |
| **4. Statistics** | Database overview |
| **5. Enhanced Prediction** | Generate AI-powered match prediction |
| **6. Team Profile** | Analyze home/away performance |

### Generating a Prediction

1. Select **Option 5** from the main menu
2. Choose an upcoming match from the list
3. (Optional) Enter external factors:
   - Team injuries
   - Weather conditions
   - Days since last match
4. Receive detailed prediction with:
   - Probability percentages
   - Confidence level
   - Key insights and trends

### Sample Output

```
══════════════════════════════════════════════════════════════════════
                    MATCH PREDICTION: Manchester City vs Arsenal
══════════════════════════════════════════════════════════════════════

📊 MATCH CONTEXT: 🏆 TITLE RACE - 3 points behind 4th

🏠 MANCHESTER CITY - HOME PROFILE
──────────────────────────────────────────────────────────────────────
Goals: 2.4 scored/game | 0.7 conceded/game
Corners: 6.2/game | Cards: 2.1/game
Shots: 14.8/game | On target: 5.9 (40%)

🔮 PREDICTION
──────────────────────────────────────────────────────────────────────
│  HOME WIN    │     DRAW     │    AWAY WIN    │
│    47.0%     │    18.7%     │     34.3%     │

✅ CONFIDENCE: MEDIUM (47%)
══════════════════════════════════════════════════════════════════════
```

---

## 📁 Project Structure

```
ThePitch/
├── src/main/java/com/thepitch/
│   ├── Main.java                 # Application entry point
│   ├── model/                    # Data models (Team, Match, Prediction)
│   ├── dao/                      # Database access objects
│   ├── service/                  # Business logic (API, sync, ELO, predictions)
│   └── analyzer/                 # Enhanced prediction analyzers
├── src/main/resources/
│   └── config.properties         # API keys and configuration
├── database/
│   └── schema.sql                # Database schema
├── scripts/                      # Python utilities for data scraping
├── pom.xml                       # Maven configuration
└── README.md
```

---

## 🛠️ Technology Stack

| Category | Technologies |
|----------|--------------|
| **Language** | Java 17 |
| **Build Tool** | Maven 3.9+ |
| **Database** | SQLite (JDBC) |
| **HTTP Client** | OkHttp |
| **JSON Parsing** | Gson |
| **CSV Processing** | OpenCSV, Apache Commons CSV |
| **UI Framework** | Swing (extensible to JavaFX) |
| **External APIs** | football-data.org, SofaScore |

---

## 📈 Data Sources

| Source | Data Type | Free Tier Limits |
|--------|-----------|------------------|
| **football-data.org** | Match results, schedules, standings | 10 req/min |
| **SofaScore** | Team/player search, H2H records | Wrapper-based |
| **FotMob (CSV)** | Detailed match statistics | Manual export |

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 📧 Contact

Arnab Kumar - [@ari9516](https://github.com/ari9516)

Project Link: [https://github.com/ari9516/ThePitch](https://github.com/ari9516/ThePitch)

---

## 🙏 Acknowledgements

- [football-data.org](https://www.football-data.org) for match data API
- [SofaScore](https://www.sofascore.com) for team/player data
- [FotMob](https://www.fotmob.com) for detailed statistics
- [Apache Maven](https://maven.apache.org) for build automation
- [SQLite](https://www.sqlite.org) for embedded database

---

## ⚠️ Disclaimer

This application is for **educational and personal use only**. Predictions are generated by statistical models and are not guaranteed to be accurate. Always gamble responsibly.



