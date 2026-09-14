import os
import urllib.request
import json
import uuid

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

fake_id = str(uuid.uuid4())
req = urllib.request.Request(f"{url}/rest/v1/profiles", 
    data=json.dumps({"id": fake_id, "email": "test@test.com", "role": "user"}).encode('utf-8'),
    headers={
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
        "Prefer": "return=representation"
    }
)
try:
    with urllib.request.urlopen(req) as response:
        print("Inserted:", response.read().decode())
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print("Error:", e.read().decode())
