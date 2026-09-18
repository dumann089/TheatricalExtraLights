# Firework Launcher (all presets)

`theatricalextralights:firework_launcher` · family: [Pyro & fireworks](/fixtures/pyro)

About 150 launcher blocks, one per effect (comets, peonies, willows, chrysanthemums, crossettes, mines, aerial strobes, daytime powder, special shells). They all share one personality; the block decides the effect.

- Carries a **safety arm**: disarmed in its screen, it reads DMX but never fires. See [Pyro & safety arm](/guide/pyro).
- Found in the **Theatrical: Pyro** creative tab.
- Registry ids are `theatricalextralights:firework_<effect>`; the full list is in [Pyro & fireworks](/fixtures/pyro).
- Shots get a small random horizontal drift; the server caps concurrent rockets and launches per tick (config `maxConcurrentRockets`).

## 3-Channel Firework (3 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Fire | 0 idle, rising edge = one shot, 2 to 255 = 1 to 10 shots/s |
| 2 | Tilt | 0 to 255 = 0° to 180° |
| 3 | Power | 0 to 255 = 0.7× to 2.6× |

### Channel by channel

**1 · Fire** — Trigger and rate. 0 is idle. Any rise from 0 fires one shot; a value of 1 stays single-shot, so bump the channel to fire once. From 2 to 255 the launcher fires continuously, from 1 shot per second at 2 to 10 per second at 255. Nothing fires while the machine is **disarmed** in its screen.

**2 · Tilt** — Launch pitch. 0 is horizontal, about 128 is straight up (90°), 255 is horizontal the other way. Aim launchers before the show; the shells keep the angle for their whole flight.

**3 · Power** — Launch power, a multiplier on the shell speed. Low values keep the burst close to the stage, high values send it high and far.
