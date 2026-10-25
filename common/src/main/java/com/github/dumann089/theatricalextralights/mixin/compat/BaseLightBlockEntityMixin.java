package com.github.dumann089.theatricalextralights.mixin.compat;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.SmoothedLight;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dá à {@code BaseLightBlockEntity} do Theatrical OFICIAL a suavização de cor/intensidade, sem
 * precisar de um Theatrical modificado. Só mexe em estado visual do cliente: não toca em
 * {@code getIntensity()}, {@code getColour()}, {@code getLightLuminance()} nem
 * {@code getLightColour()} (esses continuam exatamente como o Theatrical define; as fixtures do
 * Extra Lights montam a versão suavizada delas em {@code ExtraLightsLightBlockEntity}).
 *
 * <p>Se o {@code lightTick} ainda não rodou (ou a injeção foi pulada), os getters devolvem o valor
 * cru em vez de 0 — na pior hipótese vira "sem suavização", nunca "fixture apagada".
 */
@Mixin(targets = "dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity", remap = false)
public abstract class BaseLightBlockEntityMixin implements SmoothedLight {

    @Shadow protected int intensity;
    @Shadow protected int red;
    @Shadow protected int green;
    @Shadow protected int blue;

    @Unique private float extralights$smoothIntensity;
    @Unique private float extralights$smoothRed;
    @Unique private float extralights$smoothGreen;
    @Unique private float extralights$smoothBlue;
    @Unique private float extralights$prevSmoothIntensity;
    @Unique private float extralights$prevSmoothRed;
    @Unique private float extralights$prevSmoothGreen;
    @Unique private float extralights$prevSmoothBlue;
    @Unique private boolean extralights$smoothInitialized = false;

    @Inject(method = "lightTick", at = @At("TAIL"), require = 0)
    private void extralights$smoothLightTick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }

        extralights$prevSmoothIntensity = extralights$smoothIntensity;
        extralights$prevSmoothRed = extralights$smoothRed;
        extralights$prevSmoothGreen = extralights$smoothGreen;
        extralights$prevSmoothBlue = extralights$smoothBlue;

        if (!extralights$smoothInitialized) {
            extralights$smoothIntensity = intensity;
            extralights$smoothRed = red;
            extralights$smoothGreen = green;
            extralights$smoothBlue = blue;
            extralights$prevSmoothIntensity = intensity;
            extralights$prevSmoothRed = red;
            extralights$prevSmoothGreen = green;
            extralights$prevSmoothBlue = blue;
            extralights$smoothInitialized = true;
            return;
        }

        float speed = TheatricalExtraLightsConfig.getColorTransitionSpeed();
        extralights$smoothIntensity += (intensity - extralights$smoothIntensity) * speed;
        extralights$smoothRed += (red - extralights$smoothRed) * speed;
        extralights$smoothGreen += (green - extralights$smoothGreen) * speed;
        extralights$smoothBlue += (blue - extralights$smoothBlue) * speed;

        // gruda no alvo quando já está perto, pra não arrastar uma cauda infinita
        if (Math.abs(intensity - extralights$smoothIntensity) < 0.5f) extralights$smoothIntensity = intensity;
        if (Math.abs(red - extralights$smoothRed) < 0.5f) extralights$smoothRed = red;
        if (Math.abs(green - extralights$smoothGreen) < 0.5f) extralights$smoothGreen = green;
        if (Math.abs(blue - extralights$smoothBlue) < 0.5f) extralights$smoothBlue = blue;
    }

    @Override
    public float getSmoothedIntensity(float partialTicks) {
        if (!extralights$smoothInitialized) {
            return intensity;
        }
        return extralights$prevSmoothIntensity
                + (extralights$smoothIntensity - extralights$prevSmoothIntensity) * partialTicks;
    }

    @Override
    public int getSmoothedColour(float partialTicks) {
        if (!extralights$smoothInitialized) {
            return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
        }
        int r = (int) Mth.clamp(extralights$prevSmoothRed + (extralights$smoothRed - extralights$prevSmoothRed) * partialTicks, 0, 255);
        int g = (int) Mth.clamp(extralights$prevSmoothGreen + (extralights$smoothGreen - extralights$prevSmoothGreen) * partialTicks, 0, 255);
        int b = (int) Mth.clamp(extralights$prevSmoothBlue + (extralights$smoothBlue - extralights$prevSmoothBlue) * partialTicks, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** Versão genérica (fixtures do Theatrical base). O Extra Lights sobrescreve isso com o strobe por cima. */
    @Override
    public float getDisplayIntensity(float partialTicks) {
        return getSmoothedIntensity(partialTicks);
    }

    @Override
    public int getDisplayColour(float partialTicks) {
        return getSmoothedColour(partialTicks);
    }
}
