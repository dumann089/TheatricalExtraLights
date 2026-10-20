# Dense Moving RGB Bar

`theatricalextralights:dense_moving_bar` · family: [Moving heads & beams](/fixtures/moving-heads)

The Moving Bar with twenty-four pixels, one every sixteenth of a block, for fine chases and gradients.

- Hangs from a truss or stands on the floor; hung upside down, pan and tilt are mirrored automatically.
- Right-click opens the config screen; pan and tilt are DMX-driven, so there are no position sliders.

## Personalities

| Mode | Channels |
|---|---|
| 7-Channel Mode | 7 |
| 98-Channel Pixel Mode (Pan, Tilt + 24x Dim/RGB) | 98 |

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

## 98-Channel Pixel Mode (Pan, Tilt + 24x Dim/RGB) (98 ch)

Pixels are numbered 1 to 24 from left to right, seen from the front of the head. Each pixel lights its cell and its LED dot; a single lit pixel throws one thin sheet of light into the haze, and neighbouring lit pixels of the same colour merge into one sheet that widens with them. There is no master dimmer: to fade the whole bar, select the fixture and use its dimmer. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode, with the pixels as sub-fixtures 1.1 to 1.24. Pan and tilt sit on the main fixture, channels 1 and 2, with the same ranges as the 7-Channel Mode; there is no focus channel.

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
| 35 | Pixel 9 dimmer | 0 off to 255 full |
| 36 | Pixel 9 red | 0 to 255 |
| 37 | Pixel 9 green | 0 to 255 |
| 38 | Pixel 9 blue | 0 to 255 |
| 39 | Pixel 10 dimmer | 0 off to 255 full |
| 40 | Pixel 10 red | 0 to 255 |
| 41 | Pixel 10 green | 0 to 255 |
| 42 | Pixel 10 blue | 0 to 255 |
| 43 | Pixel 11 dimmer | 0 off to 255 full |
| 44 | Pixel 11 red | 0 to 255 |
| 45 | Pixel 11 green | 0 to 255 |
| 46 | Pixel 11 blue | 0 to 255 |
| 47 | Pixel 12 dimmer | 0 off to 255 full |
| 48 | Pixel 12 red | 0 to 255 |
| 49 | Pixel 12 green | 0 to 255 |
| 50 | Pixel 12 blue | 0 to 255 |
| 51 | Pixel 13 dimmer | 0 off to 255 full |
| 52 | Pixel 13 red | 0 to 255 |
| 53 | Pixel 13 green | 0 to 255 |
| 54 | Pixel 13 blue | 0 to 255 |
| 55 | Pixel 14 dimmer | 0 off to 255 full |
| 56 | Pixel 14 red | 0 to 255 |
| 57 | Pixel 14 green | 0 to 255 |
| 58 | Pixel 14 blue | 0 to 255 |
| 59 | Pixel 15 dimmer | 0 off to 255 full |
| 60 | Pixel 15 red | 0 to 255 |
| 61 | Pixel 15 green | 0 to 255 |
| 62 | Pixel 15 blue | 0 to 255 |
| 63 | Pixel 16 dimmer | 0 off to 255 full |
| 64 | Pixel 16 red | 0 to 255 |
| 65 | Pixel 16 green | 0 to 255 |
| 66 | Pixel 16 blue | 0 to 255 |
| 67 | Pixel 17 dimmer | 0 off to 255 full |
| 68 | Pixel 17 red | 0 to 255 |
| 69 | Pixel 17 green | 0 to 255 |
| 70 | Pixel 17 blue | 0 to 255 |
| 71 | Pixel 18 dimmer | 0 off to 255 full |
| 72 | Pixel 18 red | 0 to 255 |
| 73 | Pixel 18 green | 0 to 255 |
| 74 | Pixel 18 blue | 0 to 255 |
| 75 | Pixel 19 dimmer | 0 off to 255 full |
| 76 | Pixel 19 red | 0 to 255 |
| 77 | Pixel 19 green | 0 to 255 |
| 78 | Pixel 19 blue | 0 to 255 |
| 79 | Pixel 20 dimmer | 0 off to 255 full |
| 80 | Pixel 20 red | 0 to 255 |
| 81 | Pixel 20 green | 0 to 255 |
| 82 | Pixel 20 blue | 0 to 255 |
| 83 | Pixel 21 dimmer | 0 off to 255 full |
| 84 | Pixel 21 red | 0 to 255 |
| 85 | Pixel 21 green | 0 to 255 |
| 86 | Pixel 21 blue | 0 to 255 |
| 87 | Pixel 22 dimmer | 0 off to 255 full |
| 88 | Pixel 22 red | 0 to 255 |
| 89 | Pixel 22 green | 0 to 255 |
| 90 | Pixel 22 blue | 0 to 255 |
| 91 | Pixel 23 dimmer | 0 off to 255 full |
| 92 | Pixel 23 red | 0 to 255 |
| 93 | Pixel 23 green | 0 to 255 |
| 94 | Pixel 23 blue | 0 to 255 |
| 95 | Pixel 24 dimmer | 0 off to 255 full |
| 96 | Pixel 24 red | 0 to 255 |
| 97 | Pixel 24 green | 0 to 255 |
| 98 | Pixel 24 blue | 0 to 255 |

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

**35 · Pixel 9 dimmer** — Dimmer of pixel 9. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**36 · Pixel 9 red** — Red of pixel 9.

**37 · Pixel 9 green** — Green of pixel 9.

**38 · Pixel 9 blue** — Blue of pixel 9.

**39 · Pixel 10 dimmer** — Dimmer of pixel 10. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**40 · Pixel 10 red** — Red of pixel 10.

**41 · Pixel 10 green** — Green of pixel 10.

**42 · Pixel 10 blue** — Blue of pixel 10.

**43 · Pixel 11 dimmer** — Dimmer of pixel 11. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**44 · Pixel 11 red** — Red of pixel 11.

**45 · Pixel 11 green** — Green of pixel 11.

**46 · Pixel 11 blue** — Blue of pixel 11.

**47 · Pixel 12 dimmer** — Dimmer of pixel 12. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**48 · Pixel 12 red** — Red of pixel 12.

**49 · Pixel 12 green** — Green of pixel 12.

**50 · Pixel 12 blue** — Blue of pixel 12.

**51 · Pixel 13 dimmer** — Dimmer of pixel 13. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**52 · Pixel 13 red** — Red of pixel 13.

**53 · Pixel 13 green** — Green of pixel 13.

**54 · Pixel 13 blue** — Blue of pixel 13.

**55 · Pixel 14 dimmer** — Dimmer of pixel 14. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**56 · Pixel 14 red** — Red of pixel 14.

**57 · Pixel 14 green** — Green of pixel 14.

**58 · Pixel 14 blue** — Blue of pixel 14.

**59 · Pixel 15 dimmer** — Dimmer of pixel 15. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**60 · Pixel 15 red** — Red of pixel 15.

**61 · Pixel 15 green** — Green of pixel 15.

**62 · Pixel 15 blue** — Blue of pixel 15.

**63 · Pixel 16 dimmer** — Dimmer of pixel 16. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**64 · Pixel 16 red** — Red of pixel 16.

**65 · Pixel 16 green** — Green of pixel 16.

**66 · Pixel 16 blue** — Blue of pixel 16.

**67 · Pixel 17 dimmer** — Dimmer of pixel 17. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**68 · Pixel 17 red** — Red of pixel 17.

**69 · Pixel 17 green** — Green of pixel 17.

**70 · Pixel 17 blue** — Blue of pixel 17.

**71 · Pixel 18 dimmer** — Dimmer of pixel 18. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**72 · Pixel 18 red** — Red of pixel 18.

**73 · Pixel 18 green** — Green of pixel 18.

**74 · Pixel 18 blue** — Blue of pixel 18.

**75 · Pixel 19 dimmer** — Dimmer of pixel 19. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**76 · Pixel 19 red** — Red of pixel 19.

**77 · Pixel 19 green** — Green of pixel 19.

**78 · Pixel 19 blue** — Blue of pixel 19.

**79 · Pixel 20 dimmer** — Dimmer of pixel 20. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**80 · Pixel 20 red** — Red of pixel 20.

**81 · Pixel 20 green** — Green of pixel 20.

**82 · Pixel 20 blue** — Blue of pixel 20.

**83 · Pixel 21 dimmer** — Dimmer of pixel 21. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**84 · Pixel 21 red** — Red of pixel 21.

**85 · Pixel 21 green** — Green of pixel 21.

**86 · Pixel 21 blue** — Blue of pixel 21.

**87 · Pixel 22 dimmer** — Dimmer of pixel 22. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**88 · Pixel 22 red** — Red of pixel 22.

**89 · Pixel 22 green** — Green of pixel 22.

**90 · Pixel 22 blue** — Blue of pixel 22.

**91 · Pixel 23 dimmer** — Dimmer of pixel 23. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**92 · Pixel 23 red** — Red of pixel 23.

**93 · Pixel 23 green** — Green of pixel 23.

**94 · Pixel 23 blue** — Blue of pixel 23.

**95 · Pixel 24 dimmer** — Dimmer of pixel 24. 0 turns the pixel off whatever its colour. There is no master dimmer in this mode; the dynamic light in the room follows the brightest pixel.

**96 · Pixel 24 red** — Red of pixel 24.

**97 · Pixel 24 green** — Green of pixel 24.

**98 · Pixel 24 blue** — Blue of pixel 24.
