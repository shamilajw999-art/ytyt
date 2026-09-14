import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_ANON_KEY", "")

req = urllib.request.Request(f"{url}/rest/v1/profiles?select=*", headers={
    "apikey": key,
    "Authorization": f"Bearer {key}",
    "Content-Type": "application/json"
})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        print("Profiles:", len(data))
        for p in data:
            print(p.get("id"), p.get("email"), p.get("role"), p.get("xp"))
except Exception as e:
    print(e)
