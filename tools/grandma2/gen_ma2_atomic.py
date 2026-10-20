"""Generate grandMA2 fixture XMLs for the Theatrical Extra Lights Atomic strobes.

Atomic Strobe (tube of nine white segments between two plates of four RGB zones):
- 1CH            : Strobe (0 off, 1-254 rate, 255 on)
- 4CH            : Dim, Flash Duration, Flash Rate, Effects            (Martin Atomic layout)
- 8CH Aura       : 4CH + Aura sub-fixture (Dim, R, G, B)                (Atomic 3000 LED layout)
- 10CH Compressed: 4CH + Plate sub-fixture (Dim, Duration, Rate, R, G, B)
- 40CH Pixel     : 4CH + 9 Segment sub-fixtures (Dim) + Plate (Dim, Duration, Rate) + 8 Zone sub-fixtures (R, G, B)
- 400CH PixelMap : 4CH + 9 Segment sub-fixtures (Dim) + Plate (Dim, Duration, Rate) + 96 Pixel sub-fixtures (Dim, R, G, B)

Atomic Tilt (strobe head on a tilting yoke):
- 6CH Atomic     : Dim, Flash Duration, Flash Rate, Effects, Focus, Tilt
- 9CH Atomic RGB : Dim, Flash Duration, Flash Rate, Effects, R, G, B, Focus, Tilt

Attribute choices follow martin@atomic_3000_led in the MA2 library: DIM, STROBEDURATION, SHUTTER
(strobe rate) and STROBEMODE (effects), with the aura / plate as separate instances so the desk shows
them as sub-fixtures. The effects ranges are the mod's: 0-9 strobe, 10-39 blinder, 40-69 ramp up,
70-99 ramp down, 100-129 ramp up/down, 130-159 random, 160-189 lightning, 190-219 spikes,
220-255 sparkle. Channel conventions come from gen_ma2_gobo.py in this folder.
"""
import sys
from xml.sax.saxutils import escape

from gen_ma2_gobo import channel, dimmer, focus, function, rgb, tilt

HEADER = '''<?xml version="1.0" encoding="UTF-8"?>
<MA xmlns:xml="http://www.w3.org/XML/1998/namespace" major_vers="3" minor_vers="2" stream_vers="2">
  <Info datetime="2026-9-27T12:00:00" showfile="ma fixture builder"/>'''

FOOTER = '''    <Wheels index="2"/>
    <VirtualFunctionBlocks index="3"/>
    <FixtureMacroCollect index="5"/>
  </FixtureType>
</MA>
'''

SH = ("SHUTTER", "Shutter", "BEAM", "Beam")   # feature, feature name, preset, preset name


def duration(idx, coarse, name="Duration"):
    """Flash length, 12 ms at 0 to 650 ms at 255."""
    return channel(idx, "STROBEDURATION", "SHUTTER", "BEAM", coarse, None, "40", "15", "", [
        function(0, 0, 255, "STROBEDURATION", name, "STROBEDURATION", name, *SH, "0.012", "0.65",
                 [("12 ms", 0, 0), ("100 ms", 100, 100), ("650 ms", 255, 255)])
    ])


def rate(idx, coarse, zero_name="Off", zero_phys="0"):
    """Flash rate: 0 stops the strobe (or leaves a plate on continuously), 1-255 runs 0.5 to 25 Hz."""
    return channel(idx, "SHUTTER", "SHUTTER", "BEAM", coarse, None, "0", "60", "", [
        function(0, 0, 0, "SHUTTER", "Shutter", "SHUTTER", "Shutter", *SH, zero_phys, zero_phys,
                 [(zero_name.lower(), 0, 0)], zero_name),
        function(1, 1, 255, "STROBE", "Rate", "SHUTTER", "Shutter", *SH, "0.5", "25",
                 [("0.5 Hz", 1, 1), ("5 Hz", 110, 110), ("12 Hz", 180, 180), ("25 Hz", 255, 255)], "Strobe"),
    ])


EFFECTS = [
    ("Strobe", 0, 9, "STROBEMODE"),
    ("Blinder", 10, 39, "STROBEMODE"),
    ("Ramp Up", 40, 69, "STROBEMODEPULSEOPEN"),
    ("Ramp Down", 70, 99, "STROBEMODEPULSECLOSE"),
    ("Ramp Up/Down", 100, 129, "STROBEMODEPULSE"),
    ("Random", 130, 159, "STROBEMODERANDOM"),
    ("Lightning", 160, 189, "STROBEMODELIGHTNING"),
    ("Spikes", 190, 219, "STROBEMODERANDOM"),
    ("Sparkle", 220, 255, "STROBEMODERANDOM"),
]


def effects(idx, coarse):
    functions = [
        function(i, start, end, sub, name, "STROBEMODE", "Effects", *SH, "0", "0", [(name, start, end)], name)
        for i, (name, start, end, sub) in enumerate(EFFECTS)
    ]
    return channel(idx, "STROBEMODE", "SHUTTER", "BEAM", coarse, None, "0", "0", ' snap="true"', functions)


def strobe_1ch(idx, coarse):
    return channel(idx, "SHUTTER", "SHUTTER", "BEAM", coarse, None, "0", "100", "", [
        function(0, 0, 0, "SHUTTER", "Shutter", "SHUTTER", "Shutter", *SH, "0", "0", [("closed", 0, 0)], "Off"),
        function(1, 1, 254, "STROBE", "Rate", "SHUTTER", "Shutter", *SH, "0.5", "25",
                 [("0.5 Hz", 1, 1), ("25 Hz", 254, 254)], "Strobe"),
        function(2, 255, 255, "SHUTTER", "Shutter", "SHUTTER", "Shutter", *SH, "1", "1", [("open", 255, 255)], "On"),
    ])


def rgb_channels(idx0, ch0):
    return [rgb(idx0, ch0, 1, "Red", "ff0000"), rgb(idx0 + 1, ch0 + 1, 2, "Green", "00ff00"),
            rgb(idx0 + 2, ch0 + 2, 3, "Blue", "0000ff")]


def atomic_bar(idx0=0, ch0=1):
    return [dimmer(idx0, ch0), duration(idx0 + 1, ch0 + 1), rate(idx0 + 2, ch0 + 2), effects(idx0 + 3, ch0 + 3)]


def module(index, name, size, channels, beam_angle="60"):
    body = ("\n" + "\n".join(channels)) if channels else ""
    return f'''      <Module index="{index}" name="{name}" class="LED" beamtype="Wash" beam_angle="{beam_angle}" beam_intensity="10000">
        <Body>
          <Size x="{size[0]}" y="{size[1]}" z="0.25"/>
        </Body>{body}
      </Module>'''


def instance(index, patch, module_index, name):
    return f'      <Instance index="{index}" patch="{patch}" module_index="{module_index}" name="{escape(name)}"/>'


def fixture_type(name, short, mode, info, modules, instances):
    return f'''{HEADER}
  <FixtureType index="0" name="{name}" mode="{mode}">
    <InfoItems>
      <Info>{escape(info)}</Info>
    </InfoItems>
    <short_name>{short}</short_name>
    <manufacturer>Nailec</manufacturer>
    <short_manufacturer>Nailec</short_manufacturer>
    <Modules index="0">
{chr(10).join(modules)}
    </Modules>
    <Instances index="1">
{chr(10).join(instances)}
    </Instances>
{FOOTER}'''


STROBE_NAME = "THEATRICAL ATOMIC STROBE"
TILT_NAME = "THEATRICAL ATOMIC TILT"
BODY = (0.45, 0.25)
PLATE = (0.4, 0.09)
SEGMENT = (0.044, 0.03)
ZONE = (0.1, 0.09)
EFFECTS_TXT = ("Effects: 0-9 strobe, 10-39 blinder, 40-69 ramp up, 70-99 ramp down, 100-129 ramp up/down, "
               "130-159 random, 160-189 lightning, 190-219 spikes, 220-255 sparkle. Rate 0 stops the strobe.")


def strobe_1() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 1-channel personality: strobe only, 0 off, 1-254 rate "
            "0.5 to 25 Hz, 255 on. Requires mod personality \"1-Channel Strobe\".")
    return fixture_type(STROBE_NAME, "TEL-Atom1", "1CH", info,
                        [module(0, "MAIN", BODY, [strobe_1ch(0, 1)])], [instance(0, 1, 0, "Main")])


def strobe_4() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 4-channel personality (Martin Atomic layout): Dim, Flash "
            f"Duration, Flash Rate, Effects. {EFFECTS_TXT} Requires mod personality \"4-Channel Atomic\".")
    return fixture_type(STROBE_NAME, "TEL-Atom4", "4CH", info,
                        [module(0, "MAIN", BODY, atomic_bar())], [instance(0, 1, 0, "Main")])


def strobe_8() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 8-channel personality (Atomic 3000 LED layout): Dim, Flash "
            "Duration, Flash Rate, Effects on the main fixture, then the Aura sub-fixture 1.1 (Dim, Red, Green, "
            f"Blue) lighting both RGB plates as one continuous backlight. {EFFECTS_TXT} "
            "Requires mod personality \"8-Channel Atomic + Aura\".")
    modules = [module(0, "MAIN", BODY, atomic_bar()),
               module(1, "AURA", PLATE, [dimmer(0, 1)] + rgb_channels(1, 2), "120")]
    return fixture_type(STROBE_NAME, "TEL-Atom8", "8CH Aura", info, modules,
                        [instance(0, 1, 0, "Main"), instance(1, 5, 1, "Aura")])


def strobe_10() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 10-channel compressed personality: Dim, Flash Duration, Flash "
            "Rate, Effects on the main fixture (the white bar), then the Plate sub-fixture 1.1 (Dim, Duration, Rate, "
            f"Red, Green, Blue): all eight RGB zones as one pixel with its own strobe, rate 0 = continuous. {EFFECTS_TXT} "
            "Requires mod personality \"10-Channel Compressed (Bar + Plate)\".")
    plate = [dimmer(0, 1), duration(1, 2), rate(2, 3, "Open", "1")] + rgb_channels(3, 4)
    modules = [module(0, "MAIN", BODY, atomic_bar()), module(1, "PLATE", PLATE, plate, "120")]
    return fixture_type(STROBE_NAME, "TEL-Atom10", "10CH Compressed", info, modules,
                        [instance(0, 1, 0, "Main"), instance(1, 5, 1, "Plate")])


def strobe_40() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 40-channel pixel personality: Dim, Flash Duration, Flash Rate, "
            "Effects on the main fixture, Segment sub-fixtures 1.1 to 1.9 (Dim each, left to right, under the bar's "
            "flashes), Plate 1.10 (Dim, Duration, Rate; rate 0 = continuous) and Zone sub-fixtures 1.11 to 1.18 "
            f"(Red, Green, Blue each; 1-4 top plate, 5-8 bottom plate, left to right). {EFFECTS_TXT} "
            "Requires mod personality \"40-Channel Pixel (Bar + Plate)\".")
    modules = [
        module(0, "MAIN", BODY, atomic_bar()),
        module(1, "SEGMENT", SEGMENT, [dimmer(0, 1)]),
        module(2, "PLATE", PLATE, [dimmer(0, 1), duration(1, 2), rate(2, 3, "Open", "1")], "120"),
        module(3, "ZONE", ZONE, rgb_channels(0, 1), "120"),
    ]
    instances = [instance(0, 1, 0, "Main")]
    for s in range(9):
        instances.append(instance(1 + s, 5 + s, 1, f"Segment {s + 1}"))
    instances.append(instance(10, 14, 2, "Plate"))
    for z in range(8):
        instances.append(instance(11 + z, 17 + 3 * z, 3, f"Zone {z + 1}"))
    return fixture_type(STROBE_NAME, "TEL-Atom40", "40CH Pixel", info, modules, instances)


def strobe_400() -> str:
    info = ("Theatrical Extra Lights Atomic Strobe, 400-channel pixel map personality: Dim, Flash Duration, Flash Rate, "
            "Effects on the main fixture, Segment sub-fixtures 1.1 to 1.9 (Dim each, left to right), Plate 1.10 (Dim, "
            "Duration, Rate; rate 0 = continuous) and Pixel sub-fixtures 1.11 to 1.106 (Dim, Red, Green, Blue each): "
            "pixels 1-48 on the top plate, 49-96 on the bottom plate, each plate 12 columns x 4 rows, row by row from "
            f"the top, left to right. {EFFECTS_TXT} Requires mod personality \"400-Channel Pixel Map (Bar + 96 Pixels)\".")
    modules = [
        module(0, "MAIN", BODY, atomic_bar()),
        module(1, "SEGMENT", SEGMENT, [dimmer(0, 1)]),
        module(2, "PLATE", PLATE, [dimmer(0, 1), duration(1, 2), rate(2, 3, "Open", "1")], "120"),
        module(3, "PIXEL", (0.033, 0.022), [dimmer(0, 1)] + rgb_channels(1, 2), "120"),
    ]
    instances = [instance(0, 1, 0, "Main")]
    for s in range(9):
        instances.append(instance(1 + s, 5 + s, 1, f"Segment {s + 1}"))
    instances.append(instance(10, 14, 2, "Plate"))
    for p in range(96):
        plate = "T" if p < 48 else "B"
        row = (p % 48) // 12 + 1
        col = p % 12 + 1
        instances.append(instance(11 + p, 17 + 4 * p, 3, f"Px {plate}{row}-{col}"))
    return fixture_type(STROBE_NAME, "TEL-Atom400", "400CH PixelMap", info, modules, instances)


def tilt_6() -> str:
    info = ("Theatrical Extra Lights Atomic Tilt, 6-channel Atomic personality: Dim, Flash Duration, Flash Rate, "
            f"Effects, Focus, Tilt (white head). {EFFECTS_TXT} Requires mod personality \"6-Channel Atomic + Focus + Tilt\".")
    return fixture_type(TILT_NAME, "TEL-ATilt6", "6CH Atomic", info,
                        [module(0, "MAIN", (0.4, 0.3), atomic_bar() + [focus(4, 5), tilt(5, 6)], "40")], [instance(0, 1, 0, "Main")])


def tilt_9() -> str:
    info = ("Theatrical Extra Lights Atomic Tilt, 9-channel Atomic RGB personality: Dim, Flash Duration, Flash Rate, "
            f"Effects, Red, Green, Blue, Focus, Tilt. {EFFECTS_TXT} Requires mod personality \"9-Channel Atomic RGB + Focus + Tilt\".")
    channels = atomic_bar() + rgb_channels(4, 5) + [focus(7, 8), tilt(8, 9)]
    return fixture_type(TILT_NAME, "TEL-ATilt9", "9CH Atomic RGB", info,
                        [module(0, "MAIN", (0.4, 0.3), channels, "40")], [instance(0, 1, 0, "Main")])


FILES = {
    "nailec@theatrical_atomic_strobe@1ch.xml": strobe_1,
    "nailec@theatrical_atomic_strobe@4ch.xml": strobe_4,
    "nailec@theatrical_atomic_strobe@8ch_aura.xml": strobe_8,
    "nailec@theatrical_atomic_strobe@10ch_compressed.xml": strobe_10,
    "nailec@theatrical_atomic_strobe@40ch_pixel.xml": strobe_40,
    "nailec@theatrical_atomic_strobe@400ch_pixelmap.xml": strobe_400,
    "nailec@theatrical_atomic_tilt@6ch_atomic.xml": tilt_6,
    "nailec@theatrical_atomic_tilt@9ch_atomic_rgb.xml": tilt_9,
}

if __name__ == "__main__":
    out_dir = sys.argv[1]
    for name, build in FILES.items():
        with open(f"{out_dir}/{name}", "w", encoding="utf-8") as f:
            f.write(build())
    print("ok", len(FILES), "files")
