# Laser Mirror

`theatricalextralights:laser_mirror` · family: [Lasers & effects](/fixtures/lasers-effects)

Reflects laser beams; emits nothing on its own.

## 5-Channel Mode (5 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.
