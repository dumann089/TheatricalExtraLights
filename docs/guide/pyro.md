# Pyro & safety arm

Everything pyrotechnic lives in the **Theatrical Pyro** creative tab: fifty-plus firework launchers, the Pyro Fan, the RGB launcher, confetti cannons, gerbs, flame projectors and the flame thrower. They are DMX fixtures: patch them with the Configuration Card or their own screen and fire them from the desk.

![Pyro screen with arm key](/images/pyro-screen.png)

## Pyro screen

Right-click a pyro fixture to open its screen. It has the patch section (address, universe, network) and a **safety arm** control.

- **Armed** (default): the fixture fires when DMX tells it to.
- **Disarmed**: DMX is still received and shown, but intensity is forced to zero and nothing fires or burns. Use it while building the rig or between shows.

The arm state is saved with the world and synced to every player. It is per fixture, so you can keep the finale rack disarmed while testing the front gerbs.

## Firework launchers

Most launchers use a 3-channel personality:

| Channel | Role |
|---|---|
| Intensity | Fire rate. 0 is off, values from 2 start firing, higher is faster. |
| Tilt | Launch angle, 0° horizontal to 180° straight up. |
| Focus | Launch power: how high and how far each shell travels. |

Each launcher is a fixed effect: comets, long comets, peonies, willows, chrysanthemums, crossettes, mines, special shells (palm, ring, spinner, horsetail, spider, diadem, salute, heart, double burst, multicolour, whistler), strobes and daytime powder. The **RGB Firework Launcher** lets you pick the effect and tint it from DMX; the **Pyro Fan** fires ten tubes in a vertical fan, individually addressable in its 10-channel mode. See the [pyro reference](/fixtures/pyro) for every channel.

## Flames, gerbs, confetti

![Gold confetti burst on a stage](/images/pyro-confetti.png)

- **Flame projector**: continuous flame column driven by intensity.
- **Flame thrower**: directed jet with pan.
- **Gold gerb**: stage fountain.
- **Confetti cannon**: one channel, a burst on trigger.

## Tips

- Aim launchers with tilt before the show and keep focus for dynamics.
- Daytime powder and the rainbow fan are made for bright maps: little dynamic light, coloured plumes.
- Long comets stop at their apex and fade with a short drop; mines burst at the launcher.
- Heavy shows are throttled server-side (maximum concurrent rockets and launches per tick) to stay stable. The related client options are in the [config file](/guide/config-file): `fireworkRenderDistance`, `fireworkDynamicLightEnabled`, `fireworkSmokeEnabled`, `fireworkSmokeBudgetPerTick`, `fireworkSmokeSpawnInterval`.
