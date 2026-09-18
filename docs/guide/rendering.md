# Rendering & performance

Extra Lights renders beams on the GPU. This page explains what each option does and how to trade quality for frame rate.

![Volumetric beams with haze and shadows](/images/rendering-beams.png)

## Settings screen

Open it with the **O** key (rebindable, category *Theatrical Extra Lights*), with the client command `/tel config`, from Mod Menu on Fabric, or from the mod list config button on Forge. It is not a pause screen and does not darken the world, so you can tune while looking at your beams. Changes apply live and are written to the [config file](/guide/config-file) when you close the screen. **Reset defaults** restores everything.

Three tabs:

**General**: volumetric beam on/off, lens glow, 2D beam, brightness, beam range, fade length, gobo distance.

**Beam**: engine, quality, anisotropy, haze, max beams per frame, and the slices-only density, max alpha and slice count.

**Spot**: spot follows cone, max spot radius.

## The two engines

**Raymarch** (default) draws every beam in one full-screen pass that marches through the cone, reads the scene depth so beams stop on geometry, applies the gobo, the framing blades, the animation wheel, the haze and the shadows. Quality sets the number of samples per ray:

| Quality | Samples | When |
|---|---|---|
| Low | 8 | Integrated GPUs, large rigs |
| Medium | 16 | |
| High | 24 | Default |
| Ultra | 32 | Screenshots and video |

**Slices** is the older engine: stacked camera-facing discs. It supports gobos and blades but not haze structure or shadows. Use it only if the raymarch shader fails on your driver.

## Haze

The haze is a three-octave noise anchored in **world space** with a slow drift. A moving head sweeps its beam through the haze; the structure does not travel with the beam. *Haze* sets how much structure is visible, *Anisotropy* how strongly light scatters toward the camera (higher values make beams pointing at you brighter, like real haze).

## Shadows

With `beamShadows` on (default), each beam keeps a small occupancy grid of the blocks around it, 40 cells per axis, rebuilt every half second or when the beam leaves the grid, and adds up to eight entity bounding boxes. Both the beam shader and the projected-gobo shader test the path from each point to the source against them: a pillar or a player in the beam cuts the light behind it, in the volume and on the floor.

The player is excluded in first person so you do not shadow yourself when standing in a beam. Cost grows with beam count; if a big rig stutters, set `beamShadows` to `false` in the config file to confirm the cause.

## Projected gobos

The spot on surfaces is a separate shader that projects the gobo texture from the lens, with focus and frost blur, blade clipping, prism copies, animation wheel and shadows. `maxGoboDistance` caps how far it is drawn.

## Dynamic light

Theatrical places a light source where each beam lands. With *Spot follows cone*, its radius follows the cone section at that distance instead of the focus value alone, bounded by *Max spot radius*. With **Shimmer** installed the light is coloured and follows the fixture's RGB mix, the dimmer and the shutter.

## Compatibility

| Mod | Notes |
|---|---|
| **Embeddium / Sodium / Rubidium** | Works. Beams are drawn each frame; fixtures mark their chunk section for refresh while lit. |
| **Iris / Oculus** | The mod checks whether a shader pack is active and switches beams to fallback render types when one is. Beams keep rendering but a pack may change their look; the projected gobo and the haze are the most affected. |
| **Shimmer** | Optional coloured dynamic light. If Shimmer is present the mod feeds it its own GLSL through a resource provider so shader reloads keep working. |

## Performance tips

- Set **Max beams per frame** to the number of fixtures you actually light at once; the far ones are skipped first.
- Beams **farther than about 50 blocks** automatically march with fewer samples.
- Beams **close to the camera** (inside or within a few blocks of the cone) also drop samples, down to 3–4 per ray. The cone fills the screen there, so cost is per pixel; brightness stays the same because each step is longer.
- Prefer a long throw and a small **Beam range** over many wide beams: the cost is per pixel covered.
- On a laptop, Medium quality with haze at 0.4 is a good compromise.
- Pyro has its own budgets in the [config file](/guide/config-file#pyro).
