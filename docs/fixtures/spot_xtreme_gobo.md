# Xtreme Spot (Gobos)

`theatricalextralights:spot_xtreme_gobo` · family: [Gobo moving heads](/fixtures/gobo-heads)

Large profile spot with a gobo wheel. One of the eight gobo heads: it shares the 10ch, 19ch framing shutters and 29ch Profile 16bit personalities and the same rendering engine.

- Hangs from a truss or stands on the floor; the geometry flips automatically when hung.
- The config screen shows a live **Gobo & framing shutters** preview and, in 29ch mode, the **Profile 16bit (live DMX)** card with every decoded value.
- Custom gobos apply to the wheel this head uses, so all heads sharing the wheel show them.
- Behaviour guide: [Gobo heads & personalities](/guide/gobo-heads). Desk file: [grandMA2 fixture files](/guide/grandma2).

## Personalities

| Mode | Channels |
|---|---|
| 10-Channel Mode | 10 |
| 19ch - Framing Shutters | 19 |
| 29ch - Profile 16bit | 29 |

Select the mode in the fixture config screen; the footprint changes immediately.

## 10-Channel Mode (10 ch)

The simplest patch. Framing blades stay out, prism and animation wheel are unavailable.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 to 255 |
| 6 | Pan | 0 to 255 = −180° to 180° |
| 7 | Tilt | 0 to 255 = −225° to 45° |
| 8 | Gobo wheel | Equal ranges, one per slot |
| 9 | Zoom | 0 narrow (1°) to 255 wide (19°) |
| 10 | Gobo rotation | 0 stop, 1 to 255 speed |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.

**6 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**7 · Tilt** — Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.

**8 · Gobo wheel** — Selects the gobo. The channel is split into as many equal ranges as the wheel has slots (10 on most wheels): 0 is the open slot, then each following range is one gobo. Moving between slots animates the wheel and cross-fades the two textures in the beam and on the surface. Any slot can carry a [custom image](/guide/custom-gobos).

**9 · Zoom** — Beam angle from 1° to 19°. Zoom changes the cone of the volumetric beam and the size of the projected gobo together; combine with focus for a sharp or soft edge at any size.

**10 · Gobo rotation** — Continuous rotation of the gobo. 0 is stopped; higher values spin faster. The direction is fixed.

## 19ch - Framing Shutters (19 ch)

Channels 1 to 10 are identical to the 10-Channel Mode; the framing module follows.

| Ch | Function | Values |
|---|---|---|
| 1 | Intensity | 0 off to 255 full |
| 2 | Red | 0 to 255 |
| 3 | Green | 0 to 255 |
| 4 | Blue | 0 to 255 |
| 5 | Focus | 0 to 255 |
| 6 | Pan | 0 to 255 = −180° to 180° |
| 7 | Tilt | 0 to 255 = −225° to 45° |
| 8 | Gobo wheel | Equal ranges, one per slot |
| 9 | Zoom | 0 narrow (1°) to 255 wide (19°) |
| 10 | Gobo rotation | 0 stop, 1 to 255 speed |
| 11 | Blade 1 A | 0 out to 255 in |
| 12 | Blade 1 B | 0 out to 255 in |
| 13 | Blade 2 A | 0 out to 255 in |
| 14 | Blade 2 B | 0 out to 255 in |
| 15 | Blade 3 A | 0 out to 255 in |
| 16 | Blade 3 B | 0 out to 255 in |
| 17 | Blade 4 A | 0 out to 255 in |
| 18 | Blade 4 B | 0 out to 255 in |
| 19 | Frame rotation | 0 = −55°, 128 neutral, 255 = +55° |

### Channel by channel

**1 · Intensity** — Master dimmer. 0 is dark, 255 is full output. The beam, the projected spot and the dynamic light in the room all scale with it. Fades are smooth: the client interpolates between DMX frames.

**2 · Red** — Red component of the additive colour mix.

**3 · Green** — Green component of the additive colour mix.

**4 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**5 · Focus** — Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.

**6 · Pan** — Horizontal rotation of the head. 128 is straight ahead relative to the block's facing; the full range is one turn (−180° to 180°). Moves are interpolated between frames so slow fades look smooth.

**7 · Tilt** — Vertical rotation of the head, −225° to 45°. Around 212 the head points straight along its own axis (0°); lower values tilt it forward and down, all the way over the back. Hung upside down the range is mirrored automatically.

**8 · Gobo wheel** — Selects the gobo. The channel is split into as many equal ranges as the wheel has slots (10 on most wheels): 0 is the open slot, then each following range is one gobo. Moving between slots animates the wheel and cross-fades the two textures in the beam and on the surface. Any slot can carry a [custom image](/guide/custom-gobos).

**9 · Zoom** — Beam angle from 1° to 19°. Zoom changes the cone of the volumetric beam and the size of the projected gobo together; combine with focus for a sharp or soft edge at any size.

**10 · Gobo rotation** — Continuous rotation of the gobo. 0 is stopped; higher values spin faster. The direction is fixed.

**11 · Blade 1 A** — Corner A of blade 1 (top blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**12 · Blade 1 B** — Corner B of blade 1 (top blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**13 · Blade 2 A** — Corner A of blade 2 (right blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**14 · Blade 2 B** — Corner B of blade 2 (right blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**15 · Blade 3 A** — Corner A of blade 3 (bottom blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**16 · Blade 3 B** — Corner B of blade 3 (bottom blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**17 · Blade 4 A** — Corner A of blade 4 (left blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**18 · Blade 4 B** — Corner B of blade 4 (left blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**19 · Frame rotation** — Rotates the whole framing module. 127 and 128 are neutral; lower values turn it counter-clockwise up to −55°, higher values clockwise up to +55°.

## 29ch - Profile 16bit (29 ch)

The full profile fixture. Defaults on a fresh fixture: shutter open, dimmer 0, RGB white, prism and animation out, frost 0, pan centred, tilt 0°, speed tracking, blades out. A ready-made [grandMA2 fixture file](/guide/grandma2) exists for this mode.

| Ch | Function | Values |
|---|---|---|
| 1 | Shutter | 0 closed, 1 to 254 strobe 0.5 to 10 Hz, 255 open |
| 2 | Dimmer coarse | 16 bit with the next channel |
| 3 | Dimmer fine | 0 to 255 |
| 4 | Red | 0 to 255 |
| 5 | Green | 0 to 255 |
| 6 | Blue | 0 to 255 |
| 7 | Gobo wheel | Equal ranges, one per slot |
| 8 | Gobo rotation | 0 stop, 1 to 255 speed |
| 9 | Prism | 0 to 127 out, 128 to 170 three facets, 171 to 213 six, 214 to 255 nine |
| 10 | Prism rotation | 0 to 127 indexed 0° to 360°, 128 to 191 CW slow to fast, 192 to 255 CCW slow to fast |
| 11 | Animation wheel | 0 to 15 out, 16 to 75 flames, 76 to 135 water, 136 to 195 clouds, 196 to 255 breakup |
| 12 | Animation rotation | 0 to 127 orientation 0° to 360°, 128 to 191 scroll slow to fast, 192 to 255 reverse scroll |
| 13 | Frost | 0 none to 255 full |
| 14 | Zoom | 0 narrow (1°) to 255 wide (19°) |
| 15 | Focus | 0 to 255 |
| 16 | Pan coarse | 16 bit, −180° to 180° |
| 17 | Pan fine | 0 to 255 |
| 18 | Tilt coarse | 16 bit, −225° to 45° |
| 19 | Tilt fine | 0 to 255 |
| 20 | Pan/tilt speed | 0 to 2 tracking, 3 fast (720°/s) to 255 slow (25°/s) |
| 21 | Blade 1 A | 0 out to 255 in |
| 22 | Blade 1 B | 0 out to 255 in |
| 23 | Blade 2 A | 0 out to 255 in |
| 24 | Blade 2 B | 0 out to 255 in |
| 25 | Blade 3 A | 0 out to 255 in |
| 26 | Blade 3 B | 0 out to 255 in |
| 27 | Blade 4 A | 0 out to 255 in |
| 28 | Blade 4 B | 0 out to 255 in |
| 29 | Frame rotation | 0 = −55°, 128 neutral, 255 = +55° |

### Channel by channel

**1 · Shutter** — Mechanical shutter. 0 closed, 255 open. In between the shutter strobes, from 0.5 Hz at 1 to 10 Hz at 254; the frequency is quantised to whole game ticks, so 10 Hz is the physical maximum and is reached around 80 %. The beam, the projected gobo and the coloured dynamic light all follow the shutter.

**2 · Dimmer coarse** — High byte of the 16-bit dimmer. Patch it as a 16-bit attribute on the desk: the fixture combines it with the fine channel for 65 536 steps, then scales to the rendered intensity.

**3 · Dimmer fine** — Low byte of the 16-bit dimmer.

**4 · Red** — Red component of the additive colour mix.

**5 · Green** — Green component of the additive colour mix.

**6 · Blue** — Blue component of the additive colour mix. With red and green at 255 too the light is white; all three at 0 gives a dark fixture even with the dimmer up.

**7 · Gobo wheel** — Selects the gobo. The channel is split into as many equal ranges as the wheel has slots (10 on most wheels): 0 is the open slot, then each following range is one gobo. Moving between slots animates the wheel and cross-fades the two textures in the beam and on the surface. Any slot can carry a [custom image](/guide/custom-gobos).

**8 · Gobo rotation** — Continuous rotation of the gobo. 0 is stopped; higher values spin faster. The direction is fixed.

**9 · Prism** — Inserts the prism. Below 128 the beam is single. 128 to 170 splits it into three copies, 171 to 213 into six, 214 to 255 into nine, arranged in a ring whose spread grows with the count. Each copy is drawn at 75 %, 60 % or 50 % intensity so the total light stays plausible. The projected gobo is split the same way.

**10 · Prism rotation** — Position or rotation of the prism ring. 0 to 127 sets a fixed angle. 128 to 191 spins clockwise, 192 to 255 counter-clockwise, slow at the start of each range and fast at the end. Switching from indexed to continuous starts the spin from the indexed angle, without a jump.

**11 · Animation wheel** — Puts an effect texture in front of the gate. It modulates the beam and the projected spot: flames for fire, water for caustics, clouds for soft billows, breakup for a dappled foliage look. Combine it with a gobo, for example a window with flames behind it.

**12 · Animation rotation** — 0 to 127 sets the orientation of the wheel and keeps it still. 128 to 191 scrolls the texture forward, 192 to 255 backward, slow to fast (about 0.1 to 1.5 texture widths per second). The scroll direction follows the last indexed orientation.

**13 · Frost** — Diffusion filter. It softens the edge of the projected spot like an extra defocus. The volumetric beam width is not affected.

**14 · Zoom** — Beam angle from 1° to 19°. Zoom changes the cone of the volumetric beam and the size of the projected gobo together; combine with focus for a sharp or soft edge at any size.

**15 · Focus** — Beam sharpness. 0 is a hard-edged beam and a crisp projected image; 255 is soft. On fixtures without a separate zoom channel it also widens the cone slightly.

**16 · Pan coarse** — High byte of the 16-bit pan. With the fine channel the head resolves 65 536 positions over a full turn, so slow moves are stepless.

**17 · Pan fine** — Low byte of the 16-bit pan.

**18 · Tilt coarse** — High byte of the 16-bit tilt. Default position on a fresh fixture is 0°, straight along the head axis.

**19 · Tilt fine** — Low byte of the 16-bit tilt.

**20 · Pan/tilt speed** — Motor speed for pan and tilt. 0 to 2 is tracking: the head follows the desk instantly, use it when the desk itself fades positions. From 3 the head moves on its own motor model, 720°/s at 3 down to 25°/s at 255, with easing at the end of the move. Ideal for slow theatrical travels driven by a snap on the desk.

**21 · Blade 1 A** — Corner A of blade 1 (top blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**22 · Blade 1 B** — Corner B of blade 1 (top blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**23 · Blade 2 A** — Corner A of blade 2 (right blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**24 · Blade 2 B** — Corner B of blade 2 (right blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**25 · Blade 3 A** — Corner A of blade 3 (bottom blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**26 · Blade 3 B** — Corner B of blade 3 (bottom blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**27 · Blade 4 A** — Corner A of blade 4 (left blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**28 · Blade 4 B** — Corner B of blade 4 (left blade, seen from the fixture along the beam). 0 is fully out of the beam, 255 fully in. Pushing only one corner tilts the blade edge; pushing A and B together slides it straight in. Follows the grandMA shaper convention.

**29 · Frame rotation** — Rotates the whole framing module. 127 and 128 are neutral; lower values turn it counter-clockwise up to −55°, higher values clockwise up to +55°.
