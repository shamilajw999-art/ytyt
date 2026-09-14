import urllib.request
import json
import subprocess
import sys

try:
    # 1. Get server
    req = urllib.request.Request("https://api.gofile.io/servers")
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        server = data['data']['servers'][0]['name']
        print(f"Got server: {server}")

    # 2. Upload
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    upload_url = f"https://{server}.gofile.io/contents/uploadfile"
    
    print("Uploading to Gofile...")
    result = subprocess.run(["curl", "-s", "-F", f"file=@{apk_path}", upload_url], capture_output=True, text=True)
    
    upload_data = json.loads(result.stdout)
    if upload_data.get('status') == 'ok':
        print("\nSUCCESS!")
        print("Download Link:", upload_data['data']['downloadPage'])
    else:
        print("Upload failed:", result.stdout)

except Exception as e:
    print("Error:", e)
