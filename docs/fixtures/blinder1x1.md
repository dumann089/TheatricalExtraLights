# Blinder 1x1

`theatricalextralights:blinder1x1` · family: [Blinders & strobes](/fixtures/blinders-strobes)

Single-lamp blinder with colour mixing and a strobe channel.

## 5-Channel iRGB + Strobe (5 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Strobe | 0 closed, 1 to 254 strobe, 255 open |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Strobe** — Mechanical shutter. 0 is closed (dark regardless of the dimmer), 1 to 254 strobes from slow to fast, 255 is open. Use 255 for normal operation and drop to 0 for a hard blackout that keeps the dimmer level.
