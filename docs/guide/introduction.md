# Introduction

**Theatrical: Extra Lights** is an addon for the [Theatrical](https://modrinth.com/mod/theatrical) mod. Theatrical provides the foundation: DMX networks, Art-Net input, trusses, the Configuration Card and a first set of fixtures. Extra Lights adds the fixtures a real show designer misses, plus the tooling around them.

![Concert rig built with Extra Lights](/images/hero-rig.png)

## What you get

- **Gobo moving heads** with three personalities: a simple 10-channel mode, a 19-channel mode with four framing blades, and a 29-channel *Profile 16bit* mode with everything a modern profile fixture has (shutter with strobe, 16-bit dimmer and pan/tilt, RGB, gobo wheel, prism, animation wheel, frost, zoom, focus, framing shutters). See [Gobo heads & personalities](/guide/gobo-heads).
- **Volumetric beams** rendered on the GPU: haze anchored in the room, gobos projected on surfaces, and shadows cast by blocks and players inside the beam. See [Rendering & performance](/guide/rendering).
- **Dozens of conventional fixtures**: moving heads and beams, washes, Source Four profiles, PAR cans and arrays, LED panels and bars, blinders, strobes.
- **Effects**: lasers with an emergency stop, LED fountains, scrollers and water jets.
- **Pyro**: more than fifty firework launchers, gerbs, flame projectors and a flame thrower, confetti cannons, with an arm/disarm safety key.
- **Followspot console**: an operator desk that aims a followspot from a client-side camera at the lens, with a HUD and eight position presets.
- **Custom gobos**: drop your own PNG files in a folder and pick them in game.
- **grandMA2 fixture files** for the gobo heads, matching the mod channel for channel.

## How it fits with Theatrical

Everything in Extra Lights is a normal Theatrical fixture. You patch it with Theatrical's **Configuration Card** or from the fixture's own config screen, it joins a Theatrical **network**, and it listens to DMX coming from Theatrical's Art-Net interface or lighting desk blocks. Any console that speaks Art-Net works: grandMA, ETC Eos, Chamsys, QLC+, Onyx, Lightkey, and so on.

## Where to go next

| I want to… | Read |
|---|---|
| Install the mod | [Installation](/guide/installation) |
| Get a light moving in five minutes | [Quick start](/guide/quick-start) |
| Understand addresses, universes and networks | [Patching](/guide/patching) |
| Know every channel of a fixture | [Fixture reference](/fixtures/overview) |
| Use a grandMA2 | [grandMA2 fixture files](/guide/grandma2) |
| Fix a problem | [Troubleshooting & FAQ](/guide/faq) |

## Community

Development and support happen on the [Theatrical Discord](https://discord.gg/7qMs5d6). Source code and releases are on [GitHub](https://github.com/dumann089/TheatricalExtraLights). The mod is released under the MIT license.
