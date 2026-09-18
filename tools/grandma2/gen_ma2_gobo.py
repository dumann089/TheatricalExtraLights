"""Generate grandMA2 fixture XMLs for the Theatrical Extra Lights gobo heads.

Conventions copied from manufacturer files (Ayrton Diablo, Robe iForte) on the user's library:
- ChannelFunction from/to are PERCENT of the 8-bit range: from = start/256*100, to = (end+1)/256*100
- min_dmx_24 = start*65536, max_dmx_24 = (end+1)*65536-1
- ChannelType default is DMX (8-bit, or 16-bit when a fine channel exists)
- one function per contiguous DMX range, ranges never overlap
"""
import sys
from xml.sax.saxutils import escape

def pct(dmx):
    return f"{dmx / 256 * 100:.3f}".rstrip("0").rstrip(".")

def function(idx, start, end, sub, sub_name, attr, attr_name, feat, feat_name, preset, preset_name,
             physfrom, physto, sets, name=None):
    lo = start * 65536
    hi = (end + 1) * 65536 - 1
    nm = f' name="{escape(name)}"' if name else ""
    out = [f'          <ChannelFunction index="{idx}"{nm} from="{pct(start)}" to="{pct(end + 1)}" '
           f'min_dmx_24="{lo}" max_dmx_24="{hi}" physfrom="{physfrom}" physto="{physto}" '
           f'subattribute="{sub}" subattribute_user_name="{escape(sub_name)}" attribute="{attr}" '
           f'attribute_user_name="{escape(attr_name)}" feature="{feat}" feature_user_name="{escape(feat_name)}" '
           f'preset="{preset}" preset_user_name="{escape(preset_name)}">']
    for i, (sname, s0, s1) in enumerate(sets):
        out.append(f'            <ChannelSet index="{i}" name="{escape(sname)}" from_dmx="{s0}" to_dmx="{s1}"/>')
    out.append("          </ChannelFunction>")
    return "\n".join(out)

def channel(idx, attr, feat, preset, coarse, fine=None, default="0", highlight=None, extra="", functions=()):
    f = f' fine="{fine}"' if fine else ""
    h = f' highlight_value="{highlight}"' if highlight is not None else ""
    head = f'        <ChannelType index="{idx}" attribute="{attr}" feature="{feat}" preset="{preset}" coarse="{coarse}"{f} default="{default}"{h}{extra}>'
    return "\n".join([head, *functions, "        </ChannelType>"])

def simple(idx, attr, feat, feat_name, preset, preset_name, coarse, name, sets, default="0", highlight=None,
           physfrom="0", physto="1", fine=None, sub=None, extra=""):
    return channel(idx, attr, feat, preset, coarse, fine, default, highlight, extra, [
        function(0, 0, 255, sub or attr, name, attr, name, feat, feat_name, preset, preset_name, physfrom, physto, sets)
    ])

# ── Building blocks ─────────────────────────────────────────────────────────

def dimmer(idx, coarse, fine=None):
    return simple(idx, "DIM", "DIMMER", "Dimmer", "DIMMER", "Dimmer", coarse, "Dim",
                  [("closed", 0, 0), ("open", 255, 255)], default="0", highlight="100", fine=fine,
                  extra=' react_to_master="1"')

def rgb(idx, coarse, n, name, color):
    return simple(idx, f"COLORRGB{n}", "COLORRGB", "Color RGB", "COLOR", "Color", coarse, name,
                  [("off", 0, 0), ("full", 255, 255)], default="255", highlight="100", extra=f' color="{color}"')

def cmy(idx, coarse, n, name, color):
    return simple(idx, f"COLORMIX{n}", "COLORMIX", "Color Mix", "COLOR", "Color", coarse, name,
                  [("off", 0, 0), ("full", 255, 255)], default="0", highlight="0", extra=f' color="{color}"')

def focus(idx, coarse):
    return simple(idx, "FOCUS", "FOCUS", "Focus", "FOCUS", "Focus", coarse, "Focus",
                  [("near", 0, 0), ("mid", 128, 128), ("far", 255, 255)], default="128", highlight="50")

def zoom(idx, coarse):
    return simple(idx, "ZOOM", "FOCUS", "Focus", "FOCUS", "Focus", coarse, "Zoom",
                  [("narrow", 0, 0), ("normal", 128, 128), ("wide", 255, 255)], default="0", highlight="50", physfrom="1", physto="19")

def pan(idx, coarse, fine=None):
    default = "32768" if fine else "128"
    center = 32768 if fine else 128
    return channel(idx, "PAN", "POSITION", "POSITION", coarse, fine, default, "50", "", [
        function(0, 0, 255, "PAN", "Pan", "PAN", "Pan", "POSITION", "Position", "POSITION", "Position",
                 "-180", "180", [("center", center, center)])
    ])

def tilt(idx, coarse, fine=None):
    # tilt 0 deg = (0 + 225) / 270 of the range
    default = str(round(225 / 270 * 65535)) if fine else str(round(225 / 270 * 255))
    center = int(default)
    return channel(idx, "TILT", "POSITION", "POSITION", coarse, fine, default, "50", "", [
        function(0, 0, 255, "TILT", "Tilt", "TILT", "Tilt", "POSITION", "Position", "POSITION", "Position",
                 "-225", "45", [("center", center, center)])
    ])

def gobo_wheel(idx, coarse):
    sets = [("open", 0, 15)] + [(f"Gobo {i}", 16 * (i - 1), 16 * i - 1) for i in range(2, 17)]
    return channel(idx, "GOBO1", "GOBO1", "GOBO", coarse, None, "0", "0", ' snap="true"', [
        function(0, 0, 255, "GOBO1", "Select", "GOBO1", "G1", "GOBO1", "Gobo1", "GOBO", "Gobo", "1", "16", sets)
    ])

def gobo_spin(idx, coarse):
    return channel(idx, "GOBO1_POS", "GOBO1", "GOBO", coarse, None, "0", "0", "", [
        function(0, 0, 255, "GOBO1_ROT", "Rotate", "GOBO1_POS", "G1<>", "GOBO1", "Gobo1", "GOBO", "Gobo", "0", "12",
                 [("stop", 0, 0), ("slow", 32, 32), ("mid", 128, 128), ("fast", 255, 255)])
    ])

def blade(idx, coarse, n, side):
    a = f"BLADE{n}{side}"
    return channel(idx, a, "SHAPER", "SHAPERS", coarse, None, "0", "0", "", [
        function(0, 0, 255, a, f"{n}{side}", a, f"{n}{side}", "SHAPER", "Frames", "SHAPERS", "Shapers", "0", "1",
                 [(f"min {n}{side}", 0, 0), (f"max {n}{side}", 255, 255)])
    ])

def shaper_rot(idx, coarse):
    return channel(idx, "SHAPER ROT", "SHAPER", "SHAPERS", coarse, None, "127.5", "50", "", [
        function(0, 0, 255, "SHAPER ROT", "Index", "SHAPER ROT", "FrameAssembly", "SHAPER", "Frames", "SHAPERS", "Shapers",
                 "-55", "55", [("max CCW", 0, 0), ("zero", 128, 128), ("max CW", 255, 255)])
    ])

def shutter(idx, coarse):
    S = ("SHUTTER", "Shutter", "SHUTTER", "Shutter", "SHUTTER", "Shutter", "BEAM", "Beam")
    return channel(idx, "SHUTTER", "SHUTTER", "BEAM", coarse, None, "255", "100", "", [
        function(0, 0, 0, *S, "0", "0", [("closed", 0, 0)], "Closed"),
        function(1, 1, 254, "STROBE", "Strobe", "SHUTTER", "Shutter", "SHUTTER", "Shutter", "BEAM", "Beam",
                 "0.5", "10", [("slow", 1, 1), ("fast", 254, 254)], "Strobe"),
        function(2, 255, 255, *S, "1", "1", [("open", 255, 255)], "Open"),
    ])

def color_wheel(idx, coarse):
    colors = ["Red", "Orange", "Yellow", "Green", "Light Blue", "Blue", "Magenta", "CTO", "Congo"]
    sets = [("open", 0, 9)] + [(c, 10 + 10 * i, 19 + 10 * i) for i, c in enumerate(colors)] + [("open (2)", 100, 127)]
    C = ("COLOR1", "C1", "COLOR1", "Color1", "COLOR", "Color")
    return channel(idx, "COLOR1", "COLOR1", "COLOR", coarse, None, "0", "0", "", [
        function(0, 0, 127, "COLOR1", "Select", *C, "0", "10", sets),
        function(1, 128, 191, "COLOR1", "Select", *C, "0.3", "30", [("rotate CW slow", 128, 128), ("rotate CW fast", 191, 191)], "Rotate CW"),
        function(2, 192, 255, "COLOR1", "Select", *C, "-30", "-0.3", [("rotate CCW fast", 192, 192), ("rotate CCW slow", 255, 255)], "Rotate CCW"),
    ])

def prism(idx, coarse):
    P = ("PRISMA1", "Prism", "BEAM1", "Beam", "BEAM", "Beam")
    return channel(idx, "PRISMA1", "BEAM1", "BEAM", coarse, None, "0", "0", "", [
        function(0, 0, 127, "PRISMA1", "Prism", *P, "0", "0", [("open", 0, 127)], "Open"),
        function(1, 128, 170, "PRISMA1", "Prism", *P, "3", "3", [("3-facet", 128, 170)], "3-facet prism"),
        function(2, 171, 213, "PRISMA1", "Prism", *P, "6", "6", [("6-facet", 171, 213)], "6-facet prism"),
        function(3, 214, 255, "PRISMA1", "Prism", *P, "9", "9", [("9-facet", 214, 255)], "9-facet prism"),
    ])

def prism_rot(idx, coarse):
    P = ("PRISMA1_POS", "P1<>", "BEAM1", "Beam", "BEAM", "Beam")
    return channel(idx, "PRISMA1_POS", "BEAM1", "BEAM", coarse, None, "0", "0", "", [
        function(0, 0, 127, "PRISMA1_POS", "Index", *P, "0", "360", [("zero", 0, 0), ("half", 64, 64), ("max CW", 127, 127)], "Index"),
        function(1, 128, 191, "PRISMA1_ROT", "Rotate", *P, "0.3", "30", [("> slow", 128, 128), (">>> fast", 191, 191)], "Rotate CW"),
        function(2, 192, 255, "PRISMA1_ROT", "Rotate", *P, "-0.3", "-30", [("< slow", 192, 192), ("<<< fast", 255, 255)], "Rotate CCW"),
    ])

def anim_wheel(idx, coarse):
    A = ("ANIMATIONWHEEL1", "Anim", "ANIMATIONWHEEL1", "Anim", "ANIMATIONWHEEL1", "Animation", "GOBO", "Gobo")
    return channel(idx, "ANIMATIONWHEEL1", "ANIMATIONWHEEL1", "GOBO", coarse, None, "0", "0", ' snap="true"', [
        function(0, 0, 15, *A, "0", "0", [("open", 0, 15)], "Open"),
        function(1, 16, 75, *A, "1", "1", [("flames", 16, 75)], "Flames"),
        function(2, 76, 135, *A, "2", "2", [("water", 76, 135)], "Water"),
        function(3, 136, 195, *A, "3", "3", [("clouds", 136, 195)], "Clouds"),
        function(4, 196, 255, *A, "4", "4", [("breakup", 196, 255)], "Breakup"),
    ])

def anim_rot(idx, coarse):
    A = ("ANIMATIONWHEEL1_POS", "Anim<>", "ANIMATIONWHEEL1", "Animation", "GOBO", "Gobo")
    return channel(idx, "ANIMATIONWHEEL1_POS", "ANIMATIONWHEEL1", "GOBO", coarse, None, "0", "0", "", [
        function(0, 0, 127, "ANIMATIONWHEEL1_POS", "Index", *A, "0", "360", [("zero", 0, 0), ("max", 127, 127)], "Index"),
        function(1, 128, 191, "ANIMATIONWHEEL1_ROT", "Rotate", *A, "0.1", "1.5", [("> slow", 128, 128), (">>> fast", 191, 191)], "Scroll"),
        function(2, 192, 255, "ANIMATIONWHEEL1_ROT", "Rotate", *A, "-0.1", "-1.5", [("< slow", 192, 192), ("<<< fast", 255, 255)], "Scroll reverse"),
    ])

def frost(idx, coarse):
    return simple(idx, "FROST", "BEAM1", "Beam", "BEAM", "Beam", coarse, "Frost",
                  [("min Frost", 0, 0), ("max Frost", 255, 255)], default="0", highlight="0")

def pt_speed(idx, coarse):
    S = ("PTSPEED", "P/T Speed", "POSITION", "Position", "CONTROL", "Control")
    return channel(idx, "PTSPEED", "POSITION", "CONTROL", coarse, None, "0", "0", "", [
        function(0, 0, 2, "PTSPEED", "P/T Speed", *S, "1", "1", [("tracking", 0, 2)], "Tracking"),
        function(1, 3, 255, "PTSPEED", "P/T Speed", *S, "1", "0", [("fast", 3, 3), ("slow", 255, 255)], "Speed"),
    ])

# ── Fixtures ────────────────────────────────────────────────────────────────

def fixture(mode, short, info, channels):
    body = "\n".join(channels)
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<MA xmlns:xml="http://www.w3.org/XML/1998/namespace" major_vers="3" minor_vers="2" stream_vers="2">
  <Info datetime="2026-9-13T14:00:00" showfile="ma fixture builder"/>
  <FixtureType index="0" name="THEATRICAL GOBO" mode="{escape(mode)}">
    <InfoItems>
      <Info>{escape(info)}</Info>
    </InfoItems>
    <short_name>{short}</short_name>
    <manufacturer>Nailec</manufacturer>
    <short_manufacturer>Nailec</short_manufacturer>
    <Modules index="0">
      <Module index="0" name="MAIN" class="Headmover" beamtype="Spot" beam_angle="1" beam_intensity="12000">
        <Body>
          <Size x="0.45" y="0.45" z="0.55"/>
        </Body>
{body}
      </Module>
    </Modules>
    <Instances index="1">
      <Instance index="0" patch="1" module_index="0" name="Main"/>
    </Instances>
    <Wheels index="2"/>
    <VirtualFunctionBlocks index="3"/>
    <FixtureMacroCollect index="5"/>
  </FixtureType>
</MA>
'''

HEADS = "Spot Xtreme, VL6C, Iris 700, Pro Spot, Mini Scan, Mini Spot, Moving Scan Beams, Moving VL2C Beams"

def base10(idx0=0, ch0=1):
    return [
        dimmer(idx0 + 0, ch0 + 0),
        rgb(idx0 + 1, ch0 + 1, 1, "Red", "ff0000"),
        rgb(idx0 + 2, ch0 + 2, 2, "Green", "00ff00"),
        rgb(idx0 + 3, ch0 + 3, 3, "Blue", "0000ff"),
        focus(idx0 + 4, ch0 + 4),
        pan(idx0 + 5, ch0 + 5),
        tilt(idx0 + 6, ch0 + 6),
        gobo_wheel(idx0 + 7, ch0 + 7),
        zoom(idx0 + 8, ch0 + 8),
        gobo_spin(idx0 + 9, ch0 + 9),
    ]

def blades(idx0, ch0):
    out = []
    i = idx0
    c = ch0
    for n in (1, 2, 3, 4):
        for side in ("A", "B"):
            out.append(blade(i, c, n, side))
            i += 1
            c += 1
    out.append(shaper_rot(i, c))
    return out

def fixture_19():
    return fixture("19CH Framing Shutters", "TEL-GoboFS",
                   f"Theatrical Extra Lights gobo heads ({HEADS}) in the 19ch personality: the 10 standard channels "
                   f"then a 4-blade framing module (A/B corners per blade, blade 1 top, 2 right, 3 bottom, 4 left) and "
                   f"frame rotation -55..+55 deg. Requires mod personality \"19ch - Framing Shutters\".",
                   base10() + blades(10, 11))

def fixture_29():
    ch = [
        shutter(0, 1),
        dimmer(1, 2, fine=3),
        rgb(2, 4, 1, "Red", "ff0000"),
        rgb(3, 5, 2, "Green", "00ff00"),
        rgb(4, 6, 3, "Blue", "0000ff"),
        gobo_wheel(5, 7),
        gobo_spin(6, 8),
        prism(7, 9),
        prism_rot(8, 10),
        anim_wheel(9, 11),
        anim_rot(10, 12),
        frost(11, 13),
        zoom(12, 14),
        focus(13, 15),
        pan(14, 16, fine=17),
        tilt(15, 18, fine=19),
        pt_speed(16, 20),
    ] + blades(17, 21)
    return fixture("29CH Profile 16bit", "TEL-Profile",
                   f"Theatrical Extra Lights gobo heads ({HEADS}) in the 29ch Profile 16bit personality: shutter/strobe, "
                   f"16-bit dimmer, RGB, gobo wheel + rotation, 3/6/9-facet prism + rotation, animation wheel + rotation, "
                   f"frost, zoom, focus, 16-bit pan/tilt, pan/tilt speed, 4 framing blades A/B + frame rotation. "
                   f"Requires mod personality \"29ch - Profile 16bit\".",
                   ch)

if __name__ == "__main__":
    out_dir = sys.argv[1]
    with open(f"{out_dir}/nailec@theatrical_gobo@19ch_framing_shutters.xml", "w", encoding="utf-8") as f:
        f.write(fixture_19())
    with open(f"{out_dir}/nailec@theatrical_gobo@29ch_profile_16bit.xml", "w", encoding="utf-8") as f:
        f.write(fixture_29())
    print("ok")
