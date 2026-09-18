# Pyro & fireworks

All pyro fixtures are in the **Theatrical: Pyro** creative tab. Every launcher, the RGB launcher, the gerb, the flame projector, the flame thrower and the confetti cannon carry a **safety arm**: disarmed, they read DMX but never fire. See [Pyro & safety arm](/guide/pyro).

![Gold confetti burst on a stage](/images/pyro-confetti.png)

## Firework launchers (3-Channel Firework)

One block per effect, about 150 presets, all with the same personality:

| Ch | Function | Values |
|---|---|---|
| 1 | Fire | 0 idle. A rise from 0 fires one shot; 1 stays single-shot; 2 to 255 fires continuously at 1 to 10 shots per second. |
| 2 | Tilt | 0 to 255 = launch pitch 0° (horizontal) to 180° (straight up) |
| 3 | Power | 0 to 255 = 0.7× to 2.6× launch speed, sets height and reach |

Shots get a small random horizontal drift. The server caps concurrent rockets and launches per tick, see the [config file](/guide/config-file#pyro).

### Presets

Registry ids follow `firework_<effect>`.

| Family | Ids |
|---|---|
| Comets | `red_comet`, `blue_comet`, `green_comet`, `gold_comet`, `gold_bell_comet` |
| Long comets | `gold_long_comet`, `red_long_comet`, `blue_long_comet`, `green_long_comet`, `silver_long_comet` |
| Peonies | `red_peony`, `blue_peony`, `green_peony`, `gold_peony`, `white_peony`, `amber_peony`, `violet_peony` |
| Willows | `gold_willow`, `red_willow`, `blue_willow`, `green_willow`, `white_willow`, `amber_willow`, `violet_willow` |
| Chrysanthemums | `chrysanthemum_blue`, `_red`, `_green`, `_gold`, `_white`, `_amber`, `_violet` |
| Crossettes | `crossette_red`, `_blue`, `_green`, `_gold`, `_white`, `_amber`, `_violet` |
| Mines | `mine_blue`, `_red`, `_green`, `_gold`, `_white`, `_amber`, `_violet` |
| Aerial strobes | `white_strobe_burst`, `white_aerial_strobe`, `red_`, `blue_`, `green_`, `gold_`, `amber_`, `violet_`, `silver_aerial_strobe` |
| Daytime powder | `daytime_powder_lime`, `_magenta`, `_yellow`, `_orange`, `_red`, `_blue`, `daytime_powder_rainbow` |
| Specials | `multicolor_burst`, `palm_gold`, `horsetail_silver`, `ring_red`, `spinner_gold`, `spider_white`, `diadem_blue`, `salute_white`, `heart_pink`, `double_burst_purple`, `whistler_silver`, `silver_jet`, `mortar_hit`, `flame_projector` |

## RGB Firework Launcher (`firework_rgb_launcher`)

Pick the effect and tint it from the desk. **7-Channel RGB Firework**:

| Ch | Function | Values |
|---|---|---|
| 1 | Fire | as the 3-channel launchers |
| 2 | Tilt | 0° to 180° |
| 3 | Power | 0.7× to 2.6× |
| 4 | Red | default 255 |
| 5 | Green | default 255 |
| 6 | Blue | default 255 |
| 7 | Effect | selects the burst pattern among the presets |

## Pyro Fan (`pyro_fan`)

Ten tubes firing gold comets in a vertical fan.

**3-Channel Pyro Fan (All Tubes)**: 1 Fire for all tubes, 2 Tilt, 3 Power.

**10-Channel Pyro Fan (Per Tube)**: channels 1 to 10 are tubes 1 to 10; each rise from 0 fires that tube. Tilt is fixed and power is full in this mode. Rate 0.5 to 6 shots per second, tubes scheduled round-robin.

## Flame Projector (`flame_projector`)

**2-Channel Flame Projector**: 1 Flame length (0 to 255, also the fire trigger), 2 Head angle.

## Flame Thrower (`flame_thrower`)

**2-Channel Flame Thrower**: 1 Flame, 2 Pan.

## Gold Gerb (`gerb_gold`)

**2-Channel Gerb**: 1 Intensity, 2 Tilt.

## Flow2Jet CO₂ Cannon (`flow2jet`)

**1-Channel Flow2Jet**: 1 Blast. It has no safety arm and fires whenever the channel rises. Also available as a handheld item.

## Confetti Cannon (`confetti_cannon`)

**1-Channel Confetti Cannon**: 1 Fire. A high burst of confetti on trigger. Also available as a handheld item that fires where you look.
## Fixture pages

- [Firework Launcher (all presets)](/fixtures/firework_launcher)
- [RGB Firework Launcher](/fixtures/firework_rgb_launcher)
- [Pyro Fan — 10 Comets](/fixtures/pyro_fan)
- [Flame Projector](/fixtures/flame_projector)
- [Flame Thrower](/fixtures/flame_thrower)
- [Gold Gerb (Ground Fountain)](/fixtures/gerb_gold)
- [Flow2Jet CO₂ Cannon](/fixtures/flow2jet)
- [Confetti Cannon](/fixtures/confetti_cannon)
