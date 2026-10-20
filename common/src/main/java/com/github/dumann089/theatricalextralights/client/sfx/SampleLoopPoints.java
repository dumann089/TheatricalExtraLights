package com.github.dumann089.theatricalextralights.client.sfx;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.mixin.client.sfx.SoundBufferAccessor;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Points de boucle OpenAL (extension AL_SOFT_loop_points) sur un echantillon : il joue depuis le
 * debut, puis boucle entre deux instants au lieu de reprendre a zero. Le jet CO2 joue ainsi
 * l'attaque de l'ouverture de vanne une fois, puis le souffle en boucle sans couture.
 *
 * <p>OpenAL refuse les points sur un tampon deja attache a une source : le mixin sur
 * {@code SoundBuffer.getAlBuffer} appelle {@link #onAlBufferReady} a la creation du tampon, avant
 * tout attachement, vanilla ou d'un autre mod son.
 */
@Environment(EnvType.CLIENT)
public final class SampleLoopPoints {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** AL_LOOP_POINTS_SOFT, OpenAL Soft. */
    private static final int AL_LOOP_POINTS_SOFT = 0x2015;

    private record Points(float startSeconds, float endSeconds) {
    }

    private static final Map<ResourceLocation, Points> BY_FILE = new ConcurrentHashMap<>();
    private static final Map<SoundBuffer, Points> LOADED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Integer> APPLIED = ConcurrentHashMap.newKeySet();

    static {
        // Jet CO2 : attaque 0.00-0.10 s une fois, souffle 0.10-0.60 s en boucle.
        register(new ResourceLocation(TheatricalExtraLights.MOD_ID, "sounds/block/flow2jet.ogg"), 0.10f, 0.60f);
    }

    private SampleLoopPoints() {
    }

    /** @param soundFile chemin complet de l'echantillon, tel que le charge la bibliotheque ({@code sounds/.../x.ogg}). */
    public static void register(ResourceLocation soundFile, float startSeconds, float endSeconds) {
        BY_FILE.put(soundFile, new Points(startSeconds, endSeconds));
    }

    public static boolean hasPoints(ResourceLocation soundFile) {
        return BY_FILE.containsKey(soundFile);
    }

    /** La bibliotheque vient de decoder un echantillon : on retient son tampon s'il a des points. */
    public static void onBufferLoaded(ResourceLocation soundFile, SoundBuffer buffer) {
        Points points = BY_FILE.get(soundFile);
        if (points != null && buffer != null) {
            LOADED.put(buffer, points);
        }
    }

    /** Le tampon OpenAL vient d'etre cree pour cet echantillon : pose les points, une seule fois. */
    public static void onAlBufferReady(SoundBuffer buffer, int alBuffer) {
        Points points = LOADED.get(buffer);
        if (points == null || !APPLIED.add(alBuffer)) {
            return;
        }
        try {
            AudioFormat format = ((SoundBufferAccessor) buffer).tel$getFormat();
            float rate = format.getSampleRate();
            int channels = Math.max(1, AL10.alGetBufferi(alBuffer, AL10.AL_CHANNELS));
            int bits = Math.max(8, AL10.alGetBufferi(alBuffer, AL10.AL_BITS));
            int totalFrames = AL10.alGetBufferi(alBuffer, AL10.AL_SIZE) / (channels * bits / 8);
            int start = Math.max(0, Math.round(points.startSeconds() * rate));
            int end = Math.min(totalFrames, Math.round(points.endSeconds() * rate));
            if (end <= start + 1) {
                return;
            }
            AL10.alGetError();
            AL11.alBufferiv(alBuffer, AL_LOOP_POINTS_SOFT, new int[]{start, end});
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                LOGGER.warn("Loop points refused by OpenAL (error {}), the sample loops whole", error);
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not set loop points, the sample loops whole", t);
        }
    }
}
