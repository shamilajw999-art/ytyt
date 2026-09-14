import urllib.request
import json

# Can we use psql via some environment variable?
# Let's check for SUPABASE_DB_URL or something.
import os
for k, v in os.environ.items():
    if "SUPABASE" in k:
        print(k)
