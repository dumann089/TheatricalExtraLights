# Strobe

`theatricalextralights:strobe` · family: [Blinders & strobes](/fixtures/blinders-strobes)

Stage strobe with four ways to patch it.

- Flashes its face; it has no beam.

## Personalities

| Mode | Channels |
|---|---|
| 4-Channel Legacy | 4 |
| 5-Channel Focus | 5 |
| 6-Channel Focus + Strobe | 6 |
| 3-Channel Strobe RGB Only | 3 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 4-Channel Legacy (4 ch)

Always open; the desk makes the flashes with the dimmer.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

## 5-Channel Focus (5 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 1 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Size of the light spot on the ground: about 3 blocks at 1, about 20 blocks at 255.

## 6-Channel Focus + Strobe (6 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 1 to 255 |
| 6 | Strobe | 0 closed, 1 to 254 strobe, 255 open |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Size of the light spot on the ground, 3 to 20 blocks.

**6 · Strobe** — Mechanical shutter. 0 is closed (dark regardless of the dimmer), 1 to 254 strobes from slow to fast, 255 is open. Use 255 for normal operation and drop to 0 for a hard blackout that keeps the dimmer level.

## 3-Channel Strobe RGB Only (3 ch)

Intensity and focus are fixed and the shutter is always open: the fixture flashes continuously and you only choose the colour.

| Ch | Function | Values |
|---|---|---|
| 1 | Red | 0 to 255 |
| 2 | Green | 0 to 255 |
| 3 | Blue | 0 to 255 |

### Channel by channel

**1 · Red** — Red component of the additive colour mix.

**2 · Green** — Green component of the additive colour mix.

**3 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.
