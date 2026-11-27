package com.github.dumann089.theatricalextralights.entities;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            TheatricalExtraLightsRegistry.get(Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<FireworkRocketEntity>> FIREWORK_ROCKET =
            ENTITY_TYPES.register("firework_rocket", () ->
                    EntityType.Builder.<FireworkRocketEntity>of(FireworkRocketEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(512)
                            .updateInterval(1)
                            .build("firework_rocket"));

    public static final RegistrySupplier<EntityType<BroadcastCameraEntity>> BROADCAST_CAMERA =
            ENTITY_TYPES.register("broadcast_camera", () ->
                    EntityType.Builder.<BroadcastCameraEntity>of(BroadcastCameraEntity::new, MobCategory.MISC)
                            .sized(0.9f, 1.7f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("broadcast_camera"));

    public static final RegistrySupplier<EntityType<JibCameraEntity>> JIB_CAMERA =
            ENTITY_TYPES.register("jib_camera", () ->
                    EntityType.Builder.<JibCameraEntity>of(JibCameraEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.9f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("jib_camera"));

    public static final RegistrySupplier<EntityType<ShoulderCameraEntity>> SHOULDER_CAMERA =
            ENTITY_TYPES.register("shoulder_camera", () ->
                    EntityType.Builder.<ShoulderCameraEntity>of(ShoulderCameraEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .fireImmune()
                            .build("shoulder_camera"));

    private ModEntities() {
    }

    public static void init() {
        ENTITY_TYPES.register();
    }
}
