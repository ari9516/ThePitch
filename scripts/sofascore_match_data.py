#!/usr/bin/env python3
"""
Fetch match data for predictions using SofaScore wrapper
"""

import asyncio
import json
import sys
from sofascore_wrapper.api import SofascoreAPI
from sofascore_wrapper.search import Search

async def search_team(team_name):
    """Search for team ID"""
    api = SofascoreAPI()
    try:
        search = Search(api, search_string=team_name)
        results = await search.search_all()
        
        teams = []
        if results and 'results' in results:
            for item in results['results']:
                if item.get('type') == 'team':
                    entity = item.get('entity', {})
                    sport = entity.get('sport', {})
                    if sport.get('id') == 1:
                        teams.append({
                            'id': entity.get('id'),
                            'name': entity.get('name'),
                            'slug': entity.get('slug')
                        })
        return teams
    finally:
        await api.close()

async def get_team_matches(team_id, limit=10):
    """Get recent matches for a team with scores"""
    api = SofascoreAPI()
    try:
        # Search for matches by team ID
        search = Search(api, search_string=str(team_id))
        results = await search.search_all()
        
        matches = []
        if results and 'results' in results:
            for item in results['results']:
                if item.get('type') == 'event':
                    entity = item.get('entity', {})
                    home_id = entity.get('homeTeam', {}).get('id')
                    away_id = entity.get('awayTeam', {}).get('id')
                    
                    if home_id == team_id or away_id == team_id:
                        status = entity.get('status', {})
                        if status.get('type') == 'finished':
                            matches.append({
                                'id': entity.get('id'),
                                'home_team': entity.get('homeTeam', {}).get('name'),
                                'away_team': entity.get('awayTeam', {}).get('name'),
                                'home_score': entity.get('homeScore', {}).get('display'),
                                'away_score': entity.get('awayScore', {}).get('display'),
                                'date': entity.get('startTimestamp'),
                                'is_home': home_id == team_id
                            })
                        if len(matches) >= limit:
                            break
        return matches
    finally:
        await api.close()

async def search_match(team1_name, team2_name):
    """Search for matches between two teams"""
    api = SofascoreAPI()
    try:
        # Search for both teams and find common matches
        search = Search(api, search_string=f"{team1_name} vs {team2_name}")
        results = await search.search_all()
        
        matches = []
        if results and 'results' in results:
            for item in results['results']:
                if item.get('type') == 'event':
                    entity = item.get('entity', {})
                    matches.append({
                        'id': entity.get('id'),
                        'home_team': entity.get('homeTeam', {}).get('name'),
                        'away_team': entity.get('awayTeam', {}).get('name'),
                        'home_score': entity.get('homeScore', {}).get('display'),
                        'away_score': entity.get('awayScore', {}).get('display'),
                        'status': entity.get('status', {}).get('type'),
                        'date': entity.get('startTimestamp')
                    })
        return matches
    finally:
        await api.close()

def main():
    if len(sys.argv) < 2:
        print("=" * 60)
        print("SofaScore Match Data Fetcher")
        print("=" * 60)
        print("\nCommands:")
        print("  team <name>              - Search team by name")
        print("  matches <team_id>        - Get recent matches for team")
        print("  match <team1> <team2>    - Search match between teams")
        print("\nExamples:")
        print("  python sofascore_match_data.py team Arsenal")
        print("  python sofascore_match_data.py matches 42")
        print("  python sofascore_match_data.py match Arsenal Liverpool")
        return
    
    command = sys.argv[1]
    
    if command == "team" and len(sys.argv) >= 3:
        team_name = sys.argv[2]
        print(f"\n🔍 Searching for '{team_name}'...")
        result = asyncio.run(search_team(team_name))
        print(json.dumps(result, indent=2))
    
    elif command == "matches" and len(sys.argv) >= 3:
        team_id = int(sys.argv[2])
        print(f"\n⚽ Getting recent matches for team ID {team_id}...")
        result = asyncio.run(get_team_matches(team_id))
        print(json.dumps(result, indent=2))
    
    elif command == "match" and len(sys.argv) >= 4:
        team1 = sys.argv[2]
        team2 = sys.argv[3]
        print(f"\n🏆 Searching for matches between {team1} and {team2}...")
        result = asyncio.run(search_match(team1, team2))
        print(json.dumps(result, indent=2))
    
    else:
        print("❌ Invalid command")

if __name__ == "__main__":
    main()
    