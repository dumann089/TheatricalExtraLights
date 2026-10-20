"""Generate grandMA2 fixture XMLs for the Theatrical Extra Lights LED Panel 2.

- 4CH       : Dim, R, G, B for the whole panel
- 64CH Pixel : 4 x 4 grid, Dim/R/G/B per pixel, sub-fixtures 1.1 to 1.16
- 256CH Pixel: 8 x 8 grid, Dim/R/G/B per pixel, sub-fixtures 1.1 to 1.64

Pixels run row by row from the top, left to right seen from the front. Structure and channel
conventions come from gen_ma2_rgbbar.py and gen_ma2_gobo.py in this folder.
"""
import sys

from gen_ma2_gobo import dimmer
from gen_ma2_rgbbar import fixture_type, module, rgb_channels

PANEL = dict(name="THEATRICAL LED PANEL 2", short="TEL-Panel")
SIZE = (1.0, 1.0)


def classic():
    info = ("Theatrical Extra Lights LED Panel 2 in the 4-channel personality: Dim, Red, Green, Blue for the "
            "whole panel. Requires mod personality \"4-Channel Mode\".")
    return fixture_type(PANEL, "4CH", "", info,
                        [module(0, "MAIN", SIZE, [dimmer(0, 1)] + rgb_channels(1, 2))],
                        ['      <Instance index="0" patch="1" module_index="0" name="Main"/>'])


def pixel(grid):
    count = grid * grid
    total = count * 4
    cell = round(SIZE[0] / grid, 3)
    modules = [module(0, "MAIN", SIZE, []), module(1, "PIXEL", (cell, cell), [dimmer(0, 1)] + rgb_channels(1, 2))]
    instances = ['      <Instance index="0" patch="1" module_index="0" name="Main"/>']
    for p in range(count):
        row, col = divmod(p, grid)
        instances.append(f'      <Instance index="{p + 1}" patch="{1 + p * 4}" module_index="1" '
                         f'name="Px R{row + 1} C{col + 1}"/>')
    mode = f"{total}-Channel Pixel Mode ({grid}x{grid} Dim/RGB)"
    info = (f"Theatrical Extra Lights LED Panel 2 in the {total}-channel pixel personality: a {grid} x {grid} grid, "
            f"Dimmer/Red/Green/Blue per pixel, row by row from the top, left to right seen from the front. The pixels "
            f"are sub-fixtures 1.1 to 1.{count}; the main fixture has no channels and selecting it addresses every "
            f"pixel. Requires mod personality \"{mode}\".")
    return fixture_type(PANEL, f"{total}CH Pixel", "Px", info, modules, instances)


FILES = {
    "nailec@theatrical_led_panel_2@4ch.xml": classic,
    "nailec@theatrical_led_panel_2@64ch_pixel.xml": lambda: pixel(4),
    "nailec@theatrical_led_panel_2@256ch_pixel.xml": lambda: pixel(8),
}

if __name__ == "__main__":
    out_dir = sys.argv[1]
    for name, build in FILES.items():
        with open(f"{out_dir}/{name}", "w", encoding="utf-8") as f:
            f.write(build())
    print("ok", len(FILES), "files")
