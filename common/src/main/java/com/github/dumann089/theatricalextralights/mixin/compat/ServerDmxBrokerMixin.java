package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Soft target: {@code ServerDmxBroker} exists on recent Theatrical builds only
 * ({@code alpha.28.9999}+), not on the published Maven {@code alpha.28.120} API.
 * {@link com.github.dumann089.theatricalextralights.mixin.ExtraLightsMixinPlugin}
 * skips this mixin when the class is absent.
 */
@Mixin(targets = "dev.imabad.theatrical.networks.ServerDmxBroker", remap = false)
public class ServerDmxBrokerMixin {

    @Redirect(
            method = "flush",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/imabad/theatrical/api/dmx/DMXConsumer;consume([B)V"
            )
    )
    private static void extralights$safeConsume(DMXConsumer consumer, byte[] dmxValues) {
        try {
            consumer.consume(dmxValues);
        } catch (RuntimeException ex) {
            TheatricalExtraLights.LOGGER.error(
                    "DMX consume failed for {} — skipped this tick",
                    consumer.getClass().getName(),
                    ex
            );
        }
    }
}
