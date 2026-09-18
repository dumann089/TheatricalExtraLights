# Quick start

Five minutes from an empty world to a moving head that follows your desk.

## 1. Set up the network

Extra Lights uses Theatrical's networking, so the first steps are Theatrical's.

1. Place an **Art-Net Interface** (Theatrical) and right-click it. Create or select a **network** and set the universe your desk sends on.
2. In your lighting software, output Art-Net to the Minecraft machine's IP (or broadcast). Theatrical listens on the standard Art-Net port 6454.

::: tip
If nothing reacts later, check the Art-Net Interface first: wrong universe or a firewall blocking UDP 6454 are the two usual causes.
:::

## 2. Place a fixture

Open the creative inventory, tab **Theatrical: Extra Lights**, and place a gobo moving head, for example the **Iris 700 Spot (Gobos)**. Fixtures can stand on the floor or hang from a truss: place them against the underside of a truss block and they flip automatically.

![Moving head hung under a truss](/images/quickstart-place.png)

## 3. Patch it

Right-click the fixture to open its [config screen](/guide/config-screen).

1. **Start address** and **Network Universe**: the DMX address and universe your desk uses for this light.
2. **Mode**: pick the personality. Start with **10-Channel Mode**; switch to **29ch - Profile 16bit** when you want the full feature set.
3. **Network**: choose the network you created on the Art-Net Interface.
4. **Save**.

The footprint line shows the channel range and warns if another fixture on the same network overlaps it.

## 4. Drive it from the desk

Patch the same fixture type on your console. For grandMA2 use the [ready-made fixture files](/guide/grandma2); for other desks build a generic fixture from the [channel table](/fixtures/gobo-heads).

Push the dimmer: the beam appears. In 10-channel mode:

| Channel | Function |
|---|---|
| 1 | Dimmer |
| 2 to 4 | Red, Green, Blue |
| 5 | Focus |
| 6 | Pan |
| 7 | Tilt |
| 8 | Gobo wheel |
| 9 | Zoom |
| 10 | Gobo rotation |

## 5. Next steps

- Turn on the haze and shadows in [Rendering & performance](/guide/rendering).
- Switch to the 29-channel Profile mode for shutter, prism, animation wheel and framing blades: [Gobo heads & personalities](/guide/gobo-heads).
- Add your own gobo images: [Custom gobos](/guide/custom-gobos).
- Build a followspot position: [Followspot console](/guide/followspot).
