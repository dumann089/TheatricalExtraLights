# Laser

`theatricalextralights:laser` · family: [Lasers & effects](/fixtures/lasers-effects)

Pattern laser with three colour groups, fourteen shapes and a persistence trail. Its screen carries the [emergency stop](/guide/lasers).

- Beam length and pass-through blocks: `laserBeamLength`, `laserPassThroughBlocks` in the [config file](/guide/config-file).
- The emergency stop blocks the output whatever the DMX, is saved with the world and shown to every player.

## 19-Channel Mode (19 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Colour 1 Red | 0 to 255 |
| 3 | Colour 1 Green | 0 to 255 |
| 4 | Colour 1 Blue | 0 to 255 |
| 5 | Colour 2 Red | 0 to 255 |
| 6 | Colour 2 Green | 0 to 255 |
| 7 | Colour 2 Blue | 0 to 255 |
| 8 | Colour 3 Red | 0 to 255 |
| 9 | Colour 3 Green | 0 to 255 |
| 10 | Colour 3 Blue | 0 to 255 |
| 11 | Pattern | 14 ranges of 18 |
| 12 | Size | 0 to 255 |
| 13 | Amplitude | 0 to 255 |
| 14 | Speed | 0 to 255 |
| 15 | Rotation | 0 to 255 = 0° to 360° |
| 16 | Pan | 0 to 255 = −80° to 80° |
| 17 | Tilt | 0 to 255 = 45° to −45° |
| 18 | Focus | 0 to 255 |
| 19 | Persistence | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Colour 1 Red** — Red of the first colour group. The pattern is drawn with a gradient from colour 1 through colour 2 to colour 3.

**3 · Colour 1 Green** — Green of the first colour group.

**4 · Colour 1 Blue** — Blue of the first colour group.

**5 · Colour 2 Red** — Red of the second colour group. A group left entirely at 0 falls back to the previous group, so a single colour only needs group 1.

**6 · Colour 2 Green** — Green of the second colour group.

**7 · Colour 2 Blue** — Blue of the second colour group.

**8 · Colour 3 Red** — Red of the third colour group.

**9 · Colour 3 Green** — Green of the third colour group.

**10 · Colour 3 Blue** — Blue of the third colour group.

**11 · Pattern** — Selects the shape: 0 to 17 single beam, 18 to 35 line, 36 to 53 circle, 54 to 71 square, 72 to 89 wave, 90 to 107 tunnel, 108 to 125 star, 126 to 143 cross, 144 to 161 triangle, 162 to 179 spiral, 180 to 197 parallel lines, 198 to 215 double circle, 216 to 233 burst, 234 to 255 scatter.

**12 · Size** — Overall size of the pattern, 0 to 100 %.

**13 · Amplitude** — Depth of the pattern's modulation (wave height, tunnel depth, star points and so on).

**14 · Speed** — Animation speed. Below about 5 % (value 13) the pattern is frozen; above, it animates faster with the value.

**15 · Rotation** — Static rotation of the whole pattern.

**16 · Pan** — Horizontal aim of the projector, 128 straight ahead.

**17 · Tilt** — Vertical aim, centred on 127. Higher values point down.

**18 · Focus** — Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.

**19 · Persistence** — Length of the trail left by moving beams on the client. 0 is a clean beam, higher values leave a longer afterglow like a slow camera shutter.
