# Followspot

`theatricalextralights:followspot` · family: [Wash, spot & followspot](/fixtures/wash-spot)

Spot on a stand, meant to be driven by the [Followspot Console](/guide/followspot).

![Followspot beam on a performer](/images/followspot-stage.png)

- The console's operator mode aims this fixture with the mouse; presets store pan, tilt, intensity and focus.

## 7-Channel Mode (7 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Pan | 0 to 255 = −90° to 90° |
| 7 | Tilt | 0 to 255 = −45° to 45° |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Pan** — Horizontal aim over a half turn, 128 straight ahead. Narrower than the moving heads, like a followspot on its stand.

**7 · Tilt** — Vertical aim, 128 level.
