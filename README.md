# Theatrical: Extra Lights

**Extra stage lighting for [Theatrical](https://modrinth.com/mod/theatrical)** — moving heads, LED panels, PARs, lasers, water jets, pyro, and more.  
Built for concert and theatre setups in Minecraft, controlled over DMX / Art-Net like the base mod.

> **This is an addon.** [Theatrical](https://modrinth.com/mod/theatrical) is **required**. Keep both mods on the latest compatible versions.

[![Discord](https://img.shields.io/discord/481830554447118371.svg?label=Join%20Theatrical%20Discord)](https://discord.gg/7qMs5d6)

---

## Screenshots

| Concert lighting | Stage wash | Full rig build |
|:---:|:---:|:---:|
| ![Concert lighting](https://cdn.modrinth.com/data/cached_images/177b5dae2852e0f469fcfc30fe8b845c739f75c2_0.webp) | ![Stage wash](https://cdn.modrinth.com/data/cached_images/21f9d3d9e6272944196ab716646cd321d43cd760_0.webp) | ![Full rig](https://cdn.modrinth.com/data/tkqUgNnN/images/fae4b5f4da174516f931ec3de9960e34e2c4de42.png) |

---

## What is this mod?

Theatrical already gives you a solid lighting foundation — DMX networks, trusses, configuration cards, and a growing fixture library. **Extra Lights** adds the fixtures that are not in the official pack: more variety for real show design, without replacing Theatrical.

Use it when you want:

- More **moving heads** and beam fixtures (7ch / 10ch personalities)
- **LED panels**, blinders, washes, and RGB bars
- **PAR arrays** (Par56, Par64, Par 1000) in many colors
- **Effects** — strobes, atomic strobes, lasers, scrollers, water jets
- **Pyro** — gerbs, flame projectors, and a large firework launcher set
- **Rig pieces** — mini truss segments for cleaner builds

New lights are added over time. Updates follow Theatrical releases — use a recent Theatrical build when possible.

Need help or want to follow development? Join the **[Theatrical Discord](https://discord.gg/7qMs5d6)**.

---

## Requirements

| | |
|---|---|
| **Minecraft** | 1.20.1 |
| **Loaders** | Fabric · Forge |
| **Required mod** | [Theatrical](https://modrinth.com/mod/theatrical) |
| **Tested with** | Theatrical `1.0.0-alpha.28.120+mc1.20.1` or newer |

Install **Theatrical first**, then Extra Lights.

---

## Features

### Fixture library

Hundreds of placeable fixtures across creative tabs **Theatrical: Extra Lights** and **Theatrical Pyro**, including:

- **Moving heads** — Moving 500, Beam 7R, Mac VIP, Sharpy+, Robit Spot, Verve Spot, VL2/VL6, scans, gobo variants…
- **Wash & spot** — Source Four, followspot, searchlight, wash LED, mini wash, VL 6000
- **PAR & panels** — LED Par, Par 1000, x8 Par64, 2×2 / 2×8 / 6×3 Par64 arrays, big panels, shaped LED panels
- **Blinders & strobes** — 4×2 blinder, atomic strobe (34ch), atomic tilt, white strobe
- **Effects** — laser, laser mirror, LED fountain, RGB / vertical bars, scrollers, invisible light
- **Water jets** — jets, cones, bloom, fog, organ pipes, moving fan, and more
- **Pyro** — 50+ firework launchers, Pyro Fan, RGB launcher, confetti, gerbs, flame projector, **flame thrower**, daytime powder *(see [Pyro](#pyro-theatrical-pyro-tab) below)*
- **Rig** — mini truss, corner, T-corner, cross joint
- **Followspot console** — operator desk to remotely aim and dim a patched 7ch followspot

All DMX fixtures work with Theatrical’s **Configuration Card**, **Art-Net networks**, and in-game patching.

### Improved patching & configuration *(recent)*

- **Configuration card** — automatically jumps to the **next universe at address 1** when a fixture no longer fits in the remaining 512 channels (e.g. Universe 1 @ 500 + 34ch Atomic → Universe 2 @ 1)
- **Clear chat feedback** — fixture name, network, channel range, wrap notice, and next card address
- **Fixture config screen** — clean UI with labels above fields, Save / Cancel, live **DMX footprint** preview, and personality-aware channel count
- **Keyboard shortcuts** — Enter to save, Escape to cancel
- **Stable beams** — smoother pan/tilt and intensity at full DMX without flicker or double beams
- **Address overlap warning** — fixture config screen warns when another fixture on the same network uses overlapping channels (non-blocking)

### Followspot console *(recent)*

- **Operator desk** — place a **Followspot Console**, patch a **7-channel** fixture (network / universe / address), and control it from the panel
- **Live DMX** — Focus, RGB, and Intensity sliders plus **ZQSD** pan/tilt while the menu is open
- **First-person aiming** — **Control fixture** switches to a client-side camera at the fixture lens (mouse + ZQSD); **Esc** returns to the desk — no player teleport
- **French & German** — full UI translation for the console screen

![Followspot console UI](https://github.com/user-attachments/assets/24806e2a-fff8-4193-958f-a28e9cdad39e)

### Pyro *(Theatrical Pyro tab)*

Stage pyrotechnics controlled over DMX — launchers, mines, gerbs, and specialty effects for finales and daytime shows.

**Creative tab:** **Theatrical Pyro** — all pyro blocks and firework launchers in one place.

#### DMX control

Most **firework launchers** use a **3-channel** personality:

| Channel | Role |
|---------|------|
| **Intensity** | Fire rate (0 = off). Values ≥ 2 start firing; higher = faster shots. |
| **Tilt** | Launch angle (0° = horizontal → 180° = straight up). Controls apex height. |
| **Focus** | Launch power — how high and how far each shell travels. |

Patch with the Configuration Card like any other fixture. **Shift + right-click** to open address / network settings.

#### Firework launchers *(50+ presets)*

Each launcher is a fixed effect type. Grouped by pattern family:

| Family | Examples |
|--------|----------|
| **Comets** | Red / blue / green / gold — trailing shells, no burst |
| **Long comets** | Gold, red, blue, green, silver — dense trail, short hover at apex |
| **Peonies** | Classic sphere bursts — 7 colours |
| **Willows** | Heavy drooping trails — 7 colours |
| **Chrysanthemums** | Streak-filled spheres — 7 colours |
| **Crossettes** | Crisscross secondary breaks — 7 colours |
| **Mines** | Ground fountain — tall column, minimal fall-back |
| **Special shells** | Palm, ring, spinner, horsetail, spider, diadem, salute, heart, double burst, multicolor, whistler |
| **Strobe** | White strobe burst (scatter) · **White aerial strobe** (silent ascent, 4 s flash in the sky) |
| **Daytime powder** | Lime, magenta, yellow, orange, red, blue — Holi-style coloured smoke, visible in daylight |
| **Rainbow fan** | Multi-colour daytime powder fan |

Effects use a custom spark renderer (not vanilla particles), optional **dynamic light** on bursts, and budget-limited smoke trails on comets.

#### Specialty pyro fixtures

| Fixture | Channels | Description |
|---------|----------|-------------|
| **Pyro Fan** | 3ch or **10ch** | 10 tubes firing gold comets in a vertical fan. 10ch mode: per-tube intensity + global tilt/focus. |
| **RGB Firework Launcher** | **7ch** | Intensity, RGB, effect select, tilt, focus — pick any comet/burst preset and tint it. |
| **Confetti cannon** | 1ch | High-altitude confetti burst on DMX trigger. |
| **Gold gerb** | DMX | Stage gerb fountain. |
| **Flame projector** | DMX | Continuous flame effect. |
| **Flame thrower** | 2ch | Directed flame jet with pan control (intensity + pan). |

#### Tips

- Aim launchers with **tilt** before a show; use **focus** to match song dynamics.
- **Daytime powder** and **rainbow fan** are designed for bright maps — minimal dynamic light, coloured plumes.
- **Long comets** stop at apex and fade with a short controlled drop (~3 blocks).
- **Mines** burst at the launcher — no rocket flight; good for stage-edge columns.
- Intense pyro shows are throttled server-side (max concurrent rockets, per-tick launch cap) to stay stable on large rigs.

**Pyro-related config** (in `theatricalextralights.json`): `fireworkRenderDistance`, `fireworkDynamicLightEnabled`, `fireworkSmokeEnabled`, `fireworkSmokeBudgetPerTick`, `fireworkSmokeSpawnInterval`.

---

## Configuration file

Extra Lights creates `config/theatricalextralights.json` in your Minecraft instance folder on first launch. Edit it while the game is **closed**, or change values and restart.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `laserBeamLength` | float | `400.0` | Maximum laser beam length in blocks (minimum `20`). |
| `rgbBarBeamLength` | float | `9.0` | RGB bar light reach in blocks (minimum `1`). |
| `renderLens` | boolean | `true` | Draw lens glow on compatible fixtures. |
| `maxGoboDistance` | float | `500.0` | Maximum distance for gobo projection (minimum `10`). |
| `render2DBeam` | boolean | `false` | Use flat 2D beam rendering instead of volumetric beams where supported. |
| `laserPassThroughBlocks` | string array | see below | Block IDs lasers pass through instead of stopping on. |

**Default `laserPassThroughBlocks`:** `minecraft:glass`, `minecraft:tinted_glass`, `minecraft:iron_bars`, `minecraft:barrier`.

Add scenic blocks (backdrops, decor) so laser beams continue to a wall behind them. Blocks from **Theatrical** and **Extra Lights** are always pass-through — you do not need to list them.

**Example:**

```json
{
  "laserBeamLength": 400.0,
  "rgbBarBeamLength": 9.0,
  "renderLens": true,
  "maxGoboDistance": 500.0,
  "render2DBeam": false,
  "laserPassThroughBlocks": [
    "minecraft:glass",
    "minecraft:black_concrete"
  ]
}
```

---

## Quick start

1. Install **Theatrical** + **Extra Lights** for your loader (Fabric or Forge).
2. Create or join a **Theatrical network** (Art-Net) in-game.
3. Grab fixtures from the creative menu and build your rig on truss or floor.
4. Use the **Configuration Card** (from Theatrical) to patch address / universe onto fixtures.
5. Control everything from your DMX software through Art-Net, same as base Theatrical.

**Followspot console**

1. Place a **Followspot Console** near your rig and a **7ch followspot** (or other 7-channel fixture).
2. **Right-click** the console → set network, universe, and DMX address to match the fixture → **Link fixture**.
3. Adjust Focus / RGB / Intensity with the sliders; use **ZQSD** for pan/tilt from the desk.
4. **Control fixture** for first-person aiming at the lens; **Esc** to exit.

**Pyro**

1. Grab launchers from the **Theatrical Pyro** creative tab.
2. Patch each launcher on your Art-Net network (3ch default — intensity / tilt / focus).
3. Raise **intensity** above 1 to fire; adjust **tilt** for height and **focus** for reach.
4. For finales, combine **Pyro Fan** (10 tubes), **mines** (ground columns), and aerial shells (peonies, long comets, aerial strobe).

**Tips**

- **Shift + right-click** a fixture to open its DMX settings (address, universe, mode, network).
- Enable **auto-increment** on the Configuration Card to patch a row of fixtures quickly.
- Moving heads use **7ch / 10ch** modes — switch personality in the config screen if your desk expects a different profile.

---

## Downloads

- [Modrinth](https://modrinth.com/mod/theatrical-extra-lights)
- [GitHub](https://github.com/dumann089/TheatricalExtraLights)

---

## Contributing & credits

**Authors:** [dumann089](https://github.com/dumann089) · Rushmead · J8-Diablo · nailec  

**License:** [MIT](LICENSE) — Copyright (c) 2025 Stuart Pomeroy

Bug reports and feature requests are welcome on GitHub. For questions and show screenshots, the Theatrical Discord is the best place to ask.

---

*Not affiliated with Theatrical core development — community addon maintained alongside the main mod.*
