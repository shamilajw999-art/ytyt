import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# Login with demo account
req = urllib.request.Request(f"{url}/auth/v1/token?grant_type=password", 
    data=json.dumps({"email": "demo@mythic.app", "password": "mythicdemo123"}).encode('utf-8'),
    headers={
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
)
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        access_token = data.get("access_token")
        user_id = data.get("user", {}).get("id")
        
        # Test inserting profile
        req2 = urllib.request.Request(f"{url}/rest/v1/profiles", 
            data=json.dumps({"id": user_id, "username": "demo", "email": "demo@mythic.app"}).encode('utf-8'),
            headers={
                "apikey": key,
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json",
                "Prefer": "return=representation"
            }
        )
        try:
            with urllib.request.urlopen(req2) as resp2:
                print("Insert success:", resp2.read().decode())
        except urllib.error.HTTPError as e:
            print("Insert HTTP Error:", e.code, e.read().decode())
            
        # Test updating profile
        req3 = urllib.request.Request(f"{url}/rest/v1/profiles?id=eq.{user_id}", 
            data=json.dumps({"xp": 100}).encode('utf-8'),
            headers={
                "apikey": key,
                "Authorization": f"Bearer {access_token}",
                "Content-Type": "application/json",
                "Prefer": "return=representation"
            }, method="PATCH"
        )
        try:
            with urllib.request.urlopen(req3) as resp3:
                print("Update success:", resp3.read().decode())
        except urllib.error.HTTPError as e:
            print("Update HTTP Error:", e.code, e.read().decode())
            
except Exception as e:
    print("Login error:", e)
