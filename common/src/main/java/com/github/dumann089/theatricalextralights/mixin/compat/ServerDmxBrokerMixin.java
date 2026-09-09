package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import dev.imabad.theatrical.networks.ServerDmxBroker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * One broken fixture must not take down the dedicated server.
 * Theatrical's {@code ServerDmxBroker.flush} calls {@code consume} unsafely on the server tick.
 */
@Mixin(value = ServerDmxBroker.class, remap = false)
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
