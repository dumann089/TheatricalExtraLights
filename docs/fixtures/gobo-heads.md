# Gobo moving heads

Eight fixtures share this engine and these personalities:

| Fixture | Registry id |
|---|---|
| Spot Xtreme (Gobos) | `theatricalextralights:spot_xtreme_gobo` |
| VL6C (Gobos) | `theatricalextralights:vl6c_gobo` |
| Iris 700 Spot (Gobos) | `theatricalextralights:iris_700_gobo` |
| Pro Spot (Gobos) | `theatricalextralights:pro_spot_gobo` |
| Mini Scan (Gobos) | `theatricalextralights:mini_scan_gobos` |
| Mini Spot (Gobos) | `theatricalextralights:mini_spot_gobos` |
| Moving Scan Beams | `theatricalextralights:moving_scan_beams` |
| Moving VL2C Beams | `theatricalextralights:moving_vl2c_beams` |

Usage notes and behaviour are in the guide: [Gobo heads & personalities](/guide/gobo-heads). grandMA2 files: [grandMA2 fixture files](/guide/grandma2).

## 10-Channel Mode

| Ch | Function | Values |
|---|---|---|
| 1 | Dimmer | 0 to 255 |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 sharp to 255 soft |
| 6 | Pan | 0 to 255 = −180° to 180° |
| 7 | Tilt | 0 to 255 = −225° to 45° |
| 8 | Gobo wheel | Equal ranges, one per slot (10 slots on most wheels); the first range is open |
| 9 | Zoom | 0 narrow (1°) to 255 wide (19°) |
| 10 | Gobo rotation | 0 stop, 1 to 255 spin speed |

## 19ch - Framing Shutters

Channels 1 to 10 as in 10-Channel Mode, then:

| Ch | Function | Values |
|---|---|---|
| 11 | Blade 1 A | 0 out to 255 in |
| 12 | Blade 1 B | 0 out to 255 in |
| 13 | Blade 2 A | |
| 14 | Blade 2 B | |
| 15 | Blade 3 A | |
| 16 | Blade 3 B | |
| 17 | Blade 4 A | |
| 18 | Blade 4 B | |
| 19 | Frame rotation | 0 = −55°, 128 neutral, 255 = +55° (127 and 128 both neutral) |

Blade 1 is top, 2 right, 3 bottom, 4 left, seen from the fixture along the beam. Corner A and corner B follow the grandMA shaper convention.

## 29ch - Profile 16bit

| Ch | Function | Values |
|---|---|---|
| 1 | Shutter | 0 closed, 1 to 254 strobe 0.5 to 10 Hz, 255 open |
| 2 | Dimmer coarse | 16 bit with channel 3 |
| 3 | Dimmer fine | |
| 4 | Red | 0 to 255 |
| 5 | Green | 0 to 255 |
| 6 | Blue | 0 to 255 |
| 7 | Gobo wheel | Equal ranges, one per slot (10 slots on most wheels); the first range is open |
| 8 | Gobo rotation | 0 stop, 1 to 255 spin speed |
| 9 | Prism | 0 to 127 out, 128 to 170 three facets, 171 to 213 six, 214 to 255 nine |
| 10 | Prism rotation | 0 to 127 indexed 0 to 360°, 128 to 191 clockwise slow to fast, 192 to 255 counter-clockwise slow to fast |
| 11 | Animation wheel | 0 to 15 out, 16 to 75 flames, 76 to 135 water, 136 to 195 clouds, 196 to 255 breakup |
| 12 | Animation rotation | 0 to 127 fixed orientation 0 to 360°, 128 to 191 scroll slow to fast, 192 to 255 reverse scroll |
| 13 | Frost | 0 none to 255 full |
| 14 | Zoom | 0 narrow (1°) to 255 wide (19°) |
| 15 | Focus | 0 sharp to 255 soft |
| 16 | Pan coarse | 16 bit, −180° to 180° |
| 17 | Pan fine | |
| 18 | Tilt coarse | 16 bit, −225° to 45° |
| 19 | Tilt fine | |
| 20 | Pan/tilt speed | 0 to 2 tracking (instant), 3 fast (720°/s) to 255 slow (25°/s) |
| 21 | Blade 1 A | 0 out to 255 in |
| 22 | Blade 1 B | |
| 23 | Blade 2 A | |
| 24 | Blade 2 B | |
| 25 | Blade 3 A | |
| 26 | Blade 3 B | |
| 27 | Blade 4 A | |
| 28 | Blade 4 B | |
| 29 | Frame rotation | 0 = −55°, 128 neutral, 255 = +55° |

### Defaults on a fresh fixture

Shutter open, dimmer 0, RGB 255/255/255, prism out, animation out, frost 0, pan centred, tilt 0°, speed tracking, blades out.

### Rendering notes

- The dimmer output is the 16-bit value scaled to 8 bit, then gated by the shutter.
- Prism copies are drawn at 75 % (3 facets), 60 % (6) or 50 % (9) of the intensity each, so total light stays plausible.
- Frost is added to the focus of the projected spot; it does not widen the volumetric beam.
- With the shutter closed the coloured dynamic light turns off as well.
## Fixture pages

- [Xtreme Spot (Gobos)](/fixtures/spot_xtreme_gobo)
- [VL6C Spot (Gobos)](/fixtures/vl6c_gobo)
- [Iris 700 Spot (Gobos)](/fixtures/iris_700_gobo)
- [Pro Spot LX (Gobos)](/fixtures/pro_spot_gobo)
- [Mini Scan (Gobos)](/fixtures/mini_scan_gobo)
- [Mini Spot (Gobos)](/fixtures/mini_spot_gobo)
- [Moving Scan (Gobos)](/fixtures/moving_scan_beams)
- [Moving VL2 (Gobos)](/fixtures/moving_vl2c_beams)
