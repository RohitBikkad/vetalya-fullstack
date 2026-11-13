#!/usr/bin/env python3
"""Non-destructive HTTPS smoke checks. Does not use account or database credentials."""
import concurrent.futures
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "https://vetalya-staging-z0u9.onrender.com").rstrip("/")
assert urllib.parse.urlsplit(BASE).scheme == "https", "Use the public HTTPS staging origin"

def request(path, expected=200, body=None):
    req = urllib.request.Request(BASE + path, data=body, headers={"Content-Type": "application/json"} if body else {})
    try:
        with urllib.request.urlopen(req, timeout=45) as response:
            data, status, content_type = response.read(), response.status, response.headers.get("Content-Type", "")
    except urllib.error.HTTPError as error:
        data, status, content_type = error.read(), error.code, error.headers.get("Content-Type", "")
    assert status == expected, f"{path}: expected {expected}, received {status}"
    return data, status, content_type

def check(path):
    data, status, content_type = request(path)
    if path in ("/", "/owner/login", "/owner/register", "/vet/login"):
        assert b'id="root"' in data, f"{path}: missing React document (empty responses must fail)"
    if path == "/actuator/health/readiness":
        assert json.loads(data)["status"] == "UP"
    assert data, f"{path}: empty body"
    print(f"PASS {status} {path}")
    return data

paths = ["/", "/owner/login", "/owner/register", "/vet/login", "/vetalya-wordmark.png", "/figma-owner/Manrope-Bold.ttf", "/api/auth/health", "/actuator/health/readiness"]
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    documents = list(pool.map(check, paths))
for asset in re.findall(rb'(?:src|href)="(/assets/[^\"]+)"', documents[0]):
    check(asset.decode())
for path, status, body in [("/api/owner/pets", 401, None), ("/api/public/db-inspect", 404, None), ("/api/auth/demo-login", 404, b'{"role":"PLATFORM_ADMIN"}')]:
    request(path, status, body)
    print(f"PASS {status} {path}")
print("All staging smoke checks passed.")
