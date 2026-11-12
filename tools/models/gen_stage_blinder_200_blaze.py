"""Build the faceted round Stage Blinder 200 Blaze body and its painted lens."""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "common/src/main/resources/assets/theatricalextralights"
OUT = ROOT / "models/block/stage_blinder_200_blaze"
TEX = ROOT / "textures/block/stage_blinder_200_blaze"
OUT.mkdir(parents=True, exist_ok=True)
TEX.mkdir(parents=True, exist_ok=True)


def box(a, b, texture="#body", uv=None, only=None):
    x, y, z = a
    X, Y, Z = b
    spans = {"north": (X-x, Y-y), "south": (X-x, Y-y),
             "east": (Z-z, Y-y), "west": (Z-z, Y-y),
             "up": (X-x, Z-z), "down": (X-x, Z-z)}
    faces = {side: {"uv": uv or [0, 0, min(16, w), min(16, h)], "texture": texture}
             for side, (w, h) in spans.items() if only is None or side in only}
    return {"from": list(a), "to": list(b), "faces": faces}


# The broad inverted U sits above the lamp, as on the reference unit.
pan = [
    box((1.3, 14.9, 7.2), (14.7, 15.65, 12.4), "#metal"),
    box((1.3, 6.3, 7.2), (2.1, 15.4, 12.4), "#metal"),
    box((13.9, 6.3, 7.2), (14.7, 15.4, 12.4), "#metal"),
    box((1.1, 5.9, 9), (2.4, 7.3, 10.8), "#metal"),
    box((13.6, 5.9, 9), (14.9, 7.3, 10.8), "#metal"),
    box((.55, 6.1, 9.25), (1.1, 7.1, 10.55), "#silver"),
    box((14.9, 6.1, 9.25), (15.45, 7.1, 10.55), "#silver"),
    box((6.2, 15.65, 9), (9.8, 15.9, 10.6), "#silver"),
]

# Against a wall or a vertical truss the yoke reaches back to the support.
# The body keeps its forward horizontal axis; only the bracket changes shape.
wall_pan = [
    box((1.3, 5.9, 9.4), (2.1, 7.2, 15.4), "#metal"),
    box((13.9, 5.9, 9.4), (14.7, 7.2, 15.4), "#metal"),
    box((1.3, 5.4, 15.0), (14.7, 7.7, 15.8), "#metal"),
    box((5.5, 5.5, 15.8), (10.5, 7.6, 16.0), "#silver"),
    box((.7, 5.8, 9.0), (2.4, 7.3, 10.9), "#metal"),
    box((13.6, 5.8, 9.0), (15.3, 7.3, 10.9), "#metal"),
    box((.4, 6.1, 9.3), (.7, 7.0, 10.6), "#silver"),
    box((15.3, 6.1, 9.3), (15.6, 7.0, 10.6), "#silver"),
]

# Layered polygonal shell; six bands read as a cylinder at Minecraft scale.
tilt = []
for y0, y1, x0, x1 in [(1, 1.75, 6.2, 9.8), (1.75, 2.75, 4.5, 11.5),
                        (2.75, 3.95, 3.6, 12.4), (3.95, 9.05, 3.15, 12.85),
                        (9.05, 10.25, 3.6, 12.4), (10.25, 11.25, 4.5, 11.5),
                        (11.25, 12, 6.2, 9.8)]:
    tilt.append(box((x0, y0, 6.3), (x1, y1, 13.8), "#vents"))

# Recess, retaining bezel and the full circular diffuser share the same front plane.
tilt += [box((2, .5, 5.55), (14, 12.5, 5.68), "#face", [0, 0, 16, 16], ["north"]),
         box((4.4, 2.6, 13.8), (11.6, 10.4, 14.05), "#metal"),
         box((2.8, 5.9, 9.2), (3.2, 7.1, 10.6), "#silver"),
         box((12.8, 5.9, 9.2), (13.2, 7.1, 10.6), "#silver")]

textures = {"body": "theatricalextralights:block/stage_blinder_200_blaze/body",
            "metal": "theatricalextralights:block/stage_blinder_200_blaze/metal",
            "vents": "theatricalextralights:block/stage_blinder_200_blaze/vents",
            "silver": "theatricalextralights:block/stage_blinder_200_blaze/silver",
            "face": "theatricalextralights:block/stage_blinder_200_blaze/face",
            "particle": "theatricalextralights:block/stage_blinder_200_blaze/body"}
for name, elements in {"static": [], "pan": pan, "wall_pan": wall_pan,
                       "tilt": tilt, "whole": pan + tilt, "wall_whole": wall_pan + tilt}.items():
    (OUT / f"stage_blinder_200_blaze_{name}.json").write_text(
        json.dumps({"credit": "Theatrical Extra Lights", "textures": textures,
                    "elements": elements}, indent=2) + "\n", encoding="utf-8")

for name, color in {"body": (19, 21, 24), "metal": (31, 33, 37), "silver": (76, 79, 82)}.items():
    Image.new("RGBA", (16, 16), (*color, 255)).save(TEX / f"{name}.png")
vent = Image.new("RGBA", (16, 16), (24, 26, 28, 255))
d = ImageDraw.Draw(vent)
for y in (3, 6, 9, 12):
    d.rectangle((2, y, 13, y + 1), fill=(5, 7, 9, 255))
    d.line((3, y + 2, 12, y + 2), fill=(50, 52, 54, 255))
vent.save(TEX / "vents.png")

face = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
p = face.load()
for y in range(128):
    for x in range(128):
        dx, dy = x - 63.5, y - 63.5
        r = math.hypot(dx, dy)
        if r > 62:
            continue
        if r > 59.5:
            c = (64, 67, 70)
        elif r > 57:
            c = (8, 9, 11)
        elif r > 52.5:
            c = (42, 44, 47)
        elif r > 50.5:
            c = (144, 128, 103)
        else:
            grain = ((x * 37 + y * 19) % 9) - 4
            radial = int(14 * (1 - r / 51))
            c = (218 + grain + radial, 207 + grain + radial, 186 + grain + radial)
        p[x, y] = (*c, 255)
d = ImageDraw.Draw(face)
for a in range(0, 360, 45):
    rad = math.radians(a + 22.5)
    x, y = 63.5 + 55 * math.cos(rad), 63.5 + 55 * math.sin(rad)
    d.ellipse((x-2, y-2, x+2, y+2), fill=(93, 95, 97, 255))
    d.line((x-1, y+1, x+1, y-1), fill=(12, 13, 15, 255), width=1)
face.save(TEX / "face.png")
