# Moving Beam (7Ch/10Ch)

`theatricalextralights:moving_beam` · family: [Moving heads & beams](/fixtures/moving-heads)

Tight beam moving head.

- Hangs from a truss or stands on the floor; hung upside down, pan and tilt are mirrored automatically.
- Right-click opens the config screen; pan and tilt are DMX-driven, so there are no position sliders.

## Personalities

| Mode | Channels |
|---|---|
| 7ch - Standard | 7 |
| 10ch - Extended | 10 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 7ch - Standard (7 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Pan | 0 to 255 = −180° to 180° |
| 7 | Tilt | 0 to 255 = −225° to 45° |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**7 · Tilt** — Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.

## 10ch - Extended (10 ch)

Channels 1 to 7 as in the standard mode, plus the prism group.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Pan | 0 to 255 = −180° to 180° |
| 7 | Tilt | 0 to 255 = −225° to 45° |
| 8 | Prism / gobo select | 0 to 255 |
| 9 | Prism zoom | 0 to 255 |
| 10 | Prism rotation | 0 stop, 1 to 255 speed |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**7 · Tilt** — Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.

**8 · Prism / gobo select** — Selects the prism or gobo slot of the extended mode.

**9 · Prism zoom** — Size of the prism effect.

**10 · Prism rotation** — Rotation speed of the prism or gobo.
