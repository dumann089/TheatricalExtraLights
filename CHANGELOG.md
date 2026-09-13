# Changelog

## 1.4.11 (mc 1.20.1)

- Fixed a client crash with Shimmer when a Profile head had its shutter closed (dynamic light registered without an emission position).
- Beam haze is now anchored in world space: moving a head sweeps the beam through the haze instead of dragging the haze along.
### Gobo moving heads
- New **27ch - Profile 16bit** personality on the 8 gobo heads: mechanical shutter, 16-bit dimmer, RGB, gobo wheel and rotation, 3-facet prism with indexed or continuous rotation, frost, zoom, focus, 16-bit pan/tilt, pan/tilt speed, framing shutters.
- Heads now travel to their target with a motor model (720 to 25 deg/s with easing) instead of jumping; the speed channel sets the pace, 0 = tracking.
- Prism splits the volumetric beam into three facets; frost softens the projected spot.

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
