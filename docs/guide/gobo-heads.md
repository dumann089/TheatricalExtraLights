# Gobo heads & personalities

Eight moving heads share the same gobo engine and the same three DMX personalities:

- Spot Xtreme (Gobos)
- VL6C (Gobos)
- Iris 700 Spot (Gobos)
- Pro Spot (Gobos)
- Mini Scan (Gobos)
- Mini Spot (Gobos)
- Moving Scan Beams
- Moving VL2C Beams

They differ in body model, size and pivot points, not in DMX behaviour. Everything on this page applies to all eight. The exact channel tables are in the [fixture reference](/fixtures/gobo-heads).

![The three modes in the config screen](/images/gobo-heads-modes.png)

## Choosing a personality

Open the fixture config screen (right-click) and pick a **Mode**:

| Mode | Channels | Use it when |
|---|---|---|
| **10-Channel Mode** | 10 | You want the simplest patch: dimmer, RGB, focus, pan, tilt, gobo, zoom, gobo rotation. |
| **19ch - Framing Shutters** | 19 | Same as 10ch plus a four-blade framing module with A/B corners and frame rotation. |
| **29ch - Profile 16bit** | 29 | The full profile fixture: shutter with strobe, 16-bit dimmer, RGB, gobo wheel and rotation, 3/6/9-facet prism, animation wheel, frost, zoom, focus, 16-bit pan/tilt with motor speed, framing shutters. |

Changing the mode changes the channel count immediately; the footprint line in the screen updates and warns about overlaps.

## Gobo wheel

Each head has its own wheel, typically 10 slots: slot 0 is open (a plain beam), the others hold gobos. The wheel channel is divided into as many equal ranges as there are slots; moving between slots animates the wheel and cross-fades the two textures, in the beam and on the projected spot. The gobo rotation channel spins the gobo continuously; 0 is stopped.

Slots can hold your own images, see [Custom gobos](/guide/custom-gobos).

## Framing shutters (19ch and 29ch)

Four blades, each with two corners named **A** and **B** following the grandMA convention: blade 1 is top, 2 right, 3 bottom, 4 left, seen from the fixture looking along the beam. Corner values run from 0 (blade fully out) to 255 (blade fully in). Pushing only A or only B tilts the blade; pushing both slides it straight in. The **frame rotation** channel rotates the whole module ±55°, 128 is neutral.

The blades clip both the volumetric beam and the projected spot, and their edge softens as you defocus, like a real profile.

The **Gobo & framing shutters** card in the config screen shows a live front view: current gobo with its rotation, blade positions in percent, and frame rotation.

![Gobo and framing shutters card](/images/gobo-framing-card.png)

## Profile 16bit mode (29ch)

### Shutter and strobe

| Value | Behaviour |
|---|---|
| 0 | Closed |
| 1 to 254 | Mechanical strobe from 0.5 Hz to 10 Hz |
| 255 | Open |

The strobe runs in whole game ticks, so 10 Hz is the physical maximum. The beam, the projected gobo and the coloured dynamic light all follow it.

### Dimmer, pan and tilt in 16 bit

The dimmer and both axes have a fine channel. In Profile mode the head does not jump to its target: a motor model moves it at up to 720°/s and eases into position. The **pan/tilt speed** channel slows this down to 25°/s at 255; values 0 to 2 mean *tracking* (instant).

### Prism

| Value | Prism |
|---|---|
| 0 to 127 | Out |
| 128 to 170 | 3 facets |
| 171 to 213 | 6 facets |
| 214 to 255 | 9 facets |

The prism splits the beam and the projected gobo into copies arranged in a ring. The **prism rotation** channel indexes the ring on 0 to 127, spins it clockwise on 128 to 191 and counter-clockwise on 192 to 255, slow to fast. Switching from indexed to continuous starts from the indexed angle.

### Animation wheel

An effect texture scrolls in front of the gate and modulates the beam and the spot:

| Value | Effect |
|---|---|
| 0 to 15 | Out |
| 16 to 75 | Flames |
| 76 to 135 | Water |
| 136 to 195 | Clouds |
| 196 to 255 | Breakup |

The **animation rotation** channel sets the orientation of the wheel on 0 to 127 and scrolls it on 128 to 191 (forward) or 192 to 255 (reverse), slow to fast. Combine it with a gobo for the classic fire-through-a-window look.

### Frost

Frost softens the projected spot like an extra defocus: 0 none, 255 full.

### The Profile card

With the 29-channel mode selected, the config screen shows a **Profile 16bit (live DMX)** card with the decoded values: shutter state and strobe frequency, dimmer and effective output, RGB with a colour swatch, prism facets and rotation, animation wheel, frost, motor speed, pan and tilt in degrees. When a light does not behave as expected, this card tells you what the fixture actually receives.

![Profile card](/images/profile-card.png)

## Coloured light in the room

With Theatrical's dynamic light enabled, and optionally Shimmer installed, each head places a coloured light where its beam lands. The light follows the dimmer, the shutter and the RGB mix, and turns off when the shutter is closed.
