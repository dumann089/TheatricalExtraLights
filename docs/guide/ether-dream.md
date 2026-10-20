# Ether Dream laser projector (MadMapper)

The **Ether Dream Laser Projector** is a laser fixture that is not driven by DMX. Instead, the game client pretends to be an [Ether Dream](https://ether-dream.com/) laser DAC on your network. Any software that can output to an Ether Dream, such as **MadMapper**, LaserOS, Beyond, CloudLase or QuickShow, sees the client as a real DAC and streams its ILDA frames into the world. The projector block draws that picture with the same haze, beams, sheets and impacts as the DMX lasers.

Use it when you want to design laser content in a dedicated laser tool rather than with DMX channels: logos, text, animated graphics, beat-synced beam shows, or mapping on a stage element.

## Enabling the mode

The virtual DAC is a client feature and is **on by default**. It starts with the game and listens even when no projector is placed, so your laser software can discover it while you build the rig.

To switch it off or on:

- **In game**: open the Extra Lights settings (the *Open Configuration Menu* key, `O` by default), go to the *Laser* tab and use the **Ether Dream DAC** toggle. The change applies immediately: turning it off closes the ports and turning it on restarts the DAC.
- **In the config file**: set `etherDreamEnabled` to `false`, see [Config file](/guide/config-file).

When the DAC is off, projector blocks stay dark and their screen shows *offline*.

## Network ports

| Direction | Protocol | Port | Purpose |
| --- | --- | --- | --- |
| Client → LAN | UDP broadcast | 7654 | Identity broadcast, once per second. This is how MadMapper finds the DAC. |
| Controller → client | TCP | 7765 | Control connection. One controller at a time. |

Both ports can be changed in the config. If the game logs `TCP bind failed`, another program already uses port 7765; either close it or change `etherDreamTcpPort` and point your laser software at the new port manually.

The client also unicasts the broadcast to itself, so **MadMapper running on the same PC as Minecraft works without extra setup**. Windows Firewall may ask to allow Java or the launcher on private networks the first time; accept it or MadMapper on another machine will not see the DAC.

## MadMapper setup

1. Start Minecraft with the mod. Check the log for `[EtherDream] listening on 0.0.0.0:7765` and `[EtherDream] advertising on UDP 7654`.
2. In MadMapper, open **Preferences → Lasers** (or the laser output panel) and add a laser output of type **Ether Dream**. The virtual DAC appears in the device list within a couple of seconds, named after the MAC address in the config (`02:00:00:ED:01:00` by default).
3. Create a **Laser Fixture** in MadMapper and assign it to that output. Keep the point rate at or below 30 kpps to start; the DAC accepts up to `etherDreamMaxPointRate`.
4. Draw or import your content (lines, text, shapes) in the laser fixture and press the output button. The projector screen in game switches from *Idle* to *Playing* and the picture appears in the world.

MadMapper's laser fixture settings, such as scan speed, blanking delays and colour correction, all pass through unchanged. Blanked points (very dark colours, under `laserDacBlankThreshold`) are not drawn.

## Placing and aiming the projector

Craft or take the **Ether Dream Laser Projector** from the creative tab and hang it like any other Extra Lights fixture: on a truss, a brace bar, or the floor with the wrench mount options. It has pan and tilt like a moving head, but these are set from its screen rather than from DMX.

Right-click the projector to open its screen:

- **State** and **Link**: DAC status (*Offline*, *Idle*, *Prepared*, *Playing*, *E-Stop*) and whether a controller is connected.
- **Buffer** and **PPS**: what the controller is sending, useful to confirm MadMapper is streaming.
- **Scan angle**: half-angle of the projection cone in degrees, 5 to 60. A real 30 kpps galvo set is around 30°, which is the default. Lower it for a tighter picture at long range.
- **Scale**: multiplies the scan angle and pushes the mid-air picture plane further away. Use it to fit the drawing on a backdrop without changing the MadMapper output.
- **Save** writes the settings to the block; they are stored with the world and synced to every player.

Beam length and pass-through blocks follow the same config as the DMX lasers, `laserBeamLength` and `laserPassThroughBlocks`, see [Lasers](/guide/lasers).

## Several projectors

Every projector in the world shows the same DAC picture by default: one MadMapper output, as many projectors as you like, all drawing the same frame from their own position and angle. This is handy for a symmetric rig. Independent content per projector needs one DAC per Minecraft client, so in multiplayer each player's own client can host a DAC for the projectors they operate.

## Rendering options

The projector uses the realistic laser look. Ray count, persistence window and halo size are in the config (`laserDacMaxRays`, `laserDacPersistenceMs`, `laserDacHazeRadius`), see [Config file](/guide/config-file). If the drawing flickers, raise `laserDacPersistenceMs` so the window covers a full MadMapper frame at your frame rate.

## Troubleshooting

- **MadMapper does not list the DAC**: make sure `etherDreamEnabled` is on, check the log for the two `[EtherDream]` lines, and allow Java through the firewall. On a multi-homed PC, set `etherDreamBindAddress` to the address of the network card MadMapper uses.
- **DAC listed but the picture never appears**: the projector screen should show *Playing*. If it stays *Prepared*, the controller connected but has not started output; press play in MadMapper. If it shows *E-Stop*, the controller sent an emergency stop; clear it in MadMapper.
- **Only one controller can connect**: a second connection on port 7765 is refused while the first one is open. Close the other software or use a different client.
- **Picture drawn but nothing in the air**: like the DMX lasers, beams are only visible through haze. Raise the *Haze* slider in the *Laser* settings tab.
