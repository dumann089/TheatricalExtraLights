# Video Switcher

A desk block that cuts live between cameras. It only shows what a camera is **transmitting** (where it points, its zoom and exposure): you cannot control a camera from the desk.

1. Place the **Video Switcher** and right click it. The menu lists every camera in the world, nearest first.
2. Click a camera, then press the key you want for it (Delete clears it). Pick the keys for the two modes too: **hard cut** and **fade**, and the fade time.
3. **Operate** puts your view on the program output. Press a camera's key to switch to it. **Y** hides / shows the on-screen text (only the camera picture stays). Esc leaves the desk.

Modes:
- **Cut**: the new camera appears instantly.
- **Fade**: a dissolve from one camera to the other. Both cameras are live: the picture of the camera that leaves fades out (opacity) over the live picture of the one that comes in (while it lasts the world is drawn twice, so expect a lower frame rate during a fade). It does not work with the Fabulous graphics setting. The fade time is how long the dissolve takes. In fade mode the arrow keys change it, 1 second at a time (1 to 10 s).

The settings are saved in the block. The cameras must be loaded near you to be seen (a camera far outside your render distance shows NO SIGNAL).

Aspect ratio: each camera has its own (set in its Shift + right click menu: window, 16:9 or 4:3), and the desk can force 16:9 or 4:3 for everything it shows (the **Aspect ratio** button in the desk menu). The picture is cropped with black bars.

Fade timing: when you fade to a camera, the new camera's world is first loaded (chunks, lights, beams) while the old camera stays on screen, and the dissolve starts once it is ready (at most 4 seconds), so beams and lights are already on when the new picture appears.

Smooth switching: while the desk is operating, the world around the cameras is loaded once, around the centre of the bound cameras, and kept there (the usual occlusion culling is switched off). Switching cameras no longer reloads anything, so beams, lights and objects are already there when a camera comes on air. When you press **Operate** the world reloads once (a "Loading the cameras' world..." note shows until it is done); leaving the desk reloads it around you again. Cameras should be within your render distance of that centre, and of you, to be loaded at all.

Clean pictures: the person riding the camera on air (and the owner of a shoulder camera) is not drawn, so no silhouette appears in the image. The fade starts the moment you press the key.

The cameras' world is loaded once and stays loaded, even after you leave the desk, so operating again does not reload anything. It is released when you walk more than 64 blocks away from the stage, change dimension or leave the world (and then the world around you reloads as usual). While it is held, the view distance on the side of you away from the stage can be shorter. The vanilla hotbar and other game HUD are hidden while you operate any camera or the desk; **Y** hides only the camera HUD, and the aspect-ratio bars stay.
