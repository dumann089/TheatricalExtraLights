# Consoles, rig & misc

## Followspot Console (`followspot_console`)

An operator desk, not a DMX fixture: it has no address of its own and drives one 7-channel fixture by network, universe and address. Full description in [Followspot console](/guide/followspot).

- Two modes: **Full control** (levels, colour, focus, aim) or **Pan / Tilt only** (aim only).
- **Take control** operator mode with mouse aiming, wheel dimmer, blackout and HUD.
- Eight presets stored on the block.
- Usable within 8 blocks.

## Mini truss (Theatrical: Misc tab)

Rig pieces without DMX, sized for the mod's fixtures.

| Block | Registry id |
|---|---|
| Mini Truss | `truss` |
| Mini Truss Cross | `truss_joint` |
| Mini Truss Corner (L) | `truss_corner` |
| Mini Truss Corner (T) | `truss_corner_t` |

Fixtures placed against the underside of a truss hang from it and flip automatically. Theatrical's own truss blocks work the same way.

## Items

| Item | Registry id | Use |
|---|---|---|
| Fixture Wrench | `fixture_wrench` | Right-click a fixture to open the mount editor: offset X, Y, Z up to ±2.5 blocks and yaw, pitch, roll up to ±180°, with Reset. See [Fixture config screen](/guide/config-screen#fixture-wrench). |
| Confetti Cannon | `confetti_cannon` | Handheld version of the block: fires a confetti burst where you look. |
| Flow2Jet CO₂ Cannon | `flow2jet` | Handheld CO₂ jet. |

Theatrical's **Configuration Card** patches Extra Lights fixtures too, with automatic universe wrap; see [Patching](/guide/patching).
