# Moving Bar

`theatricalextralights:moving_bar` · family: [Moving heads & beams](/fixtures/moving-heads)

Tilting LED bar with eight pixels and a wash texture.

- Hangs from a truss or stands on the floor; hung upside down, pan and tilt are mirrored automatically.
- Right-click opens the config screen; pan and tilt are DMX-driven, so there are no position sliders.

## Personalities

| Mode | Channels |
|---|---|
| 7-Channel Mode | 7 |
| 34-Channel Pixel Mode (Pan, Tilt + 8x Dim/RGB) | 34 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 7-Channel Mode (7 ch)

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

## 34-Channel Pixel Mode (Pan, Tilt + 8x Dim/RGB) (34 ch)

Pixels are numbered 1 to 8 from left to right, seen from the front of the head. Each pixel lights its cell and its LED dot; a single lit pixel throws one thin sheet of light into the haze, and neighbouring lit pixels of the same colour merge into one sheet that widens with them. There is no master dimmer: to fade the whole bar, select the fixture and use its dimmer. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode, with the pixels as sub-fixtures 1.1 to 1.8. Pan and tilt sit on the main fixture, channels 1 and 2, with the same ranges as the 7-Channel Mode; there is no focus channel.

| Ch | Function | Values |
|---|---|---|
| 1 | Pan | 0 to 255 = −180° to 180° |
| 2 | Tilt | 0 to 255 = −225° to 45° |
| 3 | Pixel 1 dimmer | 0 off to 255 full |
| 4 | Pixel 1 red | 0 to 255 |
| 5 | Pixel 1 green | 0 to 255 |
| 6 | Pixel 1 blue | 0 to 255 |
| 7 | Pixel 2 dimmer | 0 off to 255 full |
| 8 | Pixel 2 red | 0 to 255 |
| 9 | Pixel 2 green | 0 to 255 |
| 10 | Pixel 2 blue | 0 to 255 |
| 11 | Pixel 3 dimmer | 0 off to 255 full |
| 12 | Pixel 3 red | 0 to 255 |
| 13 | Pixel 3 green | 0 to 255 |
| 14 | Pixel 3 blue | 0 to 255 |
| 15 | Pixel 4 dimmer | 0 off to 255 full |
| 16 | Pixel 4 red | 0 to 255 |
| 17 | Pixel 4 green | 0 to 255 |
| 18 | Pixel 4 blue | 0 to 255 |
| 19 | Pixel 5 dimmer | 0 off to 255 full |
| 20 | Pixel 5 red | 0 to 255 |
| 21 | Pixel 5 green | 0 to 255 |
| 22 | Pixel 5 blue | 0 to 255 |
| 23 | Pixel 6 dimmer | 0 off to 255 full |
| 24 | Pixel 6 red | 0 to 255 |
| 25 | Pixel 6 green | 0 to 255 |
| 26 | Pixel 6 blue | 0 to 255 |
| 27 | Pixel 7 dimmer | 0 off to 255 full |
| 28 | Pixel 7 red | 0 to 255 |
| 29 | Pixel 7 green | 0 to 255 |
| 30 | Pixel 7 blue | 0 to 255 |
| 31 | Pixel 8 dimmer | 0 off to 255 full |
| 32 | Pixel 8 red | 0 to 255 |
| 33 | Pixel 8 green | 0 to 255 |
| 34 | Pixel 8 blue | 0 to 255 |

### Channel by channel

**1 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**2 · Tilt** — Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.

**3 · Pixel 1 dimmer** — Dimmer of pixel 1. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**4 · Pixel 1 red** — Red of pixel 1.

**5 · Pixel 1 green** — Green of pixel 1.

**6 · Pixel 1 blue** — Blue of pixel 1.

**7 · Pixel 2 dimmer** — Dimmer of pixel 2. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**8 · Pixel 2 red** — Red of pixel 2.

**9 · Pixel 2 green** — Green of pixel 2.

**10 · Pixel 2 blue** — Blue of pixel 2.

**11 · Pixel 3 dimmer** — Dimmer of pixel 3. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**12 · Pixel 3 red** — Red of pixel 3.

**13 · Pixel 3 green** — Green of pixel 3.

**14 · Pixel 3 blue** — Blue of pixel 3.

**15 · Pixel 4 dimmer** — Dimmer of pixel 4. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**16 · Pixel 4 red** — Red of pixel 4.

**17 · Pixel 4 green** — Green of pixel 4.

**18 · Pixel 4 blue** — Blue of pixel 4.

**19 · Pixel 5 dimmer** — Dimmer of pixel 5. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**20 · Pixel 5 red** — Red of pixel 5.

**21 · Pixel 5 green** — Green of pixel 5.

**22 · Pixel 5 blue** — Blue of pixel 5.

**23 · Pixel 6 dimmer** — Dimmer of pixel 6. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**24 · Pixel 6 red** — Red of pixel 6.

**25 · Pixel 6 green** — Green of pixel 6.

**26 · Pixel 6 blue** — Blue of pixel 6.

**27 · Pixel 7 dimmer** — Dimmer of pixel 7. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**28 · Pixel 7 red** — Red of pixel 7.

**29 · Pixel 7 green** — Green of pixel 7.

**30 · Pixel 7 blue** — Blue of pixel 7.

**31 · Pixel 8 dimmer** — Dimmer of pixel 8. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**32 · Pixel 8 red** — Red of pixel 8.

**33 · Pixel 8 green** — Green of pixel 8.

**34 · Pixel 8 blue** — Blue of pixel 8.
