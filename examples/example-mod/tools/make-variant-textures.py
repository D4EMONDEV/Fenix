"""Draws the textures the mod's cow variant and armour trim need.

Two things the game loads by path and never complains about: an animal variant
whose texture is missing renders as the missing checker on a cow, and a trim
pattern whose texture is missing simply is not drawn -- the armour looks
untrimmed rather than broken.

    python tools/make-variant-textures.py
"""
import pathlib
import struct
import zlib

ROOT = (pathlib.Path(__file__).resolve().parent.parent
        / "src/main/resources/assets/example-mod/textures")

DARK = (96, 20, 40, 255)
MID = (150, 30, 60, 255)
LIT = (198, 54, 88, 255)
NONE = (0, 0, 0, 0)


def png(path, pixels, w, h):
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *pixels[y * w + x]) for x in range(w))
                   for y in range(h))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9))
                     + chunk(b"IEND", b""))
    return path


def grain(x, y):
    n = (x * 7 + y * 13 + (x * y) % 5) % 11
    return DARK if n < 2 else (LIT if n < 4 else MID)


# ------------------------------------------------------------------- the cow
# 64x64, which is the size the cow model unwraps against. A texture of another
# size is not rejected: it is sampled, and the cow comes out wearing slices of
# the wrong part of the file.
cow = [grain(x, y) for y in range(64) for x in range(64)]
p = png(ROOT / "entity/cow/ruby.png", cow, 64, 64)
print(f"{p.relative_to(ROOT)}  {p.stat().st_size} bytes  64x64")


# ------------------------------------------------------------- the trim
# Greyscale on purpose. A trim texture is recoloured by whichever material is
# applied, so one drawn in colour comes out right in exactly one material and
# wrong in every other -- and nothing says so.
def facet(w, h, filled):
    pixels = []
    for y in range(h):
        for x in range(w):
            if not filled(x, y):
                pixels.append(NONE)
                continue
            level = 150 + ((x * 5 + y * 3) % 4) * 20
            pixels.append((level, level, level, 255))
    return pixels


# A band across the chest and a stripe down each arm, in the regions the
# humanoid layer samples.
body = facet(64, 32, lambda x, y: 20 <= y <= 23 or (40 <= x < 56 and 20 <= y <= 26))
legs = facet(64, 32, lambda x, y: 20 <= y <= 22)

for name, pixels in (("humanoid", body), ("humanoid_leggings", legs)):
    p = png(ROOT / "trims/entity" / name / "facet.png", pixels, 64, 32)
    print(f"{p.relative_to(ROOT)}  {p.stat().st_size} bytes  64x32")
