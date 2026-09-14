import os
import urllib.request
import json
import uuid

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# 1. Sign up a new user to get a fresh token
test_email = f"test_{uuid.uuid4().hex[:6]}@mythic.app"
req_signup = urllib.request.Request(f"{url}/auth/v1/signup", 
    data=json.dumps({"email": test_email, "password": "password123"}).encode('utf-8'),
    headers={"apikey": key, "Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req_signup) as resp:
        data = json.loads(resp.read().decode())
        token = data.get('access_token')
        user_id = data.get('user', {}).get('id')
        print("Signed up user:", user_id)
except Exception as e:
    print("Signup failed:", e)
    if hasattr(e, 'read'):
        print(e.read().decode())
    token = None

if token:
    req_pay = urllib.request.Request(f"{url}/rest/v1/payment_orders", 
        data=json.dumps({
            "user_id": user_id,
            "plan": "pro",
            "amount": 1.99,
            "payment_reference": f"MYTHIC-{uuid.uuid4().hex[:8]}"
        }).encode('utf-8'),
        headers={
            "apikey": key,
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Prefer": "return=representation"
        }
    )
    try:
        with urllib.request.urlopen(req_pay) as resp_pay:
            print("Payment Order Success:", resp_pay.read().decode())
    except Exception as e:
        print("Payment Order Error:", e)
        if hasattr(e, 'read'):
            print(e.read().decode())
