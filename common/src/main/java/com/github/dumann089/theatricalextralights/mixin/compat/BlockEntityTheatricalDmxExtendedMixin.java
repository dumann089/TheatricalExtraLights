package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.blockentities.AtomictiltBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.BlinderBaseBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.FlameThrowerBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.LaserBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.StrobeBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Empty mixin — {@link com.github.dumann089.theatricalextralights.mixin.ExtraLightsMixinPlugin}
 * adds {@code dev.imabad.theatrical.api.dmx.DmxFrameExtendedFixture} to these classes when that
 * API exists, without a compile-time hard dependency on older Theatrical builds.
 */
@Mixin({
        LaserBlockEntity.class,
        StrobeBlockEntity.class,
        BlinderBaseBlockEntity.class,
        FlameThrowerBlockEntity.class,
        AtomictiltBlockEntity.class
})
public class BlockEntityTheatricalDmxExtendedMixin {
}
