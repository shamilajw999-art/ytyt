import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# Login with demo account to get jwt
req = urllib.request.Request(f"{url}/auth/v1/token?grant_type=password", 
    data=json.dumps({"email": "demo@mythic.app", "password": "mythicdemo123"}).encode('utf-8'),
    headers={"apikey": key, "Content-Type": "application/json"}
)
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        token = data['access_token']
        user_id = data['user']['id']
except Exception as e:
    # fallback to just testing with a script
    print("Could not login:", e)
    token = None

