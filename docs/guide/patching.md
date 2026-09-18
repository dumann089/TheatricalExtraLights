# Patching (DMX & Art-Net)

Extra Lights fixtures are patched exactly like Theatrical fixtures. This page recaps the concepts and the two ways to patch.

## Concepts

| Term | Meaning |
|---|---|
| **Network** | A Theatrical network groups fixtures and their DMX sources. Created on an Art-Net Interface or a desk block. Fixtures only receive data from sources on their own network. |
| **Universe** | 512 DMX channels. Your console outputs one or more Art-Net universes; each fixture listens to one. |
| **Start address** | First channel of the fixture inside its universe, 1 to 512. |
| **Footprint** | Number of channels the fixture occupies, set by its personality (mode). A 29-channel head at address 500 does not fit in a universe: the last valid address is 484. |
| **Personality / mode** | Alternative channel layouts of the same fixture. Changing the mode changes the footprint. |

Pan and tilt on moving heads are decoded from DMX as −180° to 180° and −225° to 45° respectively; the followspot uses a narrower −90° to 90° and −45° to 45°.

## Patching from the fixture screen

Right-click a fixture, fill **Start address** and **Network Universe**, pick a **Mode** and a **Network**, then **Save**. The screen shows the resulting footprint as `Universe 1 · channels 1-29 (29 ch)` and checks two things live:

- **Overflow**: if the footprint does not fit before channel 512, it proposes the next universe at address 1.
- **Overlap**: if another fixture on the same network already uses part of the range, an orange line names it and the overlapping channels. It is a warning, not a block: overlapping on purpose (two fixtures on one address) is a valid choice.

![Footprint and overlap lines](/images/patch-footprint.png)

## Patching with the Configuration Card

Theatrical's **Configuration Card** patches fixtures in sequence without opening a screen. Set the card's network, universe and next address, then right-click each fixture in the order you want them addressed. Extra Lights adds two behaviours:

- when the next fixture does not fit in the remaining channels of the universe, the card **jumps to the next universe at address 1** and says so in chat;
- chat feedback names the fixture, network, channel range and the card's next address, so you can check the rig from the log.

## Multiple universes

Large rigs and the LED Facade span several universes. Patch per universe on the desk; in the mod, each fixture's screen shows its universe and the facade screen shows how many universes it needs.

## Checklist when a fixture does not react

1. Same **network** on the Art-Net Interface and on the fixture.
2. Same **universe** on the console output, the interface and the fixture. Consoles count universes from 0 or from 1 depending on the brand; Theatrical follows the Art-Net numbering shown on the interface.
3. Same **start address** and the same **mode** on both sides.
4. Is the desk actually sending? Most consoles need a fixture to be *patched and selected* before its parameters output.
5. On the gobo heads in Profile mode, open the fixture screen: the **Profile card** shows the values received live. Shutter closed or dimmer at 0 explains a dark fixture.
