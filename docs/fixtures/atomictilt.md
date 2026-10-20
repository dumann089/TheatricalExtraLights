# Atomic Tilt

`theatricalextralights:atomictilt` · family: [Moving heads & beams](/fixtures/moving-heads)

A strobe head on a tilting yoke, no pan.

- Every flash is timed from the game clock, so all clients see the same flashes. A flash shorter than a frame still shows for one frame.
- The room light follows the flashes tick by tick.

## Personalities

| Mode | Channels |
|---|---|
| 6-Channel RGB + Focus + Tilt | 6 |
| 7-Channel RGB + Focus + Strobe + Tilt | 7 |
| 6-Channel Atomic + Focus + Tilt | 6 |
| 9-Channel Atomic RGB + Focus + Tilt | 9 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 6-Channel RGB + Focus + Tilt (6 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Tilt | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Tilt** — Tilt of the yoke over its full travel.

## 7-Channel RGB + Focus + Strobe + Tilt (7 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Strobe | 0 closed, 1 to 254 strobe, 255 open |
| 7 | Tilt | 0 to 255 |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Strobe** — Mechanical shutter. 0 is closed (dark regardless of the dimmer), 1 to 254 strobes from slow to fast, 255 is open. Use 255 for normal operation and drop to 0 for a hard blackout that keeps the dimmer level.

**7 · Tilt** — Tilt of the yoke over its full travel.

## 6-Channel Atomic + Focus + Tilt (6 ch)

White head driven like a Martin Atomic: intensity, duration, rate, effects.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Focus | 0 tight to 255 wide |
| 6 | Tilt | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**6 · Tilt** — Tilt of the yoke over its full travel.

## 9-Channel Atomic RGB + Focus + Tilt (9 ch)

The Atomic engine with a colour.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 to 255 |
| 2 | Flash duration | 0 to 255 |
| 3 | Flash rate | 0 to 255 |
| 4 | Effects | 0 to 255 |
| 5 | Red | 0 to 255 |
| 6 | Green | 0 to 255 |
| 7 | Blue | 0 to 255 |
| 8 | Focus | 0 tight to 255 wide |
| 9 | Tilt | 0 to 255 |

### Channel by channel

**1 · Intensity** — Flash intensity.

**2 · Flash duration** — Length of each flash, 12 ms at 0 to 650 ms at 255.

**3 · Flash rate** — 0 stops the strobe (as on the real unit); 1 to 255 runs 0.5 to 25 flashes a second.

**4 · Effects** — 0-9 plain strobe; 10-39 blinder, lamp on continuously; 40-69 ramp up; 70-99 ramp down; 100-129 ramp up and down; 130-159 random; 160-189 lightning bursts; 190-219 spikes; 220-255 sparkle (each bar segment flashes on its own in the pixel modes).

**5 · Red** — Red component of the additive colour mix.

**6 · Green** — Green component of the additive colour mix.

**7 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**8 · Focus** — Cone width of the beam. Low values give a tight pencil beam, high values a wide wash. It also drives the size of the light spot on the ground.

**9 · Tilt** — Tilt of the yoke over its full travel.
