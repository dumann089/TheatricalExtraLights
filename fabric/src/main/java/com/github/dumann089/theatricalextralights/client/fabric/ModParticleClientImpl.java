package com.github.dumann089.theatricalextralights.client.fabric;

import com.github.dumann089.theatricalextralights.client.particle.Co2JetCoreParticle;
import com.github.dumann089.theatricalextralights.client.particle.Co2JetPuffParticle;
import com.github.dumann089.theatricalextralights.client.particle.ConfettiParticle;
import com.github.dumann089.theatricalextralights.client.particle.FireworkSparkParticle;
import com.github.dumann089.theatricalextralights.client.particle.FlameThrowerJetParticle;
import com.github.dumann089.theatricalextralights.client.particle.FlameThrowerPuffParticle;
import com.github.dumann089.theatricalextralights.client.particle.WaterFanParticle;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticle;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticleProvider;
import com.github.dumann089.theatricalextralights.client.particle.WaterMovingJetParticle;
import com.github.dumann089.theatricalextralights.particle.ModParticle;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;

@SuppressWarnings("unused")
public class ModParticleClientImpl {
    private ModParticleClientImpl() {
    }

    public static void registerPlatformProviders() {
        ParticleProviderRegistry.register(
                ModParticle.WATERJET_OPTIONS,
                WaterJetParticleProvider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.WATERJETPARTICLE,
                WaterJetParticle::provider
        );
        ParticleProviderRegistry.register(
                ModParticle.CONFETTI,
                ConfettiParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.FIREWORK_SPARK,
                FireworkSparkParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.FLAME_THROWER_JET,
                FlameThrowerJetParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.FLAME_THROWER_PUFF,
                FlameThrowerPuffParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.CO2_JET_CORE,
                Co2JetCoreParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.CO2_JET_PUFF,
                Co2JetPuffParticle.Provider::new
        );
        ParticleProviderRegistry.register(
                ModParticle.WATERFANPARTICLE,
                WaterFanParticle::provider
        );
        ParticleProviderRegistry.register(
                ModParticle.WATERMOVINGJETPARTICLE,
                WaterMovingJetParticle::provider
        );
    }
}
