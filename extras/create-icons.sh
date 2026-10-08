#!/bin/bash
################################################################
# Builds the installer icons from the 1024x1024 master image:
#   IndianGold.png  (Linux, 512x512)
#   IndianGold.ico  (Windows, 16-256 px)
#   IndianGold.icns (macOS, 16-1024 px as PNG entries)
#   ../src/main/resources/icons/indiangold-*.png (window and dock icons, 16-256 px)
# Needs Java 21 (to redraw the master), ImageMagick and python3.
# Usage: extras/create-icons.sh [--redraw]
################################################################
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

MASTER=IndianGold-icon.png
if [[ "${1:-}" == "--redraw" || ! -f "$MASTER" ]]; then
    java DrawIcon.java "$MASTER"
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
for s in 16 24 32 48 64 128 256 512 1024; do
    convert "$MASTER" -filter Lanczos -resize "${s}x${s}" -strip "PNG32:$TMP/$s.png"
done

cp "$TMP/512.png" IndianGold.png
mkdir -p ../src/main/resources/icons
for s in 16 32 48 64 128 256; do cp "$TMP/$s.png" "../src/main/resources/icons/indiangold-$s.png"; done

convert "$TMP/16.png" "$TMP/24.png" "$TMP/32.png" "$TMP/48.png" "$TMP/64.png" "$TMP/128.png" "$TMP/256.png" IndianGold.ico

# ICNS: a big-endian header plus one entry per size; since macOS 10.7 the entries may hold plain PNG data.
python3 - "$TMP" IndianGold.icns <<'EOF'
import struct, sys
tmp, out = sys.argv[1], sys.argv[2]
types = [("icp4", 16), ("icp5", 32), ("ic11", 32), ("ic12", 64), ("ic07", 128),
         ("ic13", 256), ("ic08", 256), ("ic14", 512), ("ic09", 512), ("ic10", 1024)]
body = b""
for code, size in types:
    data = open(f"{tmp}/{size}.png", "rb").read()
    body += code.encode() + struct.pack(">I", len(data) + 8) + data
open(out, "wb").write(b"icns" + struct.pack(">I", len(body) + 8) + body)
EOF

file IndianGold.png IndianGold.ico IndianGold.icns
