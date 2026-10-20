package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.blockentities.FlameProjectorBlockEntity;
import com.github.dumann089.theatricalextralights.client.particle.FlameThrowerParticleSpawner;
import com.github.dumann089.theatricalextralights.client.render.pyro.FlameVolumeRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Flammes du Flame Projector : la colonne part du dessus de l'appareil vers le haut, avec les
 * particules de flamme du lance-flammes (memes textures, meme bloom). Canal 1 : declenchement et
 * densite ; canal 2 : hauteur. Le serveur n'envoie plus de particules, il ne garde que le son.
 * Quand les flammes en volume sont actives, c'est {@link FlameVolumeRenderer} qui dessine.
 */
public final class FlameProjectorClientEffects {

    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    /** Hauteur relative a canal 2 = 0 et 255 (1 = lance-flammes plein, environ 6 blocs). */
    private static final float MIN_REACH = 0.25f;
    private static final float MAX_REACH = 1.0f;

    private FlameProjectorClientEffects() {
    }

    public static void tick(FlameProjectorBlockEntity be) {
        if (!(be.getLevel() instanceof ClientLevel level) || be.getIntensity() <= 0
                || FlameVolumeRenderer.isAvailable()) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        float lengthFactor = Mth.clamp(be.getFlameLengthRaw(), 0, 255) / 255f;
        float reach = Mth.lerp(lengthFactor, MIN_REACH, MAX_REACH);
        FlameThrowerParticleSpawner.spawnColumn(level,
                pos.getX() + 0.5, pos.getY() + 10.0 / 16.0, pos.getZ() + 0.5,
                UP, be.getIntensity(), reach, level.random);
    }
}
