# Fixture config screen

Every Extra Lights fixture has a dark, flat configuration panel that scales down automatically when the window or GUI scale is too small. The layout is the same for all fixtures; some sections only appear when the fixture has the corresponding feature.

![Config screen of a gobo head](/images/config-screen.png)

## Opening it

| Action | Result |
|---|---|
| **Right-click** the fixture | Opens its config screen |
| **Sneak + right-click** | Toggles Theatrical's debug overlay for that fixture, client side, no screen |
| Right-click with the **Fixture Wrench** | Opens the mount editor (position and rotation offsets) |
| Right-click with Theatrical's **Configuration Card** | Patches the fixture at the card's next address |

If the fixture belongs to a network you are not allowed to configure in Theatrical, the interaction is refused. Network permissions are Theatrical's.

## Header

The fixture name and a pill with the channel count of the selected mode, for example **29 ch**.

## Patch

- **Start address** and **Network Universe**: digits only.
- Footprint line: `Universe 1 · channels 1-29 (29 ch)`, or `Enter a valid start address (1-512)`, or an overflow suggestion pointing to the next universe.
- Overlap line: green `No address overlap on this network`, or an orange warning naming the other fixture and the shared channels. Non-blocking.

## Position

Shown only for fixtures whose pan and tilt are set by hand rather than by DMX (adjustable water jets, for example). DMX-driven moving heads do not show it.

- **Tilt** slider −90° to 90°, **Pan** slider −180° to 180°, applied live.
- **Copy pan/tilt** and **Paste pan/tilt**: a client-side clipboard to align several fixtures identically. Paste is greyed until something has been copied.

## Gobo & framing shutters

Shown on the gobo moving heads. A live disc renders the current gobo (your custom PNG if one is mapped), its rotation and the prism copies, with the four blades drawn over it. Next to it: `Blade n  A x%  B y%` for each blade and the frame rotation. In a mode without blades it reads *Blades out in this mode*.

## Profile 16bit (live DMX)

Shown on the gobo heads when the 29-channel mode is selected. It decodes what the fixture receives: shutter state or strobe frequency, dimmer and effective output, RGB with a swatch, motor speed, prism facets and rotation, animation wheel, frost, pan and tilt in degrees. Before the first DMX frame arrives it says so.

## Settings

- **Mode**: the personalities of the fixture. Up to three are shown as a segmented row, more as a cycling button. Only shown when the fixture has more than one mode.
- **Network**: cycles through the networks known to your client, including *none*.
- **Custom Gobos**: on fixtures with a gobo wheel, opens the [custom gobo screen](/guide/custom-gobos).

## Save and cancel

**Save** sends the address, universe, network, mode and position to the server. **Cancel** discards. Keyboard: **Enter** saves, **Escape** cancels.

## Specialised screens

Some fixtures extend the panel with an extra section:

- **Laser**: output state, colour swatches and the [emergency stop](/guide/lasers).
- **Pyro**: arm state, output and the [arm/disarm key](/guide/pyro).
- **Water jets**: height field, thickness slider and, on cone jets, a cone angle slider.
- **LED Facade**: a pixel canvas with brushes, resolution, smoothing, universe and address, and a footprint line in pixels, channels and universes.
- **Followspot Console**: its own layout, see [Followspot console](/guide/followspot).

## Fixture Wrench

The **Fixture Wrench** item opens a side overlay while the world stays visible: offset X, Y, Z up to ±2.5 blocks in 0.05 steps, and yaw, pitch, roll up to ±180°, plus **Reset**. Use it to seat a fixture precisely on a truss or to fake a bracket angle. The transform is saved with the block and synced to everyone.

![Fixture Wrench overlay](/images/fixture-wrench.png)
