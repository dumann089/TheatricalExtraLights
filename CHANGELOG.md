# Changelog

## Unreleased (mc 1.20.1)

### New fixtures
- **Showtec Stage Blinder 200 Blaze** (4ch dimmer / RGB).
- **Dense RGB Bar**, **Dense Vertical RGB Bar** and **Dense Moving RGB Bar**: one LED per pixel, with their own head models. The RGB, vertical and moving bars also get pixel modes (28ch and 36ch) and a 1x2 blinder joins the blinder family.
- **Atomic Strobe** rebuilt around an Atomic-style engine (intensity, flash duration, rate, effects) with 4, 8, 10, 40 and 400 channel modes, and new **Atomic Tilt** modes (6ch and 9ch Atomic, with a focus channel).
- **LED Panel 2** pixel modes (4x4 and 8x8), with grandMA2 profiles for the 4ch and pixel modes, plus grandMA2 profiles for the Atomic strobe modes.

### Lasers
- Realistic laser engine (analytic haze scattering, dust motes, striations, aperture glare, 512-segment figures, detail slider) and the Ether Dream DAC projector built on it.

### Rendering
- **Flames**: realistic particle flames with an additive glow on the flame thrower and flame projector, an optional raymarched volumetric flame with heat distortion, a *Flames* tab in the settings and a Jet / Burst style button on each machine.
- **Flow2Jet CO2**: volumetric plume matched to stage photos, seamless loop sample, smooth dissipation, `co2JetReach` slider.
- **Blinders**: hot lamps, bloom, glare and a wide, white dynamic light on every blinder.
- **LED bars**: haze sheets that fuse neighbouring bars, per-pixel colour ramp, `rgbBarBeamIntensity` and `rgbBarBeamLength` options (volumetric bar beams follow the length).
- **Strobes**: additive glow, hot core and haze halo on flashes; coloured strobe halos keep their colour.
- Performance pass on glows, beam shadows (shared occupancy grids, stale-rebuild budget) and raymarch proxies (oriented hull instead of the world-aligned box).

### Fixes
- **Atomic Tilt (6ch / 9ch Atomic modes): the dimmer now lights the head steadily when the flash rate is 0** instead of staying dark until the strobe channel was raised (issue 47). The strobe only adds flashing on top of the dimmer level.
- LED fountain uses the smoothed colour and dimmer like the other fixtures.
- Forge: single client entry point and keybind registered before `RegisterKeyMappingsEvent`.
- French translations for the gobo settings.

## 1.4.12 (mc 1.20.1)

### Ether Dream laser projector
- New **Ether Dream Laser Projector** block driven by a virtual Ether Dream DAC instead of DMX. The client advertises itself on UDP 7654 and accepts one control connection on TCP 7765, so MadMapper, LaserOS, CloudLase or any Ether Dream capable software on the LAN can draw ILDA frames into the world. Scan angle, projection scale and DAC binding are set in the projector screen.
- Config `etherDreamEnabled` (default on) plus bind address, ports, buffer size, max point rate and `laserDac*` rendering options.
- New **Color Block DB4 (12ch)** fixture.

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
