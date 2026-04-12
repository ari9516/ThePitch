#!/usr/bin/env python3
"""
Correct SofaScore Wrapper Usage
Uses chromium-based requests to avoid 403 errors
"""

import asyncio
import json
import sys
from sofascore_wrapper.api import SofascoreAPI
from sofascore_wrapper.search import Search

async def search_team(team_name):
    """Search for a team using the correct wrapper method"""
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
                    if sport.get('id') == 1:  # Football
                        teams.append({
                            'id': entity.get('id'),
                            'name': entity.get('name'),
                            'slug': entity.get('slug'),
                            'country': entity.get('country', {}).get('name')
                        })
        return teams
    finally:
        await api.close()

async def search_player(player_name):
    """Search for a player (example - Bukayo Saka)"""
    api = SofascoreAPI()
    try:
        search = Search(api, search_string=player_name)
        results = await search.search_all()
        
        players = []
        if results and 'results' in results:
            for item in results['results']:
                if item.get('type') == 'player':
                    entity = item.get('entity', {})
                    players.append({
                        'id': entity.get('id'),
                        'name': entity.get('name'),
                        'team': entity.get('team', {}).get('name'),
                        'position': entity.get('position'),
                        'jersey_number': entity.get('jerseyNumber')
                    })
        return players
    finally:
        await api.close()

async def get_team_info(team_id):
    """Get detailed team information by searching for the team ID"""
    api = SofascoreAPI()
    try:
        search = Search(api, search_string=str(team_id))
        results = await search.search_all()
        
        if results and 'results' in results:
            for item in results['results']:
                if item.get('type') == 'team':
                    return item.get('entity', {})
        return None
    finally:
        await api.close()

def main():
    if len(sys.argv) < 2:
        print("=" * 60)
        print("SofaScore Wrapper - Correct Usage")
        print("=" * 60)
        print("\nCommands:")
        print("  search-team <name>     - Find team by name")
        print("  search-player <name>   - Find player by name")
        print("  team-info <id>         - Get team details by ID")
        print("\nExamples:")
        print("  python sofascore_correct.py search-team Arsenal")
        print("  python sofascore_correct.py search-player Saka")
        print("  python sofascore_correct.py team-info 42")
        print("=" * 60)
        return
    
    command = sys.argv[1]
    
    if command == "search-team" and len(sys.argv) >= 3:
        team_name = sys.argv[2]
        print(f"\n🔍 Searching for team '{team_name}'...")
        result = asyncio.run(search_team(team_name))
        print(json.dumps(result, indent=2))
    
    elif command == "search-player" and len(sys.argv) >= 3:
        player_name = sys.argv[2]
        print(f"\n⚽ Searching for player '{player_name}'...")
        result = asyncio.run(search_player(player_name))
        print(json.dumps(result, indent=2))
    
    elif command == "team-info" and len(sys.argv) >= 3:
        team_id = int(sys.argv[2])
        print(f"\n📊 Getting info for team ID {team_id}...")
        result = asyncio.run(get_team_info(team_id))
        print(json.dumps(result, indent=2))
    
    else:
        print("❌ Invalid command")

if __name__ == "__main__":
    main()
    