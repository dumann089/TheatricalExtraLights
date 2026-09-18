# Followspot console

The **Followspot Console** is an operator desk. It links to one 7-channel fixture on a Theatrical network, gives you level sliders for it, and a **Take control** mode that places your camera at the lens so you aim with the mouse like a real followspot operator. Your player never moves; only the camera does.

![Followspot beam on a performer](/images/followspot-stage.png)

![Followspot console screen](/images/followspot-screen.png)

## Linking a fixture

1. Place the console within a few blocks of where you will operate and right-click it.
2. In **DMX patch**, choose the **Network**, then enter the **Universe** and **Start address** of the fixture, exactly as patched on the fixture.
3. Press **Link fixture**.

The target must be a fixture with a **7-channel** footprint: intensity, red, green, blue, focus, pan, tilt. The mod's **Followspot** fixture is the natural choice; the plain moving heads in their *7ch - Standard* mode also qualify. Valid addresses are 1 to 506. The status lamp reads *Linked* with the fixture name, or tells you what is missing (*Select a network*, *No 7ch fixture at U1 @ 10*).

## Modes

| Mode | Console drives | Desk / software keeps |
|---|---|---|
| **Full control** | Intensity, colour, focus, pan, tilt | nothing |
| **Pan / Tilt only** | Pan, tilt | Intensity, colour, focus |

*Pan / Tilt only* is the usual choice in a real show: the lighting desk keeps the levels and colour, the operator only aims.

## Desk controls

With the screen open:

- **Levels**: Intensity, Focus, Red, Green, Blue sliders (disabled in Pan / Tilt only).
- **Aim**: shows pan and tilt; hold your **movement keys** (W A S D on QWERTY, Z Q S D on AZERTY, whatever you bound) to nudge the aim in 2° steps. The mapping works in screen space, so *right* on the keys always moves the spot to the right on stage, even on an upside-down fixture.
- **Presets**: eight buttons. **Click** recalls, **Shift + click** stores pan, tilt and, in Full control, intensity and focus.
- **Link fixture** applies the patch; **Take control** enters operator mode; **Cancel** closes.

## Operator mode

![Operator HUD](/images/followspot-hud.png)

Operator mode closes the screen and puts the camera at the fixture lens, looking down the beam. The fixture keeps its real geometry: hung under a truss, the camera hangs with it, and the beam lands where the reticle points.

| Input | Action |
|---|---|
| Mouse | Aim. Pan is limited to −90° to 90°, tilt to −45° to 45°. |
| Movement keys | Nudge the aim in 2° steps |
| Mouse wheel | Intensity, 8 steps per notch (Full control only) |
| Shift + wheel | Focus (Full control only) |
| Space | Blackout toggle: cuts the light and restores the previous level |
| 1 to 8 | Recall preset |
| Shift + 1 to 8 | Store preset |
| Escape | Back to the desk |

The wheel, left and right click, inventory, chat, drop and swap-hand keys are swallowed while operating, so you cannot accidentally break a block or leave the session. Opening any screen ends the session; the fixture keeps the operator's angles for a moment so the hand-off is smooth.

The HUD shows a reticle, the fixture name, pan and tilt, the throw distance to the first surface hit, an output lamp, intensity and focus meters with a colour swatch, a BLACKOUT badge when engaged, the preset strip and a key hint. In Pan / Tilt only it simply reads *Levels stay on the desk · aim only*.

## Presets

The eight positions are stored on the console block, saved with the world and shared by every operator using that console. Recall is instant.

## Notes

- A console must be used from within 8 blocks; farther away its packets are refused.
- One console drives one fixture at a time. Several consoles can drive several fixtures.
- The console itself is not a DMX fixture and needs no address.
- On a multiplayer server everyone sees the fixture move; only the operator sees through the lens.
