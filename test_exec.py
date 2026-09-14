import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

req = urllib.request.Request(f"{url}/rest/v1/rpc/exec_sql", 
    data=json.dumps({"query": "SELECT 1"}).encode('utf-8'),
    headers={
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
)
try:
    with urllib.request.urlopen(req) as response:
        print("Success:", response.read().decode())
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
