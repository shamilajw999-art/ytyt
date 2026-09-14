import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

req = urllib.request.Request(f"{url}/rest/v1/profiles?limit=1", headers={
    "apikey": key,
    "Authorization": f"Bearer {key}",
    "Content-Type": "application/json"
})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        if data:
            print("Columns in profile:", list(data[0].keys()))
        else:
            print("No profiles returned")
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
