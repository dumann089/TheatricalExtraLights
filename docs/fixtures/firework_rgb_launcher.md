# RGB Firework Launcher

`theatricalextralights:firework_rgb_launcher` · family: [Pyro & fireworks](/fixtures/pyro)

A launcher whose effect and colour are chosen from the desk.

- Carries a **safety arm**: disarmed in its screen, it reads DMX but never fires. See [Pyro & safety arm](/guide/pyro).
- Found in the **Theatrical: Pyro** creative tab.

## 7-Channel RGB Firework (7 ch)

| Ch | Function | Values |
|---|---|---|
| 1 | Fire | 0 idle, rising edge = one shot, 2 to 255 = 1 to 10 shots/s |
| 2 | Tilt | 0 to 255 = 0° to 180° |
| 3 | Power | 0 to 255 = 0.7× to 2.6× |
| 4 | Red | 0 to 255 (default 255) |
| 5 | Green | 0 to 255 (default 255) |
| 6 | Blue | 0 to 255 (default 255) |
| 7 | Effect | 0 to 255 |

### Channel by channel

**1 · Fire** — Trigger and rate. 0 is idle. Any rise from 0 fires one shot; a value of 1 stays single-shot, so bump the channel to fire once. From 2 to 255 the launcher fires continuously, from 1 shot per second at 2 to 10 per second at 255. Nothing fires while the machine is **disarmed** in its screen.

**2 · Tilt** — Launch pitch. 0 is horizontal, about 128 is straight up (90°), 255 is horizontal the other way. Aim launchers before the show; the shells keep the angle for their whole flight.

**3 · Power** — Launch power, a multiplier on the shell speed. Low values keep the burst close to the stage, high values send it high and far.

**4 · Red** — Red tint of the shell.

**5 · Green** — Green tint.

**6 · Blue** — Blue tint.

**7 · Effect** — Selects the burst pattern among the launcher presets, in the order of the preset list; 0 is the first.
