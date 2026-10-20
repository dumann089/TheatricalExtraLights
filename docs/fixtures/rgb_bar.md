# RGB Bar

`theatricalextralights:rgb_bar` · family: [PARs & LED panels](/fixtures/pars-panels)

Horizontal LED bar with nine pixels and a soft glow whose reach is `rgbBarBeamLength` in the config.

## Personalities

| Mode | Channels |
|---|---|
| 4-Channel Mode | 4 |
| 36-Channel Pixel Mode (9x Dim/RGB) | 36 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 4-Channel Mode (4 ch)

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

## 36-Channel Pixel Mode (9x Dim/RGB) (36 ch)

Pixels are numbered 1 to 9 from left to right, seen from the front of the bar. Each pixel lights its cell and its LED dot; a single lit pixel throws one thin sheet of light into the haze, and neighbouring lit pixels of the same colour merge into one sheet that widens with them. There is no master dimmer: to fade the whole bar, select the fixture and use its dimmer. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode, with the pixels as sub-fixtures 1.1 to 1.9.

| Ch | Function | Values |
|---|---|---|
| 1 | Pixel 1 dimmer | 0 off to 255 full |
| 2 | Pixel 1 red | 0 to 255 |
| 3 | Pixel 1 green | 0 to 255 |
| 4 | Pixel 1 blue | 0 to 255 |
| 5 | Pixel 2 dimmer | 0 off to 255 full |
| 6 | Pixel 2 red | 0 to 255 |
| 7 | Pixel 2 green | 0 to 255 |
| 8 | Pixel 2 blue | 0 to 255 |
| 9 | Pixel 3 dimmer | 0 off to 255 full |
| 10 | Pixel 3 red | 0 to 255 |
| 11 | Pixel 3 green | 0 to 255 |
| 12 | Pixel 3 blue | 0 to 255 |
| 13 | Pixel 4 dimmer | 0 off to 255 full |
| 14 | Pixel 4 red | 0 to 255 |
| 15 | Pixel 4 green | 0 to 255 |
| 16 | Pixel 4 blue | 0 to 255 |
| 17 | Pixel 5 dimmer | 0 off to 255 full |
| 18 | Pixel 5 red | 0 to 255 |
| 19 | Pixel 5 green | 0 to 255 |
| 20 | Pixel 5 blue | 0 to 255 |
| 21 | Pixel 6 dimmer | 0 off to 255 full |
| 22 | Pixel 6 red | 0 to 255 |
| 23 | Pixel 6 green | 0 to 255 |
| 24 | Pixel 6 blue | 0 to 255 |
| 25 | Pixel 7 dimmer | 0 off to 255 full |
| 26 | Pixel 7 red | 0 to 255 |
| 27 | Pixel 7 green | 0 to 255 |
| 28 | Pixel 7 blue | 0 to 255 |
| 29 | Pixel 8 dimmer | 0 off to 255 full |
| 30 | Pixel 8 red | 0 to 255 |
| 31 | Pixel 8 green | 0 to 255 |
| 32 | Pixel 8 blue | 0 to 255 |
| 33 | Pixel 9 dimmer | 0 off to 255 full |
| 34 | Pixel 9 red | 0 to 255 |
| 35 | Pixel 9 green | 0 to 255 |
| 36 | Pixel 9 blue | 0 to 255 |

### Channel by channel

**1 · Pixel 1 dimmer** — Dimmer of pixel 1. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**2 · Pixel 1 red** — Red of pixel 1.

**3 · Pixel 1 green** — Green of pixel 1.

**4 · Pixel 1 blue** — Blue of pixel 1.

**5 · Pixel 2 dimmer** — Dimmer of pixel 2. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**6 · Pixel 2 red** — Red of pixel 2.

**7 · Pixel 2 green** — Green of pixel 2.

**8 · Pixel 2 blue** — Blue of pixel 2.

**9 · Pixel 3 dimmer** — Dimmer of pixel 3. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**10 · Pixel 3 red** — Red of pixel 3.

**11 · Pixel 3 green** — Green of pixel 3.

**12 · Pixel 3 blue** — Blue of pixel 3.

**13 · Pixel 4 dimmer** — Dimmer of pixel 4. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**14 · Pixel 4 red** — Red of pixel 4.

**15 · Pixel 4 green** — Green of pixel 4.

**16 · Pixel 4 blue** — Blue of pixel 4.

**17 · Pixel 5 dimmer** — Dimmer of pixel 5. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**18 · Pixel 5 red** — Red of pixel 5.

**19 · Pixel 5 green** — Green of pixel 5.

**20 · Pixel 5 blue** — Blue of pixel 5.

**21 · Pixel 6 dimmer** — Dimmer of pixel 6. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**22 · Pixel 6 red** — Red of pixel 6.

**23 · Pixel 6 green** — Green of pixel 6.

**24 · Pixel 6 blue** — Blue of pixel 6.

**25 · Pixel 7 dimmer** — Dimmer of pixel 7. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**26 · Pixel 7 red** — Red of pixel 7.

**27 · Pixel 7 green** — Green of pixel 7.

**28 · Pixel 7 blue** — Blue of pixel 7.

**29 · Pixel 8 dimmer** — Dimmer of pixel 8. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**30 · Pixel 8 red** — Red of pixel 8.

**31 · Pixel 8 green** — Green of pixel 8.

**32 · Pixel 8 blue** — Blue of pixel 8.

**33 · Pixel 9 dimmer** — Dimmer of pixel 9. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**34 · Pixel 9 red** — Red of pixel 9.

**35 · Pixel 9 green** — Green of pixel 9.

**36 · Pixel 9 blue** — Blue of pixel 9.
