package com.github.dumann089.theatricalextralights.sounds;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(TheatricalExtraLights.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> CONFETTI_CANNON =
            SOUNDS.register("confetti_cannon.pop", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(TheatricalExtraLights.MOD_ID, "confetti_cannon.pop")));

    public static final RegistrySupplier<SoundEvent> FLAME_THROWER_LOOP =
            SOUNDS.register("block.flame_thrower.loop", () ->
                    SoundEvent.createFixedRangeEvent(
                            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block.flame_thrower.loop"),
                            48.0f
                    ));

    public static final RegistrySupplier<SoundEvent> FLOW2JET_LOOP =
            SOUNDS.register("block.flow2jet.loop", () ->
                    SoundEvent.createFixedRangeEvent(
                            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block.flow2jet.loop"),
                            48.0f
                    ));

    public static final RegistrySupplier<SoundEvent> MORTAR_HIT =
            SOUNDS.register("mortar_hit_shot", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(TheatricalExtraLights.MOD_ID, "mortar_hit_shot")));
    public static void initialize() {
        SOUNDS.register();
    }
}
