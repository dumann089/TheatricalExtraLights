package com.github.dumann089.theatricalextralights.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ExtraLightsMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("ReloadShaderManagerMixin")) {
            return isClassPresent("com.lowdragmc.shimmer.client.shader.ReloadShaderManager");
        }
        if (mixinClassName.contains("BlockEntityTheatricalDmxExtendedMixin")) {
            return isClassPresent("dev.imabad.theatrical.api.dmx.DmxFrameExtendedFixture");
        }
        if (mixinClassName.contains("ServerDmxBrokerMixin")) {
            return isClassPresent("dev.imabad.theatrical.networks.ServerDmxBroker");
        }
        return true;
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, ExtraLightsMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!mixinClassName.endsWith("BlockEntityTheatricalDmxExtendedMixin")) {
            return;
        }
        if (!isClassPresent("dev.imabad.theatrical.api.dmx.DmxFrameExtendedFixture")) {
            return;
        }
        String theatricalExtended = "dev/imabad/theatrical/api/dmx/DmxFrameExtendedFixture";
        if (!targetClass.interfaces.contains(theatricalExtended)) {
            targetClass.interfaces.add(theatricalExtended);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
