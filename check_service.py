import os
import urllib.request
import json

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# Try querying auth.users with the key
req = urllib.request.Request(f"{url}/auth/v1/admin/users", headers={
    "apikey": key,
    "Authorization": f"Bearer {key}",
    "Content-Type": "application/json"
})
try:
    with urllib.request.urlopen(req) as response:
        print("Success, can access auth.users!")
        data = json.loads(response.read().decode())
        print(len(data), "users found")
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
