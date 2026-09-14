import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# GET /rest/v1/
req = urllib.request.Request(f"{url}/rest/v1/?apikey={key}")
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode())
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
