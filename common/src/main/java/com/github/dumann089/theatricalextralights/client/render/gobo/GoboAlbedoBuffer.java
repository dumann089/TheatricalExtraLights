
package com.github.dumann089.theatricalextralights.client.render.gobo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import dev.imabad.theatrical.compat.ModCompat;

public final class GoboAlbedoBuffer {
    private static int textureId = -1;
    private static int width = -1;
    private static int height = -1;
    private static int framebufferId = -1;
    private static boolean ready;
    private static int clearFramebufferId = -1;

    private static final FloatBuffer CLEAR_COLOR = BufferUtils.createFloatBuffer(4);

    private static final int[] NO_OP = {-1};

    private static final ThreadLocal<Deque<int[]>> DRAW_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private GoboAlbedoBuffer() {
    }

    public static void beginFrame() {
        if (!ModCompat.SHIMMER) {
            ready = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();

        if (main == null || main.width <= 0 || main.height <= 0) {
            ready = false;
            return;
        }

        if (textureId == -1) {
            textureId = TextureUtil.generateTextureId();
        }

        if (width != main.width || height != main.height) {
            TextureUtil.prepareImage(textureId, main.width, main.height);

            GlStateManager._bindTexture(textureId);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            width = main.width;
            height = main.height;
        }

        int previousFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);

        GL30.glFramebufferTexture2D(
                GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT2,
                GL11.GL_TEXTURE_2D,
                textureId,
                0
        );

        int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glFramebufferTexture2D(
                    GL30.GL_DRAW_FRAMEBUFFER,
                    GL30.GL_COLOR_ATTACHMENT2,
                    GL11.GL_TEXTURE_2D,
                    0,
                    0
            );

            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFramebuffer);
            ready = false;
            return;
        }

// Limpiar nuestra textura usando un framebuffer independiente.
        if (clearFramebufferId == -1) {
            clearFramebufferId = GL30.glGenFramebuffers();
        }

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, clearFramebufferId);

        GL30.glFramebufferTexture2D(
                GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                textureId,
                0
        );

        GL20.glDrawBuffers(new int[]{
                GL30.GL_COLOR_ATTACHMENT0
        });

        int clearStatus = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);

        if (clearStatus == GL30.GL_FRAMEBUFFER_COMPLETE) {
            CLEAR_COLOR.clear();
            CLEAR_COLOR.put(0.0f);
            CLEAR_COLOR.put(0.0f);
            CLEAR_COLOR.put(0.0f);
            CLEAR_COLOR.put(0.0f);
            CLEAR_COLOR.flip();

            GL30.glClearBufferfv(GL11.GL_COLOR, 0, CLEAR_COLOR);

            CLEAR_COLOR.clear();
        }

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFramebuffer);

        if (clearStatus != GL30.GL_FRAMEBUFFER_COMPLETE) {
            ready = false;
            return;
        }

        framebufferId = main.frameBufferId;
        ready = true;
    }

    public static boolean isReady() {
        return ready && textureId != -1;
    }

    public static int getTextureId() {
        return isReady() ? textureId : 0;
    }

    private static boolean isMainFramebufferBound() {
        if (!isReady()) {
            return false;
        }

        return GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                == framebufferId;
    }

    public static void enableChunkWrite() {
        if (!isMainFramebufferBound()) {
            return;
        }

        int draw0 = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0);
        int draw1 = GL11.glGetInteger(GL20.GL_DRAW_BUFFER1);

        GL20.glDrawBuffers(new int[]{
                draw0,
                draw1,
                GL30.GL_COLOR_ATTACHMENT2
        });
    }

    public static void disableChunkWrite() {
        if (!isMainFramebufferBound()) {
            return;
        }

        GL20.glDrawBuffers(new int[]{
                GL30.GL_COLOR_ATTACHMENT0
        });
    }

    public static void pushMrtWrite() {
        if (!isMainFramebufferBound()) {
            DRAW_STACK.get().push(NO_OP);
            return;
        }

        int draw0 = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0);
        int draw1 = GL11.glGetInteger(GL20.GL_DRAW_BUFFER1);
        int draw2 = GL11.glGetInteger(GL20.GL_DRAW_BUFFER2);

        DRAW_STACK.get().push(new int[]{
                draw0,
                draw1,
                draw2
        });

        if (draw2 != GL30.GL_COLOR_ATTACHMENT2) {
            GL20.glDrawBuffers(new int[]{
                    draw0,
                    draw1,
                    GL30.GL_COLOR_ATTACHMENT2
            });
        }
    }

    public static void popMrtWrite() {
        Deque<int[]> stack = DRAW_STACK.get();

        if (stack.isEmpty()) {
            return;
        }

        int[] previous = stack.pop();

        if (previous == NO_OP) {
            return;
        }

        if (!isMainFramebufferBound()) {
            return;
        }

        int draw0 = previous[0];
        int draw1 = previous[1];
        int draw2 = previous[2];

        if (draw2 != GL11.GL_NONE) {
            GL20.glDrawBuffers(new int[]{
                    draw0,
                    draw1,
                    draw2
            });
        } else if (draw1 != GL11.GL_NONE) {
            GL20.glDrawBuffers(new int[]{
                    draw0,
                    draw1
            });
        } else {
            GL20.glDrawBuffers(new int[]{
                    draw0
            });
        }

        if (stack.isEmpty()) {
            DRAW_STACK.remove();
        }
    }
}