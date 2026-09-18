# Multiplayer notes

## Installation

The mod must be on the **server and every client**. DMX reception, patching, personalities, safety states and presets live on the server; beams, gobos, haze and screens are rendered by each client.

## What is synced

- Fixture state (address, universe, network, mode, DMX values, mount transform, laser stop, pyro arm, followspot presets) is stored in the block entity and sent to the players tracking the chunk. Updates are sent as data packets without a chunk rebuild, so beams do not flicker when values change.
- Custom gobo **mappings** are broadcast to all players; the **images** are streamed on demand to players who do not have the file in their own `config/gobos/` folder.
- The **config file** is not synced. Rendering quality, haze and shadows are each player's own choice. Pyro budgets are read on the side that spawns or renders the effects.

## Permissions

- Opening a fixture screen or patching a fixture requires the right to configure its **Theatrical network**. Manage this in Theatrical; Extra Lights refuses the interaction otherwise.
- The followspot console only accepts commands from players within **8 blocks** of it.
- The client command `/tel config` and the **O** key only open the local settings screen; there are no server commands and no permission nodes.

## Files on the server

| Path | Content |
|---|---|
| `config/theatricalextralights.json` | Server-side options (pyro caps, facade universes). |
| `<world>/theatrical_gobos/*.png` | Uploaded custom gobos. |
| `<world>/theatrical_gobos/gobo_mappings.json` | Which wheel slot shows which image. |

Back up the world folder and you keep the gobos.

## Art-Net on a dedicated server

Theatrical receives Art-Net on the machine running the **server**. Point your console at the server's IP (or broadcast on its subnet) and open UDP 6454 in its firewall. Clients do not need Art-Net access.

## Show safety

- Keep pyro fixtures **disarmed** while building; arm them from their screen before the show. The state is per fixture and visible to everyone.
- The laser **emergency stop** is a persisted state: engaged before a break, it stays engaged after a restart until someone releases it.
