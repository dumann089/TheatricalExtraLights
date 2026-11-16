package com.github.dumann089.theatricalextralights.client.gobo;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

final class GoboPixelUnpackState implements AutoCloseable {
    private final int previousBuffer;
    private final int previousAlignment;
    private final int previousRowLength;
    private final int previousSkipRows;
    private final int previousSkipPixels;
    private final int previousImageHeight;
    private final int previousSkipImages;
    private final int previousSwapBytes;
    private final int previousLsbFirst;

    private boolean closed;

    GoboPixelUnpackState() {
        previousBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        previousAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        previousRowLength = GL11.glGetInteger(GL12.GL_UNPACK_ROW_LENGTH);
        previousSkipRows = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_ROWS);
        previousSkipPixels = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_PIXELS);
        previousImageHeight = GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT);
        previousSkipImages = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES);
        previousSwapBytes = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES);
        previousLsbFirst = GL11.glGetInteger(GL11.GL_UNPACK_LSB_FIRST);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL12.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
        GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, GL11.GL_FALSE);
    }

    int getPreviousBuffer() {
        return previousBuffer;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, previousAlignment);
        GL11.glPixelStorei(GL12.GL_UNPACK_ROW_LENGTH, previousRowLength);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_ROWS, previousSkipRows);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_PIXELS, previousSkipPixels);
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, previousImageHeight);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, previousSkipImages);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, previousSwapBytes);
        GL11.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST, previousLsbFirst);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, previousBuffer);
    }
}