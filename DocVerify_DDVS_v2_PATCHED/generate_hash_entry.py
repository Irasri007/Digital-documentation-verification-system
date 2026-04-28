#!/usr/bin/env python3
"""
DocVerify — Manual Hash Entry Generator
========================================
Run this script on any document file to get:
  1. Its SHA-256 hash
  2. A ready-to-paste JSON entry for ddvs-data.json

Usage:
    python generate_hash_entry.py path/to/your/document.pdf

Then copy the printed JSON block into the "documents" array in ddvs-data.json.
When someone uploads that same document, it will show as VERIFIED.
"""

import hashlib
import hmac
import json
import sys
import os
from datetime import datetime, timezone

# ── MUST match hmac.key in ddvs.properties ───────────────────────────────────
HMAC_KEY = "docverify-geu-2024-change-this-secret-key-before-use"

def sha256_hex(filepath):
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()

def hmac_sign(hash_hex):
    return hmac.new(
        HMAC_KEY.encode("utf-8"),
        hash_hex.encode("utf-8"),
        hashlib.sha256
    ).hexdigest()

def generate_cred_id():
    import random, string
    year = datetime.now().year
    chars = string.ascii_uppercase + string.digits
    suffix = "".join(random.choices(chars, k=8))
    return f"CRED-GEU-{year}-{suffix}"

def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_hash_entry.py <file_path> [issuer_name] [credential_id]")
        sys.exit(1)

    filepath  = sys.argv[1]
    issuer    = sys.argv[2] if len(sys.argv) > 2 else "Graphic Era University"
    cred_id   = sys.argv[3] if len(sys.argv) > 3 else generate_cred_id()

    if not os.path.exists(filepath):
        print(f"Error: File not found: {filepath}")
        sys.exit(1)

    filename  = os.path.basename(filepath)
    filesize  = os.path.getsize(filepath)
    hash_hex  = sha256_hex(filepath)
    signature = hmac_sign(hash_hex)
    issued_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z")

    entry = {
        "credentialId": cred_id,
        "hash":         hash_hex,
        "signature":    signature,
        "fileName":     filename,
        "fileSize":     filesize,
        "fileType":     "application/pdf",
        "issuedAt":     issued_at,
        "issuer":       issuer
    }

    print("\n" + "="*60)
    print("  SHA-256 Hash:", hash_hex)
    print("  Credential ID:", cred_id)
    print("="*60)
    print("\nPaste this into the \"documents\" array in ddvs-data.json:\n")
    print(json.dumps(entry, indent=4))
    print("\nAlso add this to the \"auditLog\" array:\n")
    audit = {
        "action":       "ISSUE",
        "credentialId": cred_id,
        "fileName":     filename,
        "result":       "SUCCESS",
        "at":           issued_at
    }
    print(json.dumps(audit, indent=4))
    print()

if __name__ == "__main__":
    main()
