"""Generate the grandMA2 fixture XML for the Theatrical Extra Lights Blinder 2x2 Warm.

- 4CH Pixel : one dimmer per lamp, sub-fixtures 1.1 to 1.4

Lamps run row by row from the top, left to right seen from the front, as in the mod's
"4-Channel Pixel Mode". The colour is fixed warm tungsten in the block, so there is no colour
channel. Structure and channel conventions come from gen_ma2_ledpanel.py in this folder.
"""
import sys

from gen_ma2_gobo import dimmer
from gen_ma2_rgbbar import fixture_type, module

BLINDER = dict(name="THEATRICAL BLINDER 2X2 WARM", short="TEL-Bl2x2W")
SIZE = (0.75, 0.6)
GRID = (2, 2)  # rows, columns
POSITION = {(0, 0): "top left", (0, 1): "top right", (1, 0): "bottom left", (1, 1): "bottom right"}


def pixel():
    rows, cols = GRID
    count = rows * cols
    lamp = (round(SIZE[0] / cols, 3), round(SIZE[1] / rows, 3))
    modules = [module(0, "MAIN", SIZE, []), module(1, "LAMP", lamp, [dimmer(0, 1)])]
    instances = ['      <Instance index="0" patch="1" module_index="0" name="Main"/>']
    for p in range(count):
        row, col = divmod(p, cols)
        instances.append(f'      <Instance index="{p + 1}" patch="{1 + p}" module_index="1" '
                         f'name="Lamp {p + 1}"/>')
    order = ", ".join(f"{p + 1} {POSITION[divmod(p, cols)]}" for p in range(count))
    info = (f"Theatrical Extra Lights Blinder 2x2 Warm in the 4-channel pixel personality: one dimmer per lamp, "
            f"seen from the front ({order}). The lamps are sub-fixtures 1.1 to 1.{count}; the main fixture has no "
            f"channels and selecting it addresses every lamp. Colour is fixed warm tungsten. "
            f"Requires mod personality \"4-Channel Pixel Mode\".")
    return fixture_type(BLINDER, "4CH Pixel", "Px", info, modules, instances)


FILES = {
    "nailec@theatrical_blinder_2x2_warm@4ch_pixel.xml": pixel,
}

if __name__ == "__main__":
    out_dir = sys.argv[1]
    for name, build in FILES.items():
        with open(f"{out_dir}/{name}", "w", encoding="utf-8") as f:
            f.write(build())
    print("ok", len(FILES), "files")
