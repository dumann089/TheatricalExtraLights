# Moving Jet (Adjustable)

`theatricalextralights:moving_jet` · family: [Water jets](/fixtures/water-jets)

Water jet on a pan/tilt head.

- Height, thickness and, where available, cone angle or spread are set in the fixture screen (right-click), not on DMX.

## 3-Channel Mode (3 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Jet level | 0 off to 255 full |
| 2 | Pan | 0 to 255 = −180° to 180° |
| 3 | Tilt | 0 to 255 = −90° to 90° |

### Channel by channel

**1 · Jet level** — Turns the water on and sets its level. 0 is off; higher values raise the flow, which makes the jet taller and denser up to the height set in the fixture screen.

**2 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**3 · Tilt** — Vertical aim of the jet, 128 straight up.
