"""Generate one wiki page per fixture (docs/fixtures/<id>.md), the family pages and the
sidebar JSON, from the channel model below. Run from the docs/ folder:

    python generate-fixtures.py

Keep this file as the single source of truth for channel documentation."""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "fixtures")
SIDEBAR = os.path.join(HERE, ".vitepress", "fixtures-sidebar.json")

# ── Channel library ─────────────────────────────────────────────────────────
# name, short value column, detailed paragraph(s)

def ch(name, values, text):
    return {"name": name, "values": values, "text": text}


INTENSITY = ch("Intensity", "0 off to 255 full",
               "Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.")
INTENSITY_JET = ch("Jet level", "0 off to 255 full",
                   "Turns the water on and sets its level. 0 is off; higher values raise the flow, which makes the jet taller and denser up to the height set in the fixture screen.")
RED = ch("Red", "0 to 255", "Red component of the additive colour mix.")
GREEN = ch("Green", "0 to 255", "Green component of the additive colour mix.")
BLUE = ch("Blue", "0 to 255",
          "Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.")
FOCUS = ch("Focus", "0 to 255",
           "Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.")
FOCUS_CONE = ch("Focus", "0 tight to 255 wide",
                "Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.")
PAN = ch("Pan", "0 to 255 = −180° to 180°",
         "Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.")
TILT = ch("Tilt", "0 to 255 = −225° to 45°",
          "Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.")
GOBO_WHEEL = ch("Gobo wheel", "Equal ranges, one per slot",
                "Selects the gobo. The channel is split into as many equal ranges as the wheel has slots (10 on most wheels): 0 is the open slot, then each following range is one gobo. Moving between slots animates the wheel and cross-fades the two textures in the beam and on the surface. Any slot can carry a [custom image](/guide/custom-gobos).")
GOBO_ROT = ch("Gobo rotation", "0 stop, 1 to 255 speed",
              "Continuous rotation of the gobo. 0 is stopped; higher values spin faster. The direction is fixed.")
ZOOM = ch("Zoom", "0 narrow (1°) to 255 wide (19°)",
          "Beam angle from 1° to 19°. Zoom changes the cone of the volumetric beam and the size of the projected gobo together; combine with focus for a sharp or soft edge at any size.")
PRISM_10 = ch("Prism / gobo select", "0 to 255", "Selects the prism or gobo slot of the extended mode.")
PRISM_ZOOM_10 = ch("Prism zoom", "0 to 255", "Size of the prism effect.")
PRISM_ROT_10 = ch("Prism rotation", "0 stop, 1 to 255 speed", "Rotation speed of the prism or gobo.")
STROBE = ch("Strobe", "0 closed, 1 to 254 strobe, 255 open",
            "Mechanical shutter. 0 is closed (dark regardless of the dimmer), 1 to 254 strobes from slow to fast, 255 is open. Use 255 for normal operation and drop to 0 for a hard blackout that keeps the dimmer level.")
BLADE_A = lambda n, side: ch(f"Blade {n} {side}", "0 out to 255 in",
                             f"Corner {side} of blade {n} ({['top', 'right', 'bottom', 'left'][n - 1]} blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.")
FRAME_ROT = ch("Frame rotation", "0 = −55°, 128 neutral, 255 = +55°",
               "Rotates the whole framing module. 127 and 128 are neutral; lower values turn it counter-clockwise up to −55°, higher values clockwise up to +55°.")

SHUTTER_PRO = ch("Shutter", "0 closed, 1 to 254 strobe 0.5 to 10 Hz, 255 open",
                 "Mechanical shutter. 0 closed, 255 open. In between the shutter strobes, from 0.5 Hz at 1 to 10 Hz at 254; the frequency is quantised to whole game ticks, so 10 Hz is the physical maximum and is reached around 80 %. The beam, the projected gobo and the coloured dynamic light all follow the shutter.")
DIM_COARSE = ch("Dimmer coarse", "16 bit with the next channel",
                "High byte of the 16-bit dimmer. Patch it as a 16-bit attribute on the desk: the fixture combines it with the fine channel for 65 536 steps, then scales to the rendered intensity.")
DIM_FINE = ch("Dimmer fine", "0 to 255", "Low byte of the 16-bit dimmer.")
PRISM = ch("Prism", "0 to 127 out, 128 to 170 three facets, 171 to 213 six, 214 to 255 nine",
           "Inserts the prism. Below 128 the beam is single. 128 to 170 splits it into three copies, 171 to 213 into six, 214 to 255 into nine, arranged in a ring whose spread grows with the count. Each copy is drawn at 75 %, 60 % or 50 % intensity so the total light stays plausible. The projected gobo is split the same way.")
PRISM_ROT = ch("Prism rotation", "0 to 127 indexed 0° to 360°, 128 to 191 CW slow to fast, 192 to 255 CCW slow to fast",
               "Position or rotation of the prism ring. 0 to 127 sets a fixed angle. 128 to 191 spins clockwise, 192 to 255 counter-clockwise, slow at the start of each range and fast at the end. Switching from indexed to continuous starts the spin from the indexed angle, without a jump.")
ANIM_WHEEL = ch("Animation wheel", "0 to 15 out, 16 to 75 flames, 76 to 135 water, 136 to 195 clouds, 196 to 255 breakup",
                "Puts an effect texture in front of the gate. It modulates the beam and the projected spot: flames for fire, water for caustics, clouds for soft billows, breakup for a dappled foliage look. Combine it with a gobo, for example a window with flames behind it.")
ANIM_ROT = ch("Animation rotation", "0 to 127 orientation 0° to 360°, 128 to 191 scroll slow to fast, 192 to 255 reverse scroll",
              "0 to 127 sets the orientation of the wheel and keeps it still. 128 to 191 scrolls the texture forward, 192 to 255 backward, slow to fast (about 0.1 to 1.5 texture widths per second). The scroll direction follows the last indexed orientation.")
FROST = ch("Frost", "0 none to 255 full",
           "Diffusion filter. It softens the edge of the projected spot like an extra defocus. The volumetric beam width is not affected.")
PAN_COARSE = ch("Pan coarse", "16 bit, −180° to 180°",
                "High byte of the 16-bit pan. With the fine channel the head resolves 65 536 positions over a full turn, so slow moves are stepless.")
PAN_FINE = ch("Pan fine", "0 to 255", "Low byte of the 16-bit pan.")
TILT_COARSE = ch("Tilt coarse", "16 bit, −225° to 45°",
                 "High byte of the 16-bit tilt. Default position on a fresh fixture is 0°, straight along the head axis.")
TILT_FINE = ch("Tilt fine", "0 to 255", "Low byte of the 16-bit tilt.")
PT_SPEED = ch("Pan/tilt speed", "0 to 2 tracking, 3 fast (720°/s) to 255 slow (25°/s)",
              "Motor speed for pan and tilt. 0 to 2 is tracking: the head follows the desk instantly, use it when the desk itself fades positions. From 3 the head moves on its own motor model, 720°/s at 3 down to 25°/s at 255, with easing at the end of the move. Ideal for slow theatrical travels driven by a snap on the desk.")

LASER_CH = [
    INTENSITY,
    ch("Colour 1 Red", "0 to 255", "Red of the first colour group. The pattern is drawn with a gradient from colour 1 through colour 2 to colour 3."),
    ch("Colour 1 Green", "0 to 255", "Green of the first colour group."),
    ch("Colour 1 Blue", "0 to 255", "Blue of the first colour group."),
    ch("Colour 2 Red", "0 to 255", "Red of the second colour group. A group left entirely at 0 falls back to the previous group, so a single colour only needs group 1."),
    ch("Colour 2 Green", "0 to 255", "Green of the second colour group."),
    ch("Colour 2 Blue", "0 to 255", "Blue of the second colour group."),
    ch("Colour 3 Red", "0 to 255", "Red of the third colour group."),
    ch("Colour 3 Green", "0 to 255", "Green of the third colour group."),
    ch("Colour 3 Blue", "0 to 255", "Blue of the third colour group."),
    ch("Pattern", "14 ranges of 18",
       "Selects the shape: 0 to 17 single beam, 18 to 35 line, 36 to 53 circle, 54 to 71 square, 72 to 89 wave, 90 to 107 tunnel, 108 to 125 star, 126 to 143 cross, 144 to 161 triangle, 162 to 179 spiral, 180 to 197 parallel lines, 198 to 215 double circle, 216 to 233 burst, 234 to 255 scatter."),
    ch("Size", "0 to 255", "Overall size of the pattern, 0 to 100 %."),
    ch("Amplitude", "0 to 255", "Depth of the pattern's modulation (wave height, tunnel depth, star points and so on)."),
    ch("Speed", "0 to 255", "Animation speed. Below about 5 % (value 13) the pattern is frozen; above, it animates faster with the value."),
    ch("Rotation", "0 to 255 = 0° to 360°", "Static rotation of the whole pattern."),
    ch("Pan", "0 to 255 = −80° to 80°", "Horizontal aim of the projector, 128 straight ahead."),
    ch("Tilt", "0 to 255 = 45° to −45°", "Vertical aim, centred on 127. Higher values point down."),
    FOCUS,
    ch("Persistence", "0 to 255", "Length of the trail left by moving beams on the client. 0 is a clean beam, higher values leave a longer afterglow like a slow camera shutter."),
]

FIRE = ch("Fire", "0 idle, rising edge = one shot, 2 to 255 = 1 to 10 shots/s",
          "Trigger and rate. 0 is idle. Any rise from 0 fires one shot; a value of 1 stays single-shot, so bump the channel to fire once. From 2 to 255 the launcher fires continuously, from 1 shot per second at 2 to 10 per second at 255. Nothing fires while the machine is **disarmed** in its screen.")
LAUNCH_TILT = ch("Tilt", "0 to 255 = 0° to 180°",
                 "Launch pitch. 0 is horizontal, about 128 is straight up (90°), 255 is horizontal the other way. Aim launchers before the show; the shells keep the angle for their whole flight.")
LAUNCH_POWER = ch("Power", "0 to 255 = 0.7× to 2.6×",
                  "Launch power, a multiplier on the shell speed. Low values keep the burst close to the stage, high values send it high and far.")

# ── Personalities ───────────────────────────────────────────────────────────

def P(name, channels, note=None):
    return {"name": name, "channels": channels, "note": note}


P_10CH_GOBO = P("10-Channel Mode", [INTENSITY, RED, GREEN, BLUE, FOCUS, PAN, TILT, GOBO_WHEEL, ZOOM, GOBO_ROT],
                "The simplest patch. Framing blades stay out, prism and animation wheel are unavailable.")
P_19CH = P("19ch - Framing Shutters",
           [INTENSITY, RED, GREEN, BLUE, FOCUS, PAN, TILT, GOBO_WHEEL, ZOOM, GOBO_ROT]
           + [BLADE_A(n, s) for n in (1, 2, 3, 4) for s in ("A", "B")] + [FRAME_ROT],
           "Channels 1 to 10 are identical to the 10-Channel Mode; the framing module follows.")
P_29CH = P("29ch - Profile 16bit",
           [SHUTTER_PRO, DIM_COARSE, DIM_FINE, RED, GREEN, BLUE, GOBO_WHEEL, GOBO_ROT, PRISM, PRISM_ROT, ANIM_WHEEL, ANIM_ROT,
            FROST, ZOOM, FOCUS, PAN_COARSE, PAN_FINE, TILT_COARSE, TILT_FINE, PT_SPEED]
           + [BLADE_A(n, s) for n in (1, 2, 3, 4) for s in ("A", "B")] + [FRAME_ROT],
           "The full profile fixture. Defaults on a fresh fixture: shutter open, dimmer 0, RGB white, prism and animation out, frost 0, pan centred, tilt 0°, speed tracking, blades out. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode.")
P_7CH = P("7ch - Standard", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE, PAN, TILT])
P_7CH_ONLY = P("7-Channel Mode", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE, PAN, TILT])
P_10CH_EXT = P("10ch - Extended", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE, PAN, TILT, PRISM_10, PRISM_ZOOM_10, PRISM_ROT_10],
               "Channels 1 to 7 as in the standard mode, plus the prism group.")
P_4CH = P("4-Channel Mode", [INTENSITY, RED, GREEN, BLUE])
P_4CH_IRGB = P("4-Channel iRGB", [INTENSITY, RED, GREEN, BLUE])
P_1CH = P("1-Channel Mode", [INTENSITY], "The colour is fixed by the block.")
P_1CH_JET = P("1-Channel Mode", [INTENSITY_JET], "Height, thickness and, where available, cone angle or spread are set in the fixture screen, not on DMX.")

COLOR_PRESETS = ["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Magenta", "Lightblue", "White"]
P_PRESETS = [P(c, [ch("Intensity", "0 off to 255 full", f"Master dimmer of the fixed {c.lower()} colour.")]) for c in COLOR_PRESETS] + \
            [P("iRGB", [INTENSITY, RED, GREEN, BLUE], "Free colour mixing instead of a fixed preset.")]

# ── Fixtures ────────────────────────────────────────────────────────────────

_FAMILY = None
FAMILIES = {"Gobo moving heads", "Moving heads & beams", "Wash, spot & followspot", "PARs & LED panels", "Blinders & strobes",
            "Lasers & effects", "Water jets", "Pyro & fireworks"}


def F(id_, name, *args, notes=None, hero=None):
    """F(id, name, family, intro, personalities[, notes]) or F(id, name, intro, personalities[, notes])
    with the family taken from the current _FAMILY section."""
    args = list(args)
    if len(args) >= 3 and args[0] in FAMILIES:
        family, intro, personalities = args[0], args[1], args[2]
        rest = args[3:]
    else:
        family, intro, personalities = _FAMILY, args[0], args[1]
        rest = args[2:]
    if rest:
        notes = rest[0]
    return {"id": id_, "name": name, "family": family, "intro": intro, "personalities": personalities,
            "notes": notes or [], "hero": hero}


GOBO_NOTES = [
    "Hangs from a truss or stands on the floor; the geometry flips automatically when hung.",
    "The config screen shows a live **Gobo & framing shutters** preview and, in 29ch mode, the **Profile 16bit (live DMX)** card with every decoded value.",
    "Custom gobos apply to the wheel this head uses, so all heads sharing the wheel show them.",
    "Behaviour guide: [Gobo heads & personalities](/guide/gobo-heads). Desk file: [grandMA2 fixture files](/guide/grandma2).",
]
GOBO_HEADS = [
    ("spot_xtreme_gobo", "Xtreme Spot (Gobos)", "Large profile spot with a gobo wheel."),
    ("vl6c_gobo", "VL6C Spot (Gobos)", "Compact spot in the VL6 body, with a gobo wheel."),
    ("iris_700_gobo", "Iris 700 Spot (Gobos)", "Mid-size profile spot with a gobo wheel."),
    ("pro_spot_gobo", "Pro Spot LX (Gobos)", "Profile spot in a long body, with a gobo wheel."),
    ("mini_scan_gobo", "Mini Scan (Gobos)", "Small mirror scanner with a gobo wheel."),
    ("mini_spot_gobo", "Mini Spot (Gobos)", "Small spot with a gobo wheel, for tight rigs."),
    ("moving_scan_beams", "Moving Scan (Gobos)", "Scanner body with beam-style gobos."),
    ("moving_vl2c_beams", "Moving VL2 (Gobos)", "VL2 body with beam-style gobos."),
]

FIXTURES = []
for id_, name, intro in GOBO_HEADS:
    FIXTURES.append(F(id_, name, "Gobo moving heads",
                      intro + " One of the eight gobo heads: it shares the 10ch, 19ch framing shutters and 29ch Profile 16bit personalities and the same rendering engine.",
                      [P_10CH_GOBO, P_19CH, P_29CH], GOBO_NOTES))

MH_NOTES = ["Hangs from a truss or stands on the floor; hung upside down, pan and tilt are mirrored automatically.",
            "Right-click opens the config screen; pan and tilt are DMX-driven, so there are no position sliders."]
for id_, name, intro in [
    ("moving_vl6", "Moving VL6 (7Ch/10Ch)", "Classic wash-style moving head."),
    ("moving_vl2c", "Moving VL2 (7Ch/10Ch)", "Moving head in the VL2 body."),
    ("moving_beam", "Moving Beam (7Ch/10Ch)", "Tight beam moving head."),
    ("moving_scan", "Moving Scan (7Ch/10Ch)", "Mirror scanner."),
    ("beam_7r", "Beam 7r (7Ch/10Ch)", "Sharp beam fixture in the 7R style."),
    ("mac_vip", "Mac Vip (7Ch/10Ch)", "Spot moving head with its own gobo wheel on channel 8 of the extended mode; accepts custom gobos."),
    ("sharplus", "Sharplus (7Ch/10Ch)", "Narrow beam moving head."),
    ("moving500", "Moving 500 (7Ch/10Ch)", "Compact spot moving head."),
    ("robitspot", "Robit Spot (7Ch/10Ch)", "Spot moving head."),
    ("vervespot", "Verve Spot (7Ch/10Ch)", "LED spot moving head."),
    ("searchlight", "Searchlight (7Ch/10Ch)", "Large searchlight on a pan/tilt base, for sky beams."),
]:
    FIXTURES.append(F(id_, name, "Moving heads & beams", intro, [P_7CH, P_10CH_EXT], MH_NOTES))
for id_, name, intro in [
    ("vl6000", "VL 6000", "Very large wash moving head."),
    ("washlight", "Wash FX648", "LED wash moving head."),
    ("washled", "Wash Led", "Compact LED wash moving head."),
    ("miniwash", "Mini Wash", "Small LED wash moving head."),
    ("moving_bar", "Moving Bar", "Tilting LED bar with a wash texture."),
]:
    FIXTURES.append(F(id_, name, "Moving heads & beams", intro, [P_7CH_ONLY], MH_NOTES))

_FAMILY = "Moving heads & beams"
FIXTURES.append(F("atomictilt", "Atomic Tilt", "A strobe head on a tilting yoke, no pan.", [
    P("6-Channel RGB + Focus + Tilt", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE,
                                       ch("Tilt", "0 to 255", "Tilt of the yoke over its full travel.")]),
    P("7-Channel RGB + Focus + Strobe + Tilt", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE, STROBE,
                                                ch("Tilt", "0 to 255", "Tilt of the yoke over its full travel.")]),
]))
MMB_BEAM = lambda n: [ch(f"Beam {n} intensity", "0 to 255", f"Dimmer of beam {n} (1 is at one end of the bar, 7 at the other)."),
                      ch(f"Beam {n} tilt", "0 to 255", f"Tilt of beam {n}."),
                      ch(f"Beam {n} red", "0 to 255", f"Red of beam {n}."),
                      ch(f"Beam {n} green", "0 to 255", f"Green of beam {n}."),
                      ch(f"Beam {n} blue", "0 to 255", f"Blue of beam {n}.")]
FIXTURES.append(F("moving_mini_bar", "Moving Mini Bar", "Seven individually tiltable beams on one bar.", [
    P("5ch - Unite Beam", [INTENSITY, ch("Tilt", "0 to 255", "Tilt of all seven beams together."), RED, GREEN, BLUE],
      "All beams move and colour together."),
    P("35ch - Alone Beam", sum((MMB_BEAM(n) for n in range(1, 8)), []),
      "The five channels repeat for each beam. The room light takes the brightest beam's level and colour."),
]))
FIXTURES.append(F("dwt_panel", "Moving Panel Par64 (DWT)", "A panning panel of four white PAR sections plus a warm section.", [
    P("6-Channel Mode", [ch(f"Section {n}", "0 to 255", f"Level of white section {n}.") for n in (1, 2, 3, 4)] + [
        ch("Warm section", "0 to 255", "Level of the warm-white section, tinted amber."),
        ch("Pan", "0 to 255 = 0° to 360°", "Rotation of the panel over a full turn. Unlike the moving heads this range starts at 0°, not −180°.")])]))

# Wash / spot
_FAMILY = "Wash, spot & followspot"
FIXTURES += [
    F("source_four", "Source Four", "Fixed ellipsoidal profile spot with colour mixing.", [P_4CH],
      ["Aim it with the Position section of its screen or the Fixture Wrench."]),
    F("source_four_warm", "Source Four (Warm)", "Fixed profile spot with a warm tungsten colour.", [P_1CH]),
    F("followspot", "Followspot", "Spot on a stand, meant to be driven by the [Followspot Console](/guide/followspot).", [
        P("7-Channel Mode", [INTENSITY, RED, GREEN, BLUE, FOCUS_CONE,
                             ch("Pan", "0 to 255 = −90° to 90°", "Horizontal aim over a half turn, 128 straight ahead. Narrower than the moving heads, like a followspot on its stand."),
                             ch("Tilt", "0 to 255 = −45° to 45°", "Vertical aim, 128 level.")])],
      ["The console's operator mode aims this fixture with the mouse; presets store pan, tilt, intensity and focus."]),
    F("led_fountain", "Led Fountain", "A fan of LED beams rising from a base.", [
        P("3-Channel Mode", [RED, GREEN, BLUE], "No dimmer channel: the intensity is the brightest of the three colours.")]),
    F("invisiblelight", "Invisible Light (25 blocks)", "A light source with no visible body, lighting scenery from an impossible place over 25 blocks.", [P_4CH]),
    F("bigscroller", "Big Scroller", "Large wash with a colour scroller look.", [P_4CH]),
    F("horizontalscroller", "Horizontal Scroller", "Horizontal wash with a colour scroller look.", [P_4CH]),
    F("verticalscroller", "Vertical Scroller", "Vertical wash with a colour scroller look.", [P_4CH]),
    F("parscroller", "Par64 (Scroller)", "PAR 64 with an eight-gel colour scroller.", [
        P("2-Channel Mode", [INTENSITY, ch("Gel position", "0 to 255 across 8 gels",
                                          "Position of the scroll. 0 is the first gel, 255 the last; the eight gels are red, green, blue, yellow, magenta, cyan, orange and white, cross-faded continuously so intermediate values blend two neighbours like a real scroller mid-frame.")])]),
]

# PARs
def par_family(prefix, label, colours, intro):
    for c in colours:
        cid = f"{prefix}{c}" if prefix.endswith("_") or prefix == "" else f"{prefix}_{c}"
        FIXTURES.append(F(cid, f"{label} {c.capitalize() if c != 'lightblue' else 'Lightblue'}", "PARs & LED panels",
                          intro.format(colour=c.replace("lightblue", "light blue")), [P_1CH]))


par_family("par1000", "Par 1000", ["red", "blue", "green", "magenta", "amber", "orange", "purple", "lightblue", "white"],
           "Single PAR 1000 can with a fixed {colour} gel.")
FIXTURES.append(F("par1000", "Par 1000", "PARs & LED panels", "Single PAR 1000 can, plain white.", [P_1CH]))
par_family("par56", "Par56", ["red", "green", "blue", "orange", "magenta", "lightblue", "purple", "white", "warm", "yellow"],
           "PAR 56 can with a fixed {colour} gel and a soft wash texture.")
par_family("x8par", "x8 Par64", ["red", "green", "blue", "magenta", "lightblue", "yellow", "white", "purple", "warm", "orange"],
           "Bar of eight PAR 64 cans with a fixed {colour} gel, dimmed together.")
par_family("a2x2par64", "2x2 Par64", ["red", "green", "blue", "magenta", "lightblue", "purple", "orange", "yellow", "warm", "white"],
           "Block of four PAR 64 cans with a fixed {colour} gel, dimmed together.")
for id_, name, intro in [("a1x1par64", "2x2 Par64 (Block/Color Preset)", "Block of four PAR 64 cans."),
                         ("a2x8par64", "2x8 Par64 (Color Preset)", "Two rows of eight PAR 64 cans."),
                         ("a6x3par64_vertical", "6x3 Par64 Vertical (Color Preset)", "Vertical array of eighteen PAR 64 cans.")]:
    FIXTURES.append(F(id_, name, "PARs & LED panels", intro + " Nine one-channel colour presets or a four-channel RGB mode.", P_PRESETS,
                      ["Pick the colour as a personality when the desk only has one dimmer channel to spare, or iRGB for free mixing."]))
for id_, name, intro in [("par_led", "LED Par", "LED PAR with colour mixing."), ("led_panel_2", "LED Panel 2", "Flat LED panel."),
                         ("big_panel", "Big Panel 3x3", "Nine-cell LED panel, driven as one."), ("big_panel2", "Big Panel 3x2", "Six-cell LED panel, driven as one."),
                         ("rgb_bar", "RGB Bar", "Horizontal LED bar with a soft glow whose reach is `rgbBarBeamLength` in the config."),
                         ("vertical_bar", "Vertical RGB Bar", "Vertical LED bar."), ("mini_bar", "Mini RGB Bar", "Short LED bar."),
                         ("truss_3lights", "Truss 3x3 Lights", "Truss piece with nine built-in lights, driven as one.")]:
    FIXTURES.append(F(id_, name, "PARs & LED panels", intro, [P_4CH]))
FIXTURES.append(F("led_facade", "LED Facade", "A pixel-mapped LED wall. Paint the lit pixels in its screen, choose the resolution and smoothing, and drive every pixel from the desk.", [
    P("4-Channel Pixel (per lit pixel)", [ch("Pixel dimmer", "0 to 255", "Dimmer of this pixel."), ch("Pixel red", "0 to 255", "Red of this pixel."),
                                          ch("Pixel green", "0 to 255", "Green of this pixel."), ch("Pixel blue", "0 to 255", "Blue of this pixel.")],
      "Lit pixels are numbered row by row, left to right, top to bottom, four channels each, continuing into the next universe every 512 channels. The screen's footprint line shows pixels, channels and universes. Map it on the console as an LED matrix of 4-channel RGB pixels.")],
    ["Resolution 16, 32, 64, 128 or 256; smoothing Sharp, Soft or Very soft; brushes of 1, 2, 3 and 5 pixels.",
     "The maximum number of universes is `ledFacadeMaxUniverses` in the config (default 64).",
     "The facade emits one aggregated coloured light in front of the wall."]))

# Blinders / strobes
_FAMILY = "Blinders & strobes"
FIXTURES += [
    F("blinder", "Blinder 4x2", "Eight-lamp audience blinder with colour mixing.", [P_4CH_IRGB], ["Lights its face and throws light on the ground in front."]),
    F("blinder2x2", "2x2 Blinder", "Four-lamp blinder with colour mixing.", [P_4CH_IRGB]),
    F("blinder1x1", "Blinder 1x1", "Single-lamp blinder with colour mixing and a strobe channel.",
      [P("5-Channel iRGB + Strobe", [INTENSITY, RED, GREEN, BLUE, STROBE])]),
    F("blinder_warm", "Blinder (warm)", "Eight-lamp blinder, fixed warm tungsten.", [P_1CH]),
    F("blinder2x2warm", "2x2 Blinder (Warm)", "Four-lamp blinder, fixed warm tungsten.", [P_1CH]),
    F("strobe", "Strobe", "Stage strobe with four ways to patch it.", [
        P("4-Channel Legacy", [INTENSITY, RED, GREEN, BLUE], "Always open; the desk makes the flashes with the dimmer."),
        P("5-Channel Focus", [INTENSITY, RED, GREEN, BLUE, ch("Focus", "1 to 255", "Size of the light spot on the ground: about 3 blocks at 1, about 20 blocks at 255.")]),
        P("6-Channel Focus + Strobe", [INTENSITY, RED, GREEN, BLUE, ch("Focus", "1 to 255", "Size of the light spot on the ground, 3 to 20 blocks."), STROBE]),
        P("3-Channel Strobe RGB Only", [RED, GREEN, BLUE], "Intensity and focus are fixed and the shutter is always open: the fixture flashes continuously and you only choose the colour."),
    ], ["Flashes its face; it has no beam."]),
    F("white_strobe", "White Strobe (1 Channel)", "Simple white strobe.", [P_1CH]),
    F("atomic_strobe", "Atomic Strobe (34 Channels)", "Large LED strobe with eight RGB zones and a nine-segment white bar, on a floor plate with a tilting yoke.", [
        P("34-Channel Atomic", sum(([ch(f"Zone {z} red", "0 to 255", f"Red of RGB zone {z}."), ch(f"Zone {z} green", "0 to 255", f"Green of RGB zone {z}."),
                                     ch(f"Zone {z} blue", "0 to 255", f"Blue of RGB zone {z}.")] for z in range(1, 9)), [])
          + [ch(f"White bar segment {s}", "0 to 255", f"Level of white bar segment {s}.") for s in range(1, 10)]
          + [ch("Focus", "1 to 255", "Size of the light spot on the ground.")],
          "The room light takes the brightest emitter as level and an intensity-weighted mix of every lit emitter as colour, with the white bar weighted heavier.")]),
]

# Laser
_FAMILY = "Lasers & effects"
FIXTURES += [
    F("laser", "Laser", "Pattern laser with three colour groups, fourteen shapes and a persistence trail. Its screen carries the [emergency stop](/guide/lasers).",
      [P("19-Channel Mode", LASER_CH)],
      ["Beam length and pass-through blocks: `laserBeamLength`, `laserPassThroughBlocks` in the [config file](/guide/config-file).",
       "The emergency stop blocks the output whatever the DMX, is saved with the world and shown to every player."]),
    F("laser_mirror", "Laser Mirror", "Reflects laser beams; emits nothing on its own.",
      [P("5-Channel Mode", [INTENSITY, RED, GREEN, BLUE, FOCUS])]),
]

# Water
JET_NOTE = ["Height, thickness and, where available, cone angle or spread are set in the fixture screen (right-click), not on DMX."]
for id_, name, intro, extra in [
    ("water_jet", "Water Jet (25m)", "Fixed 25-metre vertical jet.", ""),
    ("water_jet_thin", "Water Jet 1 (Adjustable)", "Thin adjustable jet.", ""),
    ("water_jet_spread", "Water Jet 2 (Adjustable)", "Adjustable jet with a wider spray.", ""),
    ("water_jet_big", "Conifer Water Jet (Adjustable)", "Dense conifer-shaped jet.", ""),
    ("water_jet_central", "Central Water Jet (Adjustable)", "Tall central jet for the middle of a basin.", ""),
    ("water_jet_cone", "Water Jet Cone (Adjustable)", "Conical spray; the cone angle is adjustable.", ""),
    ("water_jet_bloom", "Bloom Jet (Adjustable)", "Flower-shaped bloom; the cone angle is adjustable.", ""),
    ("water_jet_fog", "Fog Cone Jet (Adjustable)", "Misting cone; the cone angle is adjustable.", ""),
    ("fan_water_jet", "Fan Water Jet (Adjustable)", "Flat fan of water.", ""),
    ("cake_water_jet", "Wedding Cake Water Jet (Adjustable)", "Tiered cake effect.", ""),
    ("vase_water_jet", "Vase Water Jet (Adjustable)", "Vase-shaped column.", ""),
    ("spinner", "Spinner (Water Jet Effect)", "Rotating spray.", ""),
    ("organpipes", "Organ Pipes (Water Jet Effect)", "Row of pipes of increasing height; the spread is adjustable.", ""),
    ("organpipes_inv", "Organ Pipes Inverted (Water Jet Effect)", "Row of pipes of decreasing height.", ""),
]:
    FIXTURES.append(F(id_, name, "Water jets", intro, [P_1CH_JET], JET_NOTE))
_FAMILY = "Water jets"
FIXTURES.append(F("moving_jet", "Moving Jet (Adjustable)", "Water jet on a pan/tilt head.", [
    P("3-Channel Mode", [INTENSITY_JET, PAN, ch("Tilt", "0 to 255 = −90° to 90°", "Vertical aim of the jet, 128 straight up.")])], JET_NOTE))
WALTZ = P("3-Channel Mode (labelled 2-Channel Mode)", [INTENSITY_JET, ch("Tilt", "0 to 255", "Base angle of the jets."),
                                                        ch("Swing speed", "0 to 255", "Speed of the swinging motion, 0 still.")],
          "The mode label in the screen still reads 2-Channel Mode; the footprint is three channels.")
FIXTURES.append(F("waltzes_water_jet", "9 Waltzes Water Jets (Adjustable)", "Nine swinging jets in a row.", [WALTZ]))
FIXTURES.append(F("waltz_curtain", "Waltz Curtain Water Jets (Adjustable)", "Curtain of swinging jets.", [WALTZ]))

# Pyro
_FAMILY = "Pyro & fireworks"
PYRO_NOTE = ["Carries a **safety arm**: disarmed in its screen, it reads DMX but never fires. See [Pyro & safety arm](/guide/pyro).",
             "Found in the **Theatrical: Pyro** creative tab."]
FIXTURES.append(F("firework_launcher", "Firework Launcher (all presets)",
                  "About 150 launcher blocks, one per effect (comets, peonies, willows, chrysanthemums, crossettes, mines, aerial strobes, daytime powder, special shells). They all share one personality; the block decides the effect.",
                  [P("3-Channel Firework", [FIRE, LAUNCH_TILT, LAUNCH_POWER])],
                  PYRO_NOTE + ["Registry ids are `theatricalextralights:firework_<effect>`; the full list is in [Pyro & fireworks](/fixtures/pyro).",
                               "Shots get a small random horizontal drift; the server caps concurrent rockets and launches per tick (config `maxConcurrentRockets`)."]))
FIXTURES.append(F("firework_rgb_launcher", "RGB Firework Launcher", "A launcher whose effect and colour are chosen from the desk.", [
    P("7-Channel RGB Firework", [FIRE, LAUNCH_TILT, LAUNCH_POWER,
                                 ch("Red", "0 to 255 (default 255)", "Red tint of the shell."), ch("Green", "0 to 255 (default 255)", "Green tint."),
                                 ch("Blue", "0 to 255 (default 255)", "Blue tint."),
                                 ch("Effect", "0 to 255", "Selects the burst pattern among the launcher presets, in the order of the preset list; 0 is the first.")])], PYRO_NOTE))
FIXTURES.append(F("pyro_fan", "Pyro Fan — 10 Comets", "Ten tubes firing gold comets in a vertical fan.", [
    P("3-Channel Pyro Fan (All Tubes)", [ch("Fire", "0 idle, rising edge fires, 2 to 255 continuous", "Fires all ten tubes; each rise from 0 fires one round, continuous values fire repeatedly."), LAUNCH_TILT, LAUNCH_POWER]),
    P("10-Channel Pyro Fan (Per Tube)", [ch(f"Tube {n}", "0 idle, rising edge fires", f"Fires tube {n} on each rise from 0. Tilt is fixed and power is full in this mode.") for n in range(1, 11)],
      "Rate 0.5 to 6 shots per second, tubes scheduled round-robin, up to 10 launches per tick.")],
    ["Found in the Theatrical: Pyro tab."]))
FIXTURES.append(F("flame_projector", "Flame Projector", "Vertical flame column.", [
    P("2-Channel Flame Projector", [ch("Flame length", "0 off to 255 full", "Height of the flame and the trigger: 0 is off."),
                                    ch("Head angle", "0 to 255", "Tilt of the flame head.")])], PYRO_NOTE))
FIXTURES.append(F("flame_thrower", "Flame Thrower", "Directed flame jet.", [
    P("2-Channel Flame Thrower", [ch("Flame", "0 off to 255 full", "Length of the jet and the trigger."), ch("Pan", "0 to 255", "Horizontal direction of the jet.")])], PYRO_NOTE))
FIXTURES.append(F("gerb_gold", "Gold Gerb (Ground Fountain)", "Stage gerb, a gold spark fountain.", [
    P("2-Channel Gerb", [INTENSITY, ch("Tilt", "0 to 255", "Angle of the fountain.")])], PYRO_NOTE))
FIXTURES.append(F("flow2jet", "Flow2Jet CO₂ Cannon", "CO₂ jet with a blast sound. Also exists as a handheld item.", [
    P("1-Channel Flow2Jet", [ch("Blast", "0 off to 255", "Fires the CO₂ blast while above 0.")])],
    ["No safety arm on this one: it fires whenever the channel rises."]))
FIXTURES.append(F("confetti_cannon", "Confetti Cannon", "High confetti burst. Also exists as a handheld item that fires where you look.", [
    P("1-Channel Confetti Cannon", [ch("Fire", "0 idle, rising edge fires", "Fires one burst on each rise from 0.")])], PYRO_NOTE))

FAMILY_ORDER = ["Gobo moving heads", "Moving heads & beams", "Wash, spot & followspot", "PARs & LED panels", "Blinders & strobes",
                "Lasers & effects", "Water jets", "Pyro & fireworks"]
FAMILY_PAGE = {"Gobo moving heads": "gobo-heads", "Moving heads & beams": "moving-heads", "Wash, spot & followspot": "wash-spot",
               "PARs & LED panels": "pars-panels", "Blinders & strobes": "blinders-strobes", "Lasers & effects": "lasers-effects",
               "Water jets": "water-jets", "Pyro & fireworks": "pyro"}
# fixtures declared without an explicit family above
for f in FIXTURES:
    if f["family"] not in FAMILY_ORDER:
        raise SystemExit(f"unknown family {f['family']} for {f['id']}")

# ── Rendering ───────────────────────────────────────────────────────────────

def render_fixture(f):
    lines = [f"# {f['name']}", "", f"`theatricalextralights:{f['id']}` · family: [{f['family']}](/fixtures/{FAMILY_PAGE[f['family']]})", "", f["intro"], ""]
    if f["notes"]:
        for n in f["notes"]:
            lines.append(f"- {n}")
        lines.append("")
    multi = len(f["personalities"]) > 1
    if multi:
        lines += ["## Personalities", "", "| Mode | Channels |", "|---|---|"]
        for p in f["personalities"]:
            lines.append(f"| {p['name']} | {len(p['channels'])} |")
        lines += ["", "Select the mode in the fixture config screen; the footprint changes immediately.", ""]
    for p in f["personalities"]:
        lines += [f"## {p['name']} ({len(p['channels'])} ch)", ""]
        if p.get("note"):
            lines += [p["note"], ""]
        lines += ["| Ch | Function | Values |", "|---|---|---|"]
        for i, c in enumerate(p["channels"], 1):
            lines.append(f"| {i} | {c['name']} | {c['values']} |")
        lines += ["", "### Channel by channel", ""]
        for i, c in enumerate(p["channels"], 1):
            lines.append(f"**{i} · {c['name']}** — {c['text']}")
            lines.append("")
    return "\n".join(lines)


def render_family(fam, items):
    slug = FAMILY_PAGE[fam]
    lines = [f"# {fam}", "", f"One page per fixture, each channel explained. {len(items)} fixtures.", "", "| Fixture | Modes |", "|---|---|"]
    for f in items:
        modes = ", ".join(f"{p['name']} ({len(p['channels'])})" for p in f["personalities"])
        if len(f["personalities"]) > 4:
            modes = f"{len(f['personalities'])} modes"
        lines.append(f"| [{f['name']}](/fixtures/{f['id']}) | {modes} |")
    lines.append("")
    return "\n".join(lines), slug


def main():
    os.makedirs(OUT, exist_ok=True)
    sidebar = []
    for fam in FAMILY_ORDER:
        items = [f for f in FIXTURES if f["family"] == fam]
        for f in items:
            with open(os.path.join(OUT, f"{f['id']}.md"), "w", encoding="utf-8") as fh:
                fh.write(render_fixture(f))
        body, slug = render_family(fam, items)
        if fam == "Gobo moving heads":
            # keep the hand-written channel page, append the per-fixture list
            path = os.path.join(OUT, "gobo-heads.md")
            text = open(path, encoding="utf-8").read()
            marker = "\n## Fixture pages\n"
            text = text.split(marker)[0].rstrip() + marker + "\n" + "\n".join(f"- [{f['name']}](/fixtures/{f['id']})" for f in items) + "\n"
            open(path, "w", encoding="utf-8").write(text)
        elif fam == "Pyro & fireworks":
            path = os.path.join(OUT, "pyro.md")
            text = open(path, encoding="utf-8").read()
            marker = "\n## Fixture pages\n"
            text = text.split(marker)[0].rstrip() + marker + "\n" + "\n".join(f"- [{f['name']}](/fixtures/{f['id']})" for f in items) + "\n"
            open(path, "w", encoding="utf-8").write(text)
        else:
            with open(os.path.join(OUT, f"{slug}.md"), "w", encoding="utf-8") as fh:
                fh.write(body)
        sidebar.append({"text": fam, "collapsed": True,
                        "items": [{"text": "Overview", "link": f"/fixtures/{slug}"}] + [{"text": f["name"], "link": f"/fixtures/{f['id']}"} for f in items]})
    sidebar.append({"text": "Consoles, rig & misc", "items": [{"text": "Overview", "link": "/fixtures/consoles-misc"}]})
    with open(SIDEBAR, "w", encoding="utf-8") as fh:
        json.dump(sidebar, fh, indent=2, ensure_ascii=False)
    print(f"{len(FIXTURES)} fixture pages, sidebar written")


if __name__ == "__main__":
    main()
