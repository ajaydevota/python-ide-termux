import os
import struct
import zlib


def png(w, h, rows):
    raw = bytearray()
    for row in rows:
        raw.append(0)
        for px in row:
            raw += bytes(px)

    def chunk(t, d):
        return (struct.pack(">I", len(d)) + t + d +
                struct.pack(">I", zlib.crc32(t + d) & 0xffffffff))

    return (b"\x89PNG\r\n\x1a\n" +
            chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) +
            chunk(b"IDAT", zlib.compress(bytes(raw), 9)) +
            chunk(b"IEND", b""))


SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for dens, s in SIZES.items():
    rows = []
    for y in range(s):
        row = []
        for x in range(s):
            r, g, b, a = 46, 52, 64, 255
            if abs(x - y) < s * 0.09 and x < s * 0.55:
                r, g, b = 136, 192, 208
            if s * 0.68 < y < s * 0.78 and s * 0.50 < x < s * 0.85:
                r, g, b = 163, 190, 140
            row.append((r, g, b, a))
        rows.append(row)
    out = os.path.join("app", "src", "main", "res", "mipmap-" + dens)
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "ic_launcher.png"), "wb") as f:
        f.write(png(s, s, rows))

print("launcher icons generated")
