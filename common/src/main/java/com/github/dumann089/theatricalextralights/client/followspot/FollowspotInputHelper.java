package com.github.dumann089.theatricalextralights.client.followspot;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FollowspotInputHelper {

    private FollowspotInputHelper() {
    }

    /**
     * Raw GLFW key/mouse state — {@link KeyMapping#isDown()} is unreliable while a Screen is open,
     * which broke Followspot Console pan/tilt (WASD) while sliders still worked.
     */
    public static boolean isKeyDown(KeyMapping mapping) {
        if (mapping == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        // 1.20.1 KeyMapping has no getKey(); resolve via saveString (e.g. key.keyboard.w)
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    public static boolean isEscapeDown(Minecraft minecraft) {
        return minecraft != null
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE);
    }
}
