# Troubleshooting & FAQ

## Nothing lights up

1. Check the chain: console output universe → Art-Net Interface network and universe → fixture network, universe, address, mode. See the [patching checklist](/guide/patching#checklist-when-a-fixture-does-not-react).
2. Open the fixture screen. On a gobo head in 29-channel mode, the **Profile card** shows what arrives: shutter closed (channel 1 at 0) or dimmer at 0 means the desk is not sending what you think.
3. On pyro fixtures, check that the machine is **armed**; on lasers, that the **emergency stop** is released.
4. Sneak + right-click the fixture to toggle Theatrical's debug overlay and read its raw state.

## The fixture reacts but the beam is invisible

- **Volumetric beam** may be off: settings screen, General tab.
- With a shader pack (Iris / Oculus) some packs hide additive geometry; try without the pack to confirm.
- The beam only exists when intensity is above 0 *and* the shutter is open in Profile mode.

## The beam flickers when I move a fader

Fixed in 1.4.11. Update the mod on server and clients.

## The strobe looks slow at high values

The strobe runs in whole game ticks; 10 Hz is the maximum and is reached around 80 % of the channel. Values above that stay at 10 Hz.

## grandMA2 shows errors on the fixture type

The type in your show is an old import. MA never replaces an existing type: delete it in Fixture Types, then import the file again. See [grandMA2 fixture files](/guide/grandma2).

## My custom gobo does not appear for other players

Mappings are broadcast, images are streamed on demand. Make sure the other player has reconnected after you saved the gobo, or give them the PNG for their own `config/gobos/` folder. Very large images take longer to arrive.

## Which blocks stop lasers?

Every block except those listed in `laserPassThroughBlocks` in the [config file](/guide/config-file) and the blocks of Theatrical and Extra Lights.

## Performance is low with many fixtures

Lower **Quality**, reduce **Max beams per frame**, and try `beamShadows: false`. See [Rendering & performance](/guide/rendering#performance-tips).

## The game crashed with Shimmer and a Profile head

Fixed in 1.4.11 (a closed shutter registered a light without a position). Update.

## Where is the settings screen?

Press **O**, or type `/tel config`, or use Mod Menu (Fabric) / the mod list config button (Forge).

## Can I use another console than grandMA?

Yes. Any Art-Net console works. Build a generic fixture from the [channel tables](/fixtures/overview).

## Does it work on 1.20.2, 1.21 or NeoForge?

Not in this release. Extra Lights targets Minecraft 1.20.1 on Fabric and Forge, following Theatrical.

## Reporting a bug

Post on the [Theatrical Discord](https://discord.gg/7qMs5d6) or open an issue on [GitHub](https://github.com/dumann089/TheatricalExtraLights/issues) with the mod versions (Theatrical and Extra Lights), your loader, the fixture and mode, and the crash report or `latest.log` if any.
