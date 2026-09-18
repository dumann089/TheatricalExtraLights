# Custom gobos

Any slot of a gobo wheel can show your own image: a logo, a text, a breakup pattern. The image is uploaded to the server world and distributed to every player automatically.

![Custom gobo screen](/images/custom-gobo-screen.png)

## 1. Prepare the image

- **Format**: PNG. Only `.png` files are listed.
- **Content**: a gobo is a mask. White (or bright) areas let light through, black areas block it. Colour is kept and tints the light, so a plain black-and-white image gives the cleanest result.
- **Size**: square, power-of-two sizes work best (256×256 or 512×512). Keep the file small; a few dozen kilobytes is plenty for a mask and keeps the network transfer quick.

## 2. Put it in the gobos folder

Copy the file to `config/gobos/` inside your Minecraft instance folder (next to `config/theatricalextralights.json`). The folder is created on first launch. No restart is needed: the screen lists the folder when you open it.

## 3. Assign it to a wheel slot

1. Right-click a gobo head and press **Custom Gobos**.
2. On the left, pick the PNG. The list is paginated; use the arrows or the mouse wheel.
3. On the right, pick the wheel slot to replace. Slots that already carry a custom image show an accent dot; the preview and the status line tell you what the slot currently holds.
4. **Save**.

**Restore original gobo** removes the custom image from the selected slot on the next save.

## Scope of a mapping

Gobo wheels are shared *libraries*: the Iris 700, Pro Spot, Mini Spot and others each have their own wheel, and several fixtures use the same wheel. A custom gobo replaces a slot of the **wheel**, so every fixture using that wheel shows it. This matches a real rig, where a custom gobo is cut once and fitted in all the units of the same type.

## How it travels

- The client uploads the PNG to the server in chunks. The server stores it in the world folder under `theatrical_gobos/`, records the mapping in `theatrical_gobos/gobo_mappings.json`, and broadcasts the new mapping to all players.
- A player who does not have the file locally requests it from the server, which streams it back. The texture lives in memory and is dropped on disconnect.
- Mappings are loaded when the server starts and sent to each player on join, so they are per world and survive restarts. Back up the world folder and you back up the gobos.

## Tips

- Keep the wheel's slot 0 as an open gobo unless you really want to lose the plain beam.
- The animation wheel of the Profile mode is applied on top of the gobo: a custom window gobo with the *flames* animation behind it is a classic.
- In the fixture screen, the **Gobo & framing shutters** card previews your image with the blades and the prism.
