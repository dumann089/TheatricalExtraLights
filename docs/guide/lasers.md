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
