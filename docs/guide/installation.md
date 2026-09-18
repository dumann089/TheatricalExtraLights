# Installation

## Requirements

| | |
|---|---|
| **Minecraft** | 1.20.1 |
| **Loader** | Fabric or Forge |
| **Required mod** | [Theatrical](https://modrinth.com/mod/theatrical) `1.0.0-alpha.28.120+mc1.20.1` or newer |
| **Required library** | [Architectury API](https://modrinth.com/mod/architectury-api) (Theatrical needs it too) |

Extra Lights follows Theatrical releases. When you update one, update the other.

## Steps

1. Install Fabric or Forge for Minecraft 1.20.1.
2. Put **Architectury API**, **Theatrical** and **Theatrical: Extra Lights** in your `mods` folder. Use the jar that matches your loader: `TheatricalExtraLights-fabric-…` or `TheatricalExtraLights-forge-…`.
3. Start the game once. The config file `config/theatricalextralights.json` is created with default values.

The mod must be installed on **both the server and every client**. Fixtures, DMX and patching run on the server; beams, gobos and screens are rendered on the client.

## Optional mods

| Mod | Effect |
|---|---|
| **Embeddium / Sodium** | Faster chunk rendering. Fully supported; the beams refresh their chunk section while a fixture is lit. |
| **Iris / Oculus** | Shader packs. Beams keep rendering; the mod detects the shader pipeline once and caches the result. Some packs alter the look of the volumetric beam. |
| **Shimmer** | Coloured dynamic light. Fixtures register a coloured light source at the point their beam hits, which follows the shutter and dimmer. |

## Updating

Replace the jar in `mods`. Worlds saved with an older version load fine: fixture data is stored per block and new channels default to sensible values (shutter open, colour white, prism out). If a personality changed channel count between versions, re-import the matching [grandMA2 fixture file](/guide/grandma2) and re-patch on the desk.

## Uninstalling

Remove the jar. Blocks from the mod disappear from the world on next load, as with any mod.
