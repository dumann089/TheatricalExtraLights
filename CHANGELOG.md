# Changelog

## 1.4.12 (mc 1.20.1)

### Gobo moving heads
- Profile personality is now **29ch - Profile 16bit**: two new channels after the prism rotation for an **animation wheel** (11: out / flames / water / clouds / breakup, 12: indexed orientation or continuous scroll in both directions). The effect texture scrolls in front of the gate and modulates both the volumetric beam and the projected gobo. Channels 13-29 shift by two (frost, zoom, focus, pan/tilt 16 bit, speed, framing shutters).

### Rendering
- **Shadows in the beam and on the projected spot.** Blocks and entities that cross the cone now cut the light behind them: an occupancy grid of the blocks around each beam (rebuilt every half second or when the beam leaves it) plus up to 8 entity boxes are tested in both the volumetric and the gobo projector shaders. Config `beamShadows` (default on).
- Raymarch **sample LOD** when the camera is inside or next to a cone: fewer steps per ray (and cheaper haze) so close-up beams no longer stall the GPU, including on Low. Energy is kept by scaling with step size. Distant beams still drop samples as before.

### Fixes
- The **O** key (Open Configuration Menu) now opens the real settings screen instead of the empty legacy one.
- Custom gobos larger than one network chunk are reassembled before decoding, so players fetching them from the server get the image.
- Profile card waiting text no longer says "28ch".

### Docs
- New VitePress wiki under `docs/` (guides, full fixture reference, grandMA2 files); grandMA2 generator and XML files added under `tools/grandma2/`.

## 1.4.11 (mc 1.20.1)

- Fixed a client crash with Shimmer when a Profile head had its shutter closed (dynamic light registered without an emission position).
- Beam haze is now anchored in world space: moving a head sweeps the beam through the haze instead of dragging the haze along.
### Gobo moving heads
- New **27ch - Profile 16bit** personality on the 8 gobo heads: shutter (0 closed, 1-254 strobe 0.5 to 10 Hz in whole ticks, 255 open), 16-bit dimmer, RGB, gobo wheel and rotation, 3-, 6- or 9-facet prism with indexed or continuous rotation, shown in the gobo preview, frost, zoom, focus, 16-bit pan/tilt, pan/tilt speed, framing shutters.
- Heads now travel to their target with a motor model (720 to 25 deg/s with easing) instead of jumping; the speed channel sets the pace, 0 = tracking.
- Prism splits the volumetric beam and the projected gobo into three facets; frost softens the projected spot.

### Followspot console
- 8 position presets (pan, tilt, intensity, focus): keys 1-8 recall, Shift+1-8 store in control mode; click / Shift+click in the console screen; preset strip in the HUD.

### Cleanup
- Removed the laser block entity debug logging.
- Fixed the one-frame beam flicker on every DMX change (block entity data is now sent without a chunk rebuild).

## 1.4.10 (mc 1.20.1)

- 19ch framing-shutter personality (A/B blades, frame rotation) on gobo heads, clipping the raymarch beam and the projected spot.
- Redesigned config screens (TelUi), custom gobo screen, GUI-scale fit; DMX-driven heads no longer show pan/tilt faders.
- Laser screen with emergency stop; pyro screen with arm/disarm key; every block moved off the generic DMX screen.
- Gobo & framing-shutter live preview in the gobo head screen.
- Followspot console rework: mouse aiming, operator HUD, screen-space aim mapping, camera aligned with Theatrical's real light direction on hung fixtures.
- Iris/Oculus probe cached instead of reflecting every frame.
- Fixed z-fighting on the gobo spot, StackOverflowError when opening the config screen.
