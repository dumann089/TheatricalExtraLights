// ModParticle.java
package com.github.dumann089.theatricalextralights.particle;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.client.particle.FireworkSparkParticleOptions;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticleOptions;
import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ModParticle {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(TheatricalExtraLights.MOD_ID, Registries.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> WATERJETPARTICLE = PARTICLE_TYPES.register("water_jet_particle", () ->
            new SimpleParticleType(false) {});

    public static final RegistrySupplier<SimpleParticleType> WATERFANPARTICLE = PARTICLE_TYPES.register("water_fan_particle", () ->
            new SimpleParticleType(false) {});
    public static final RegistrySupplier<SimpleParticleType> WATERJET2PARTICLE = PARTICLE_TYPES.register("water_jet2_particle", () ->
            new SimpleParticleType(false) {});

    public static final RegistrySupplier<SimpleParticleType> WATERMOVINGJETPARTICLE = PARTICLE_TYPES.register("water_moving_jet_particle", () ->
            new SimpleParticleType(false) {});

    public static final RegistrySupplier<ParticleType<WaterJetParticleOptions>>
            WATERJET_OPTIONS = PARTICLE_TYPES.register(
            "waterjet",
            () -> new ParticleType<WaterJetParticleOptions>(false, WaterJetParticleOptions.DESERIALIZER) {
                @Override
                public Codec<WaterJetParticleOptions> codec() {
                    return WaterJetParticleOptions.CODEC;
                }
            }
    );

    public static final RegistrySupplier<SimpleParticleType> CONFETTI = PARTICLE_TYPES.register("confetti", () ->
            new SimpleParticleType(false) {});

    public static final RegistrySupplier<ParticleType<FireworkSparkParticleOptions>>
            FIREWORK_SPARK = PARTICLE_TYPES.register(
            "firework_spark",
            () -> new ParticleType<FireworkSparkParticleOptions>(false, FireworkSparkParticleOptions.DESERIALIZER) {
                @Override
                public Codec<FireworkSparkParticleOptions> codec() {
                    return FireworkSparkParticleOptions.CODEC;
                }
            }
    );

    public static final RegistrySupplier<SimpleParticleType> FLAME_THROWER_JET = PARTICLE_TYPES.register(
            "flame_thrower_jet",
            () -> new SimpleParticleType(false) {}
    );

    public static final RegistrySupplier<SimpleParticleType> FLAME_THROWER_PUFF = PARTICLE_TYPES.register(
            "flame_thrower_puff",
            () -> new SimpleParticleType(false) {}
    );

    public static final RegistrySupplier<SimpleParticleType> CO2_JET_CORE = PARTICLE_TYPES.register(
            "co2_jet_core",
            () -> new SimpleParticleType(false) {}
    );

    public static final RegistrySupplier<SimpleParticleType> CO2_JET_PUFF = PARTICLE_TYPES.register(
            "co2_jet_puff",
            () -> new SimpleParticleType(false) {}
    );

    public static void initialize() {
        PARTICLE_TYPES.register();
    }
}
