# Lasers & emergency stop

The **Laser** fixture draws multi-beam patterns with three colour groups, size, amplitude, speed, rotation, pan, tilt, focus and a persistence trail. The **Laser Mirror** block bounces beams. Both are DMX fixtures like any other; channel details are in the [reference](/fixtures/lasers-effects).

![Laser screen](/images/laser-screen.png)

## Laser screen

Right-click a laser to open its screen. Besides the usual patch section (address, universe, network) it shows:

- **Output state**: whether the laser is currently emitting, from DMX intensity and the stop state.
- **Emergency stop**: a large red control that cuts the output immediately, whatever the DMX says. The laser stays dark until you release the stop from the same screen. The state is saved with the world and synced to every player.

Use it exactly like the E-stop on a real laser controller: before a rehearsal pause, when someone walks on stage, or when the desk sends something unexpected.

## Beam length and pass-through

Two config options shape the beams, see [Config file](/guide/config-file):

- `laserBeamLength`: maximum length in blocks (default 400).
- `laserPassThroughBlocks`: blocks the beam goes through instead of stopping on. Glass, tinted glass, iron bars and barriers by default. Add your scenic blocks so beams reach the wall behind a backdrop. Blocks from Theatrical and Extra Lights are always pass-through.

## Realistic rendering

Since the realistic laser engine, a laser behaves like the real thing rather than a glowing ribbon:

- **The beam is a few centimetres wide** and never fades with distance. It is only visible through the light the haze scatters back, strongly forward-biased: look towards the projector and beams dazzle, look from behind it and they almost disappear.
- **Haze is mandatory.** The *Haze* slider (Extra Lights settings, *Laser* tab) sets the haze density seen by lasers. At 0 the air is clear and only the impacts remain visible, exactly like a laser in a clean room. Haze drifts slowly, so beams show billows along their length.
- **Scanned patterns become sheets.** With *Persistence* (channel 19) high, the eye fuses the fast scan into a cone or a plane whose brightness falls off in 1/r from the lens, with brighter edges where the scanner turns a sharp corner. With *Persistence* low, the pattern is shown as separate beams and a bright scan head runs along the figure.
- **Impacts.** Where a beam hits a block it burns a tiny white-cored dot with a soft halo; a sheet draws the whole figure as a line on the wall.
- **Colours saturate to white** at the core, like an over-exposed camera, and stay pure on the edges.

Settings in the *Laser* tab: *Realistic laser* (falls back to the flat ribbons when off, or automatically under an Iris shader pack), *Impacts*, *Scan head*, *Haze*, *Laser brightness* and *Beam radius*. The raymarch *Quality* setting also drives the haze detail of lasers. Config keys are listed in [Config file](/guide/config-file).
