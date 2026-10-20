"""Generate grandMA2 fixture XMLs for the Theatrical Extra Lights LED bars.

Three bars, each with a classic mode and a pixel mode matching the block's DMX personalities:

- RGB Bar:          4CH (Dim, R, G, B)                    / 36CH Pixel (9 x Dim/R/G/B)
- Vertical RGB Bar: 4CH (Dim, R, G, B)                    / 36CH Pixel (9 x Dim/R/G/B, pixel 1 at the bottom)
- Moving Bar:       7CH (Dim, R, G, B, Focus, Pan, Tilt)  / 34CH Pixel (Pan, Tilt, then 8 x Dim/R/G/B)

The pixel modes are multi-instance types: a MAIN module (channel-less, or Pan/Tilt on the moving
bar) plus one PIXEL module patched once per pixel, so the desk shows the pixels as sub-fixtures
(1.1 .. 1.n) and the effect engine, the colour picker and pixel layouts work per pixel, dimmer
chases included. There is no master dimmer: selecting the main fixture addresses every pixel, so
the fixture dimmer still fades the whole bar.

Channel conventions come from gen_ma2_gobo.py in this folder.
"""
import sys
from xml.sax.saxutils import escape

from gen_ma2_gobo import dimmer, focus, pan, rgb, tilt

HEADER = '''<?xml version="1.0" encoding="UTF-8"?>
<MA xmlns:xml="http://www.w3.org/XML/1998/namespace" major_vers="3" minor_vers="2" stream_vers="2">
  <Info datetime="2026-9-26T22:00:00" showfile="ma fixture builder"/>'''

FOOTER = '''    <Wheels index="2"/>
    <VirtualFunctionBlocks index="3"/>
    <FixtureMacroCollect index="5"/>
  </FixtureType>
</MA>
'''

# slug (file name), fixture type name, display label, short name, pixel count, body size (x, y), vertical, moving
BARS = [
    dict(slug="rgb_bar", name="THEATRICAL RGB BAR", label="RGB Bar", short="TEL-Bar", pixels=9,
         size=(2.9, 0.25), vertical=False, moving=False, order="left to right"),
    dict(slug="vertical_rgb_bar", name="THEATRICAL VERTICAL RGB BAR", label="Vertical RGB Bar", short="TEL-VBar", pixels=9,
         size=(0.25, 2.9), vertical=True, moving=False, order="bottom to top"),
    dict(slug="moving_bar", name="THEATRICAL MOVING BAR", label="Moving Bar", short="TEL-MBar", pixels=8,
         size=(1.5, 0.19), vertical=False, moving=True, order="left to right"),
    dict(slug="dense_rgb_bar", name="THEATRICAL DENSE RGB BAR", label="Dense RGB Bar", short="TEL-DBar", pixels=46,
         size=(2.9, 0.25), vertical=False, moving=False, order="left to right"),
    dict(slug="dense_vertical_rgb_bar", name="THEATRICAL DENSE VERTICAL RGB BAR", label="Dense Vertical RGB Bar", short="TEL-DVBar", pixels=46,
         size=(0.25, 2.9), vertical=True, moving=False, order="bottom to top"),
    dict(slug="dense_moving_bar", name="THEATRICAL DENSE MOVING BAR", label="Dense Moving RGB Bar", short="TEL-DMBar", pixels=24,
         size=(1.5, 0.19), vertical=False, moving=True, order="left to right"),
]


def rgb_channels(idx0, ch0):
    return [
        rgb(idx0 + 0, ch0 + 0, 1, "Red", "ff0000"),
        rgb(idx0 + 1, ch0 + 1, 2, "Green", "00ff00"),
        rgb(idx0 + 2, ch0 + 2, 3, "Blue", "0000ff"),
    ]


def module(index, name, size, channels):
    body = ("\n" + "\n".join(channels)) if channels else ""
    return f'''      <Module index="{index}" name="{name}" class="LED" beamtype="Wash" beam_angle="28" beam_intensity="3000">
        <Body>
          <Size x="{size[0]}" y="{size[1]}" z="0.08"/>
        </Body>{body}
      </Module>'''


def fixture_type(bar, mode, short_suffix, info, modules, instances):
    return f'''{HEADER}
  <FixtureType index="0" name="{bar["name"]}" mode="{mode}">
    <InfoItems>
      <Info>{escape(info)}</Info>
    </InfoItems>
    <short_name>{bar["short"]}{short_suffix}</short_name>
    <manufacturer>Nailec</manufacturer>
    <short_manufacturer>Nailec</short_manufacturer>
    <Modules index="0">
{chr(10).join(modules)}
    </Modules>
    <Instances index="1">
{chr(10).join(instances)}
    </Instances>
{FOOTER}'''


def classic_channels(bar):
    channels = [dimmer(0, 1)] + rgb_channels(1, 2)
    if bar["moving"]:
        channels += [focus(4, 5), pan(5, 6), tilt(6, 7)]
    return channels


def fixture_classic(bar):
    channels = classic_channels(bar)
    total = len(channels)
    layout = "Dim, Red, Green, Blue" + (", Focus, Pan, Tilt" if bar["moving"] else "")
    info = (f"Theatrical Extra Lights {bar['label']} in the {total}-channel personality: {layout} for the "
            f"whole bar. Requires mod personality \"{total}-Channel Mode\".")
    modules = [module(0, "MAIN", bar["size"], channels)]
    instances = ['      <Instance index="0" patch="1" module_index="0" name="Main"/>']
    return fixture_type(bar, f"{total}CH", "", info, modules, instances)


def fixture_pixel(bar):
    pixels = bar["pixels"]
    stride = 4
    header = [pan(0, 1), tilt(1, 2)] if bar["moving"] else []
    total = len(header) + pixels * stride
    if bar["vertical"]:
        pixel_size = (bar["size"][0], round(bar["size"][1] / pixels, 3))
    else:
        pixel_size = (round(bar["size"][0] / pixels, 3), bar["size"][1])
    modules = [
        module(0, "MAIN", bar["size"], header),
        module(1, "PIXEL", pixel_size, [dimmer(0, 1)] + rgb_channels(1, 2)),
    ]
    instances = ['      <Instance index="0" patch="1" module_index="0" name="Main"/>']
    for p in range(pixels):
        instances.append(f'      <Instance index="{p + 1}" patch="{len(header) + 1 + p * stride}" '
                         f'module_index="1" name="Pixel {p + 1}"/>')
    header_txt = ("Pan and Tilt on the main fixture (channels 1 and 2), then " if bar["moving"] else "")
    main_txt = ("the main fixture carries pan and tilt" if bar["moving"] else "the main fixture has no channels")
    mode_name = (f"{total}-Channel Pixel Mode (" + ("Pan, Tilt + " if bar["moving"] else "")
                 + f"{pixels}x Dim/RGB)")
    info = (f"Theatrical Extra Lights {bar['label']} in the {total}-channel pixel personality: {header_txt}"
            f"Dimmer/Red/Green/Blue for each of the {pixels} pixels, {bar['order']}, no master dimmer. The pixels "
            f"are sub-fixtures 1.1 to 1.{pixels}, each with its own dimmer and colour; {main_txt}, and selecting "
            f"it addresses all pixels. Requires mod personality \"{mode_name}\".")
    return fixture_type(bar, f"{total}CH Pixel", "Px", info, modules, instances)


def write(out_dir, bar):
    classic = fixture_classic(bar)
    classic_total = len(classic_channels(bar))
    pixel_total = (2 if bar["moving"] else 0) + bar["pixels"] * 4
    with open(f"{out_dir}/nailec@theatrical_{bar['slug']}@{classic_total}ch.xml", "w", encoding="utf-8") as f:
        f.write(classic)
    with open(f"{out_dir}/nailec@theatrical_{bar['slug']}@{pixel_total}ch_pixel.xml", "w", encoding="utf-8") as f:
        f.write(fixture_pixel(bar))


if __name__ == "__main__":
    out_dir = sys.argv[1]
    for bar in BARS:
        write(out_dir, bar)
    print("ok")
