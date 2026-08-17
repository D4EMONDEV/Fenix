"""Writes the .nbt the mod's game tests are placed in.

Nothing generates a structure template from Java -- one is normally made in game
with a structure block and saved. This one is small and regular enough to write
by hand, which keeps the tests self-contained.

The shape is copied from a template the game already accepts rather than from
memory: root compound, DataVersion, size as a list of three ints, a palette of
block states, and blocks as {pos: [x,y,z], state: index}.

    python tools/make-test-structure.py
"""
import gzip
import pathlib
import struct

# The version the game writes for 26.2. A template with an older one is run
# through the data fixers, which is slower and silently rewrites what it reads.
DATA_VERSION = 4903

OUT = (pathlib.Path(__file__).resolve().parent.parent
       / "src/main/resources/data/example-mod/structure")

# Five wide so a test has room around whatever it places, five tall so nothing
# hits the ceiling, with two floor layers: the lower one is what the tests stand
# on, the upper one is what a block placed at y=2 rests against.
SIZE = (5, 5, 5)
FLOOR_Y = 1


def tag_string(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def named(tag_id, name, payload):
    return struct.pack(">b", tag_id) + tag_string(name) + payload


def list_of(element_id, payloads):
    return struct.pack(">bi", element_id, len(payloads)) + b"".join(payloads)


def int_list(values):
    return list_of(3, [struct.pack(">i", v) for v in values])


def compound(*fields):
    return b"".join(fields) + b"\x00"


palette = ["minecraft:polished_andesite", "minecraft:air"]

blocks = []
for x in range(SIZE[0]):
    for y in range(SIZE[1]):
        for z in range(SIZE[2]):
            state = 0 if y <= FLOOR_Y else 1
            blocks.append(compound(
                named(9, "pos", int_list([x, y, z])),
                named(3, "state", struct.pack(">i", state)),
            ))

root = compound(
    named(3, "DataVersion", struct.pack(">i", DATA_VERSION)),
    named(9, "size", int_list(list(SIZE))),
    named(9, "palette", list_of(10, [
        compound(named(8, "Name", tag_string(name))) for name in palette
    ])),
    named(9, "blocks", list_of(10, blocks)),
    named(9, "entities", list_of(10, [])),
)

document = named(10, "", root)

OUT.mkdir(parents=True, exist_ok=True)
path = OUT / "test_platform.nbt"
path.write_bytes(gzip.compress(document, 9))
print(f"{path.name}  {path.stat().st_size} bytes  "
      f"{SIZE[0]}x{SIZE[1]}x{SIZE[2]}, {len(blocks)} blocks")
