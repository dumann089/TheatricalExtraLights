package com.github.dumann089.theatricalextralights.client.render.pyro;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Copie de l'image courante pour la distorsion de chaleur, refaite a la demande. */
public final class SceneColorCopy {

    private static TextureTarget copy;

    private SceneColorCopy() {
    }

    /** Copie la cible principale telle qu'elle est maintenant ; renvoie l'id de texture, 0 si impossible. */
    public static int capture() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) {
            return 0;
        }
        if (copy == null || copy.width != main.width || copy.height != main.height) {
            if (copy != null) {
                copy.destroyBuffers();
            }
            copy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            copy.setFilterMode(GL11.GL_LINEAR);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
        return copy.getColorTextureId();
    }
}
