# grandMA2 fixture files

Fixture files for the gobo moving heads are maintained alongside the mod and match it channel for channel. They follow the conventions of the manufacturer files shipped with MA (percent channel-function ranges, hexadecimal colours for the RGB attributes, physical values in degrees and hertz), so the colour picker, the shaper editor and the effect engine work out of the box.

## Files

| File | Mode in the mod | Channels |
|---|---|---|
| `nailec@theatrical_gobo@10ch.xml` | 10-Channel Mode | 10 |
| `nailec@theatrical_gobo@19ch_framing_shutters.xml` | 19ch - Framing Shutters | 19 |
| `nailec@theatrical_gobo@29ch_profile_16bit.xml` | 29ch - Profile 16bit | 29 |

All three are one fixture type, **Nailec / THEATRICAL GOBO**, with three modes. They apply to the eight gobo heads (Spot Xtreme, VL6C, Iris 700, Pro Spot, Mini Scan, Mini Spot, Moving Scan Beams, Moving VL2C Beams).

### LED bars

| File | Fixture type | Mode in the mod | Channels |
|---|---|---|---|
| `nailec@theatrical_rgb_bar@4ch.xml` | THEATRICAL RGB BAR | 4-Channel Mode | 4 |
| `nailec@theatrical_rgb_bar@36ch_pixel.xml` | THEATRICAL RGB BAR | 36-Channel Pixel Mode (9x Dim/RGB) | 36 |
| `nailec@theatrical_vertical_rgb_bar@4ch.xml` | THEATRICAL VERTICAL RGB BAR | 4-Channel Mode | 4 |
| `nailec@theatrical_vertical_rgb_bar@36ch_pixel.xml` | THEATRICAL VERTICAL RGB BAR | 36-Channel Pixel Mode (9x Dim/RGB) | 36 |
| `nailec@theatrical_moving_bar@7ch.xml` | THEATRICAL MOVING BAR | 7-Channel Mode | 7 |
| `nailec@theatrical_moving_bar@34ch_pixel.xml` | THEATRICAL MOVING BAR | 34-Channel Pixel Mode (Pan, Tilt + 8x Dim/RGB) | 34 |
| `nailec@theatrical_dense_rgb_bar@4ch.xml` | THEATRICAL DENSE RGB BAR | 4-Channel Mode | 4 |
| `nailec@theatrical_dense_rgb_bar@184ch_pixel.xml` | THEATRICAL DENSE RGB BAR | 184-Channel Pixel Mode (46x Dim/RGB) | 184 |
| `nailec@theatrical_dense_vertical_rgb_bar@4ch.xml` | THEATRICAL DENSE VERTICAL RGB BAR | 4-Channel Mode | 4 |
| `nailec@theatrical_dense_vertical_rgb_bar@184ch_pixel.xml` | THEATRICAL DENSE VERTICAL RGB BAR | 184-Channel Pixel Mode (46x Dim/RGB) | 184 |
| `nailec@theatrical_dense_moving_bar@7ch.xml` | THEATRICAL DENSE MOVING BAR | 7-Channel Mode | 7 |
| `nailec@theatrical_dense_moving_bar@98ch_pixel.xml` | THEATRICAL DENSE MOVING BAR | 98-Channel Pixel Mode (Pan, Tilt + 24x Dim/RGB) | 98 |

All under manufacturer **Nailec**. The pixel modes are multi-instance types: the pixels appear as sub-fixtures **1.1 to 1.9** (1.1 to 1.8 on the Moving Bar, 1.1 to 1.46 on the two dense fixed bars, 1.1 to 1.24 on the Dense Moving RGB Bar), each with its own dimmer and RGB, so dimmer chases, pixel effects, the colour picker and layouts all work per pixel. On the RGB Bar and Moving Bar the pixels run left to right seen from the front; on the Vertical RGB Bar, bottom to top. Channels run Dimmer/R/G/B per pixel: pixel 1 on 1 to 4, pixel 2 on 5 to 8 and so on. On the Moving Bar the main fixture carries pan and tilt on channels 1 and 2 and the pixels start at channel 3; on the two fixed bars the main fixture has no channels of its own. There is no master dimmer: to fade the whole bar, select the fixture and use its dimmer, which addresses all pixels at once.

### LED panel

| File | Fixture type | Mode in the mod | Channels |
|---|---|---|---|
| `nailec@theatrical_led_panel_2@4ch.xml` | THEATRICAL LED PANEL 2 | 4-Channel Mode | 4 |
| `nailec@theatrical_led_panel_2@64ch_pixel.xml` | THEATRICAL LED PANEL 2 | 64-Channel Pixel Mode (4x4 Dim/RGB) | 64 |
| `nailec@theatrical_led_panel_2@256ch_pixel.xml` | THEATRICAL LED PANEL 2 | 256-Channel Pixel Mode (8x8 Dim/RGB) | 256 |

In the pixel modes the pixels are sub-fixtures **1.1 to 1.16** (4x4) or **1.1 to 1.64** (8x8), named by row and column (Px R1 C1 is top left seen from the front), each with Dim, Red, Green, Blue.

### Atomic strobes

| File | Fixture type | Mode in the mod | Channels |
|---|---|---|---|
| `nailec@theatrical_atomic_strobe@1ch.xml` | THEATRICAL ATOMIC STROBE | 1-Channel Strobe | 1 |
| `nailec@theatrical_atomic_strobe@4ch.xml` | THEATRICAL ATOMIC STROBE | 4-Channel Atomic | 4 |
| `nailec@theatrical_atomic_strobe@8ch_aura.xml` | THEATRICAL ATOMIC STROBE | 8-Channel Atomic + Aura | 8 |
| `nailec@theatrical_atomic_strobe@10ch_compressed.xml` | THEATRICAL ATOMIC STROBE | 10-Channel Compressed (Bar + Plate) | 10 |
| `nailec@theatrical_atomic_strobe@40ch_pixel.xml` | THEATRICAL ATOMIC STROBE | 40-Channel Pixel (Bar + Plate) | 40 |
| `nailec@theatrical_atomic_strobe@400ch_pixelmap.xml` | THEATRICAL ATOMIC STROBE | 400-Channel Pixel Map (Bar + 96 Pixels) | 400 |
| `nailec@theatrical_atomic_tilt@6ch_atomic.xml` | THEATRICAL ATOMIC TILT | 6-Channel Atomic + Focus + Tilt | 6 |
| `nailec@theatrical_atomic_tilt@9ch_atomic_rgb.xml` | THEATRICAL ATOMIC TILT | 9-Channel Atomic RGB + Focus + Tilt | 9 |

The Atomic layouts follow the Martin Atomic 3000 LED profile of the MA2 library: **DIM**, **STROBEDURATION** (12 to 650 ms), **SHUTTER** as the flash rate (0 stops the strobe, 1 to 255 runs 0.5 to 25 Hz) and **STROBEMODE** for the effects, with named ranges for strobe, blinder, ramp up, ramp down, ramp up/down, random, lightning, spikes and sparkle. The Aura (8ch) and the Plate (10ch, 40ch) are sub-fixture **1.1**; on the 40ch pixel mode the nine bar segments are **1.1 to 1.9**, the plate **1.10** and the eight RGB zones **1.11 to 1.18** (zones 1 to 4 on the top plate, 5 to 8 on the bottom, left to right). On the 400ch pixel map the 96 pixels are **1.11 to 1.106**, named by plate, row and column (T1-1 is the top plate, top row, left pixel; B4-12 the bottom plate, bottom row, right pixel), each with Dim, Red, Green, Blue, so pixel layouts and the effect engine drive the whole face. On a plate the rate channel at 0 leaves it on continuously.

Download them from the [`tools/grandma2`](https://github.com/dumann089/TheatricalExtraLights/tree/ver/1.20.1/tools/grandma2) folder of the repository.

## Importing

1. Copy the XML files to `gma2/library/` on a USB stick.
2. On the desk: **Setup → Patch & Fixture Schedule → Fixture Types → Import**, select the USB drive, pick the file, then **Import**.
3. Patch the fixtures at the same universe and address you entered in the mod.

::: warning Re-importing after an update
MA never replaces a fixture type that is already in the show: importing a file with the same name and mode keeps the old version. After a mod update, **delete the type** in Fixture Types (unpatch its fixtures first), then import the new file. The **Diagnostic** button in the Fixture Types view should then report nothing except, at most, a warning about the zoom angle, which is harmless.
:::

## Attribute mapping (29ch)

| Ch | Attribute | Feature | Notes |
|---|---|---|---|
| 1 | SHUTTER | Shutter | Closed / Strobe 0.5 to 10 Hz / Open; default open |
| 2 to 3 | DIM (16 bit) | Dimmer | |
| 4 to 6 | COLORRGB1 to 3 | Color RGB | Default 255, colour picker enabled |
| 7 | GOBO1 | Gobo1 | 16 snap slots |
| 8 | GOBO1_POS (sub GOBO1_ROT) | Gobo1 | Rotate |
| 9 | PRISMA1 | Beam | Open / 3 / 6 / 9 facets |
| 10 | PRISMA1_POS / PRISMA1_ROT | Beam | Index 0 to 360° / Rotate CW / CCW |
| 11 | ANIMATIONWHEEL1 | Animation | Open / Flames / Water / Clouds / Breakup |
| 12 | ANIMATIONWHEEL1_POS / _ROT | Animation | Index / Scroll / Scroll reverse |
| 13 | FROST | Beam | |
| 14 | ZOOM | Focus | 1° to 19° |
| 15 | FOCUS | Focus | |
| 16 to 17 | PAN (16 bit) | Position | −180° to 180° |
| 18 to 19 | TILT (16 bit) | Position | −225° to 45° |
| 20 | PTSPEED | Control | Tracking / Speed |
| 21 to 28 | BLADE1A, BLADE1B … BLADE4A, BLADE4B | Shapers | |
| 29 | SHAPER ROT | Shapers | −55° to 55° |

## Other consoles

Any desk that can build a generic fixture works: use the channel tables from the [fixture reference](/fixtures/gobo-heads). If you export a fixture for another platform (Eos, Chamsys, QLC+, GDTF), consider contributing it to the repository.

## Generating the files

The XML files are generated by small Python scripts from the channel layout: `tools/grandma2/gen_ma2_gobo.py` for the gobo heads and `tools/grandma2/gen_ma2_rgbbar.py` for the six LED bars `tools/grandma2/gen_ma2_ledpanel.py` for the LED Panel 2 and `tools/grandma2/gen_ma2_atomic.py` for the Atomic strobes. Run any of them with an output folder as argument to regenerate its files after a change.
