import urllib.request
import json
import random

url = "https://arhfpupxjcttsoyugmuc.supabase.co"
key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFyaGZwdXB4amN0dHNveXVnbXVjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc5MDk1MjMsImV4cCI6MjEwMzQ4NTUyM30.rSX8MxzdOwFVHnaT17t1KWI_hMAdiBYtkKh-z2MW6SY"

email = f"testuser{random.randint(1000, 9999)}@gmail.com"
password = "password123"

req = urllib.request.Request(f"{url}/auth/v1/signup", 
    data=json.dumps({"email": email, "password": password, "data": {"full_name": "Test User"}}).encode('utf-8'),
    headers={
        "apikey": key,
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json"
    }
)
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        print("Signup response:", json.dumps(data, indent=2))
        
        user_id = data.get("user", {}).get("id")
        # Let's login to get token if not present
        if not data.get("access_token"):
            req_login = urllib.request.Request(f"{url}/auth/v1/token?grant_type=password", 
                data=json.dumps({"email": email, "password": password}).encode('utf-8'),
                headers={
                    "apikey": key,
                    "Authorization": f"Bearer {key}",
                    "Content-Type": "application/json"
                }
            )
            with urllib.request.urlopen(req_login) as resp_login:
                data_login = json.loads(resp_login.read().decode())
                access_token = data_login.get("access_token")
                print("Login successful, token retrieved.")
        else:
            access_token = data.get("access_token")
            
        # Check profile
        req2 = urllib.request.Request(f"{url}/rest/v1/profiles?id=eq.{user_id}", headers={
            "apikey": key,
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json"
        })
        with urllib.request.urlopen(req2) as resp2:
            profiles = json.loads(resp2.read().decode())
            print("Profiles found for this user:", len(profiles))
            if profiles:
                print(profiles[0])
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
