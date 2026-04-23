"""
ThePitch – Stats Fetcher (football-data.co.uk)
===============================================
Downloads Premier League 2024-25 stats directly from football-data.co.uk
This is a FREE, open CSV download — no scraping, no API key, no bot detection.

Stats included: shots, shots on target, corners, fouls, yellow/red cards, xG
Outputs: data/pl_match_stats.csv
"""

import requests
import pandas as pd
import os
import sys
from io import StringIO

# ── Helpers ───────────────────────────────────────────────────────────────────
def safe_float(val):
    try:
        return float(str(val).strip())
    except Exception:
        return 0.0

def safe_int(val):
    try:
        v = str(val).strip()
        if v == "" or v.lower() == "nan":
            return 0
        return int(float(v))
    except Exception:
        return 0

def get_col(df, *candidates):
    """Return the first candidate column name that exists in df, or None."""
    for c in candidates:
        if c in df.columns:
            return c
    return None

# ── Config ────────────────────────────────────────────────────────────────────
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data")
os.makedirs(OUT_DIR, exist_ok=True)

# football-data.co.uk direct CSV — public, free, no login needed
CSV_URL = "https://www.football-data.co.uk/mmz4281/2425/E0.csv"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0",
}

print("=" * 60)
print("  ThePitch - Stats Fetcher")
print("  Source: football-data.co.uk (free direct CSV)")
print("=" * 60)

# ── Step 1: Download CSV ──────────────────────────────────────────────────────
print(f"\n[1/3] Downloading PL 2024-25 stats CSV...")
print(f"      URL: {CSV_URL}")

try:
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    resp = requests.get(CSV_URL, headers=HEADERS, timeout=30, verify=False)
    resp.raise_for_status()
    print(f"      OK Downloaded ({len(resp.content) // 1024} KB)")
except requests.RequestException as e:
    print(f"      FAILED: {e}")
    sys.exit(1)

# ── Step 2: Parse ─────────────────────────────────────────────────────────────
print("\n[2/3] Parsing CSV...")

raw = pd.read_csv(StringIO(resp.text), on_bad_lines="skip")
raw = raw.dropna(subset=["HomeTeam", "AwayTeam"])

print(f"      Raw rows    : {len(raw)}")
print(f"      Raw columns : {list(raw.columns)}")

# Detect xG columns (varies by season/data provider)
xg_home_col = get_col(raw, "HxG", "HXGF", "xG_Home", "Home_xG", "B365CAHH")
xg_away_col = get_col(raw, "AxG", "AXGF", "xG_Away", "Away_xG", "B365CAHA")
print(f"      xG cols     : home={xg_home_col}, away={xg_away_col}")

# ── Step 3: Build per-team rows ───────────────────────────────────────────────
print("\n[3/3] Building per-team stats rows...")

all_stats = []

for _, row in raw.iterrows():
    date      = str(row.get("Date", "")).strip()
    home_team = str(row.get("HomeTeam", "")).strip()
    away_team = str(row.get("AwayTeam", "")).strip()

    if not home_team or not away_team or home_team == "nan":
        continue

    game_ref = f"{date}_{home_team}_{away_team}".replace(" ", "_").replace("/", "-")

    all_stats.append({
        "game_ref":       game_ref,
        "team_name":      home_team,
        "opponent":       away_team,
        "match_date":     date,
        "is_home":        1,
        "shots":          safe_int(row.get("HS",  0)),
        "shots_ot":       safe_int(row.get("HST", 0)),
        "corners":        safe_int(row.get("HC",  0)),
        "fouls":          safe_int(row.get("HF",  0)),
        "yellow_cards":   safe_int(row.get("HY",  0)),
        "red_cards":      safe_int(row.get("HR",  0)),
        "xg":             safe_float(row[xg_home_col]) if xg_home_col else 0.0,
        "goals":          safe_int(row.get("FTHG", 0)),
        "goals_conceded": safe_int(row.get("FTAG", 0)),
    })

    all_stats.append({
        "game_ref":       game_ref,
        "team_name":      away_team,
        "opponent":       home_team,
        "match_date":     date,
        "is_home":        0,
        "shots":          safe_int(row.get("AS",  0)),
        "shots_ot":       safe_int(row.get("AST", 0)),
        "corners":        safe_int(row.get("AC",  0)),
        "fouls":          safe_int(row.get("AF",  0)),
        "yellow_cards":   safe_int(row.get("AY",  0)),
        "red_cards":      safe_int(row.get("AR",  0)),
        "xg":             safe_float(row[xg_away_col]) if xg_away_col else 0.0,
        "goals":          safe_int(row.get("FTAG", 0)),
        "goals_conceded": safe_int(row.get("FTHG", 0)),
    })

# ── Save ──────────────────────────────────────────────────────────────────────
if not all_stats:
    print("FAILED: No stats built.")
    sys.exit(1)

stats_df = pd.DataFrame(all_stats)

# Drop rows where all stat columns are 0 (future/incomplete matches)
stat_cols = ["shots", "shots_ot", "corners", "fouls", "yellow_cards"]
stats_df = stats_df[stats_df[stat_cols].sum(axis=1) > 0]

out_path = os.path.join(OUT_DIR, "pl_match_stats.csv")
stats_df.to_csv(out_path, index=False)

print(f"\n{'='*60}")
print(f"SUCCESS!")
print(f"   Matches processed : {len(stats_df) // 2}")
print(f"   Total rows        : {len(stats_df)} (2 per match)")
print(f"   Columns           : {list(stats_df.columns)}")
print(f"   Saved to          : {os.path.abspath(out_path)}")
print(f"\n   Sample data:")
print(stats_df[["team_name","shots","shots_ot","corners","yellow_cards","xg"]].head(4).to_string())
print(f"\n{'='*60}")
print(f"Now run the Java app -> option 7 to import -> option 5 for real predictions!")
