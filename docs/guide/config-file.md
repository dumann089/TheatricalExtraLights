# Config file

Extra Lights writes `config/theatricalextralights.json` in the Minecraft instance folder (client) or the server folder (server) on first launch, and normalises it on every load: missing keys get their defaults. All options are local to the side that reads them; nothing is synced. Most rendering options are also exposed in the in-game [settings screen](/guide/rendering#settings-screen); a few are file-only.

Edit the file while the game is closed, or use the settings screen, which writes the file once when you close it.

## Beams and rendering

| Key | Default | Range | Effect |
|---|---|---|---|
| `volumetricBeamEnabled` | `true` | | Master switch for volumetric beams. |
| `volumetricEngine` | `"RAYMARCH"` | `RAYMARCH`, `LEGACY_SLICES` | Beam engine. Anything else falls back to raymarch. |
| `raymarchQuality` | `"HIGH"` | `LOW`, `MEDIUM`, `HIGH`, `ULTRA` | Samples per ray: 8, 16, 24, 32. Automatically reduced when the camera is close to a beam or the beam is far. |
| `raymarchAnisotropy` | `0.55` | −0.9 to 0.9 | Forward versus back scattering of the haze. |
| `raymarchDustAmount` | `0.55` | 0 to 1 | Amount of haze structure in the beam (*Haze* in the screen). |
| `raymarchMaxBeamsPerFrame` | `128` | 1 to 128 in the screen | Beam budget per frame. |
| `beamShadows` | `true` | | Blocks and entities inside the beam cast shadows in the volume and on the projected spot. File-only. |
| `volumetricBeamDistance` | `64.0` | 8 to 256 | Beam range in blocks. |
| `volumetricBeamBrightness` | `0.15` | 0.01 to 1 | Beam brightness multiplier. |
| `volumetricBeamFadeLength` | `12.0` | 0 to 48 | Fade at the end of a beam that hits nothing. |
| `volumetricBeamSlices` | `128` | 16 to 512 | Slices engine only. |
| `volumetricBeamDensity` | `0.15` | 0 to 2 | Slices engine only. |
| `volumetricBeamMaxAlpha` | `0.15` | 0 to 1 | Slices engine only. |
| `render2DBeam` | `true` | | Flat 2D beam on the fixtures that support it. |
| `renderLens` | `true` | | Lens glow on fixtures. |
| `maxGoboDistance` | `500.0` | 50 to 1000 | Maximum distance of the projected gobo. |
| `spotFollowsBeam` | `true` | | Size the dynamic-light spot from the cone at the lit distance instead of the focus value alone. |
| `spotMaxRadius` | `48.0` | 1 to 256 | Upper bound of that spot radius. |

## Lasers and bars

| Key | Default | Effect |
|---|---|---|
| `laserBeamLength` | `400.0` | Maximum laser beam length in blocks, minimum 20. |
| `laserPassThroughBlocks` | `[]` | Block ids lasers go through instead of stopping on, for example `"minecraft:glass"`. Theatrical and Extra Lights blocks are always pass-through. |
| `laserRealistic` | `true` | Realistic laser engine (thin beams, sheets, impacts, haze). `false` draws the old flat ribbons. Automatically off under an Iris shader pack. |
| `laserHaze` | `0.7` | Haze density seen by lasers, 0 to 1. At 0 only the impacts are visible. |
| `laserBrightness` | `1.0` | Overall laser gain, 0.05 to 4. |
| `laserBeamRadiusCm` | `1.0` | Beam radius at the lens in centimetres, 0.2 to 6. |
| `laserImpacts` | `true` | Hot spot and line where beams hit blocks. |
| `laserScanFlicker` | `true` | Bright scan head running along the pattern when the Persistence channel is low. |

## Ether Dream laser projector

Client-side virtual DAC used by the **Ether Dream Laser Projector** block, see [Ether Dream guide](/guide/ether-dream). Also switchable from the *Laser* tab of the Extra Lights settings (*Ether Dream DAC*).

| Key | Default | Meaning |
| --- | --- | --- |
| `etherDreamEnabled` | `true` | Start the virtual Ether Dream DAC with the game client. `false` keeps the ports closed; projector blocks stay dark. |
| `etherDreamBindAddress` | `"0.0.0.0"` | Interface the TCP control socket binds to. Leave on all interfaces unless you need to pin it to one network card. |
| `etherDreamTcpPort` | `7765` | Ether Dream control port (one controller at a time). |
| `etherDreamBroadcastPort` | `7654` | UDP port the DAC announces itself on, once per second. |
| `etherDreamBufferCapacity` | `1800` | Point buffer advertised to the controller. |
| `etherDreamMaxPointRate` | `100000` | Highest point rate (pps) accepted from the controller. |
| `etherDreamHwRevision` / `etherDreamSwRevision` | `2` / `2` | Revision numbers reported in the broadcast. |
| `etherDreamMac` | `"02:00:00:ED:01:00"` | MAC address reported in the broadcast; change it when two clients on the same LAN both run a DAC. |
| `laserDacMaxRays` | `96` | Maximum visible segments drawn per frame. |
| `laserDacBlankThreshold` | `256` | Colour level under which a point counts as blanked. |
| `laserDacPersistenceMs` | `90` | Window over which received points are fused into one picture; raise it if the drawing flickers. |
| `laserDacHazeRadius` | `0.11` | Halo radius around each ray, in blocks. |
| `rgbBarBeamLength` | `9.0` | Reach of the RGB bar glow, minimum 1. |

## Pyro

| Key | Default | Effect |
|---|---|---|
| `maxConcurrentRockets` | `768` | Cap on live firework rockets. |
| `maxSparksPerRocket` | `600` | Sparks per shell. |
| `fireworkRenderDistance` | `2048.0` | Ceiling for the pyro render distance. |
| `fireworkDynamicRenderDistance` | `true` | Follow the client render distance (or server view distance), capped by the value above. |
| `fireworkDynamicLightEnabled` | `true` | Dynamic light on bursts. |
| `fireworkSmokeEnabled` | `true` | Smoke trails on comets. |
| `fireworkSmokeBudgetPerTick` | `24` | Smoke particles per tick. |
| `fireworkSmokeSpawnInterval` | `3` | Ticks between smoke spawns, minimum 1. |

## LED Facade

| Key | Default | Effect |
|---|---|---|
| `ledFacadeMaxUniverses` | `64` | Maximum universes a single facade may span. |

## Example

```json
{
  "volumetricEngine": "RAYMARCH",
  "raymarchQuality": "HIGH",
  "raymarchDustAmount": 0.55,
  "beamShadows": true,
  "volumetricBeamDistance": 64.0,
  "laserBeamLength": 400.0,
  "laserRealistic": true,
  "laserHaze": 0.7,
  "laserPassThroughBlocks": ["minecraft:glass", "minecraft:black_concrete"]
}
```

Keys you leave out keep their defaults.
