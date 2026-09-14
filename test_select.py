import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

req = urllib.request.Request(f"{url}/rest/v1/profiles", headers={
    "apikey": key,
    "Authorization": f"Bearer {key}",
    "Content-Type": "application/json"
})
try:
    with urllib.request.urlopen(req) as response:
        print("Success:", len(json.loads(response.read().decode())))
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
