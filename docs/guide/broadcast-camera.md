# Broadcast Camera

A TV shoulder camera on a wheeled tripod. It is not a DMX fixture: it is operated by hand, like the followspot operator mode. It is an entity, so it can roll around.

## Use

1. Hold the **Broadcast Camera** item (creative tab *Theatrical: Extra Lights*) and right-click the ground. The lens points away from you, so you stand behind it.
2. **Right-click** the camera. You climb on it and your view moves to the lens.
3. Control it:

| Input | Action |
|-------|--------|
| Mouse | Pan / tilt the head. The head follows the mouse smoothly, like the Minecraft cinematic camera, and moves slower the more you zoom in |
| Mouse wheel | Zoom, 1x to 100x, 0.1x per notch, with a slow ease-in/out |
| `.` / `,` (default) | Zoom in / out while held. **Rebindable** in *Options > Controls > Theatrical Extra Lights* |
| W A S D | Roll the dolly forward / back / left / right (relative to the way the camera was placed) |
| Z | Toggle **quick zoom mode**: each wheel notch zooms 1x instead of 0.1x, the zoom keys are about three times faster and the zoom reacts faster. It never changes the current zoom by itself |
| Up / Down | Exposure in 1/3 stop steps (hold to repeat) |
| Left | Reset exposure |
| Y | Hide / show the HUD |
| U | Free the mouse (like the chat); press again to capture it |
| R | **Recordings** (all camera variants). Opens a menu to record a new take, play a saved one (optionally looping) or delete it. While recording or playing, R stops it (a recording is saved when stopped or when you leave the camera). A take stores everything: pan / tilt, zoom, exposure, dolly movement, boom swing / lift (jib) and tripod height / boom length. While a take plays your own controls are ignored. Shift or Esc exit the camera, and **the take keeps playing on the camera after you leave** (pan / tilt, height, boom and dolly movement, visible to everyone; zoom and exposure only exist in the operator's view, so they stop). Getting on the camera again takes control and stops it. Takes are saved in `config/theatricalextralights/camera_recordings/` |
| Esc (or Shift) | Get off the camera |

## Movement

The dolly never moves diagonally and does not depend on the mouse: W/A/S/D are relative to the way the camera was placed, along one of the four axes. If you hold two keys, only the last one pressed counts.

## Tripod height

**Shift + right-click** the camera (even while somebody is on it) to open a menu with a slider for the tripod height, plus Reset and Done. The column is telescopic and the hitbox follows it; it will not grow into a ceiling.

## Notes

- Exposure is a screen post-process, so it keeps working when the HUD is hidden. It resets when you get off the camera.
- A very small shake appears only near maximum zoom.
- Tilt is limited to 60 degrees up and 40 degrees down. Pan is free.
- Other players see the head move and the tally lights turn bright red while somebody is on it.
- Hit the camera to pick it up (it drops as an item in survival).

**Camera menu** (Shift + right click on the camera): besides the height you can give the camera a **name** (shown in the Animation Synchronizer and Video Switcher lists) and choose the **aspect ratio** of its picture (window, 16:9 or 4:3).
