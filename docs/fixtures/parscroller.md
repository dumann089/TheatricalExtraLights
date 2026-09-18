# Par64 (Scroller)

`theatricalextralights:parscroller` · family: [Wash, spot & followspot](/fixtures/wash-spot)

PAR 64 with an eight-gel colour scroller.

## 2-Channel Mode (2 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Gel position | 0 to 255 across 8 gels |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Gel position** — Position of the scroll. 0 is the first gel, 255 the last; the eight gels are red, green, blue, yellow, magenta, cyan, orange and white, cross-faded continuously so intermediate values blend two neighbours like a real scroller mid-frame.
