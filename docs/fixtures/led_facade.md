# LED Facade

`theatricalextralights:led_facade` · family: [Wash, spot & followspot](/fixtures/wash-spot)

A pixel-mapped LED wall. Paint the lit pixels in its screen, choose the resolution and smoothing, and drive every pixel from the desk.

- Resolution 16, 32, 64, 128 or 256; smoothing Sharp, Soft or Very soft; brushes of 1, 2, 3 and 5 pixels.
- The maximum number of universes is `ledFacadeMaxUniverses` in the config (default 64).
- The facade emits one aggregated coloured light in front of the wall.

## 4-Channel Pixel (per lit pixel) (4 ch)

Lit pixels are numbered row by row, left to right, top to bottom, four channels each, continuing into the next universe every 512 channels. The screen's footprint line shows pixels, channels and universes. Map it on the console as an LED matrix of 4-channel RGB pixels.

| Ch | Function | Values |
|---|---|---|
| 1 | Pixel dimmer | 0 to 255 |
| 2 | Pixel red | 0 to 255 |
| 3 | Pixel green | 0 to 255 |
| 4 | Pixel blue | 0 to 255 |

### Channel by channel

**1 · Pixel dimmer** — Dimmer of this pixel.

**2 · Pixel red** — Red of this pixel.

**3 · Pixel green** — Green of this pixel.

**4 · Pixel blue** — Blue of this pixel.
