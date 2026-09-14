import os
import urllib.request
import json
import uuid

url = os.environ.get("SUPABASE_URL", "")
key = os.environ.get("SUPABASE_KEY", "")

# Call get_columns.py logic on payment_orders
req = urllib.request.Request(f"{url}/rest/v1/payment_orders?limit=1", headers={
    "apikey": key,
    "Authorization": f"Bearer {key}",
    "Content-Type": "application/json"
})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        print("Columns in payment_orders:", list(data[0].keys()) if data else "No rows, but success")
except Exception as e:
    print(e)
    if hasattr(e, 'read'):
        print(e.read().decode())
