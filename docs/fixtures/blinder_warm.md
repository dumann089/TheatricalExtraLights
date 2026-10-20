# Blinder (warm)

`theatricalextralights:blinder_warm` · family: [Blinders & strobes](/fixtures/blinders-strobes)

Eight-lamp blinder, fixed warm tungsten.

## Personalities

| Mode | Channels |
|---|---|
| 1-Channel Mode | 1 |
| 8-Channel Pixel Mode | 8 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 1-Channel Mode (1 ch)

The colour is fixed by the block.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

## 8-Channel Pixel Mode (8 ch)

One channel per lamp, seen from the front: top row first, left to right. The colour is fixed by the block.

| Ch | Function | Values |
|---|---|---|
| 1 | Lamp 1 | 0 off to 255 full |
| 2 | Lamp 2 | 0 off to 255 full |
| 3 | Lamp 3 | 0 off to 255 full |
| 4 | Lamp 4 | 0 off to 255 full |
| 5 | Lamp 5 | 0 off to 255 full |
| 6 | Lamp 6 | 0 off to 255 full |
| 7 | Lamp 7 | 0 off to 255 full |
| 8 | Lamp 8 | 0 off to 255 full |

### Channel by channel

**1 · Lamp 1** — Dimmer of lamp 1 (top, left seen from the front). The fixture's room light follows the brightest lamp.

**2 · Lamp 2** — Dimmer of lamp 2 (top, right seen from the front). The fixture's room light follows the brightest lamp.

**3 · Lamp 3** — Dimmer of lamp 3 (row 2, left seen from the front). The fixture's room light follows the brightest lamp.

**4 · Lamp 4** — Dimmer of lamp 4 (row 2, right seen from the front). The fixture's room light follows the brightest lamp.

**5 · Lamp 5** — Dimmer of lamp 5 (row 3, left seen from the front). The fixture's room light follows the brightest lamp.

**6 · Lamp 6** — Dimmer of lamp 6 (row 3, right seen from the front). The fixture's room light follows the brightest lamp.

**7 · Lamp 7** — Dimmer of lamp 7 (bottom, left seen from the front). The fixture's room light follows the brightest lamp.

**8 · Lamp 8** — Dimmer of lamp 8 (bottom, right seen from the front). The fixture's room light follows the brightest lamp.
