package com.github.dumann089.theatricalextralights.compat;

import dev.imabad.theatrical.api.Support;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Support blocks that Theatrical does not know about.
 *
 * <p>Hanging fixtures only count a block as a support when it implements
 * {@link Support}. Concert Stage Craft's trusses are ordinary blocks, so a fixture placed under
 * them was treated as free-standing: it kept its default height and clipped into (or floated
 * under) the truss until a fixture-wrench offset was added by hand. The same transform is applied
 * here as for Theatrical's own trusses, keeping the model centred on its own block.
 */
public final class SupportCompat {

    /** Concert Stage Craft's registry namespace. */
    public static final String CSC_NAMESPACE = "concertstage";

    private SupportCompat() {
    }

    /** True for Theatrical/Extra Lights supports and CSC truss blocks. */
    public static boolean isSupport(BlockState state) {
        return state.getBlock() instanceof Support || isCscTruss(state);
    }

    /**
     * CSC registers every truss section, corner, junction, base and roof piece under
     * {@code concertstage:truss_*}. Matching the registry name (rather than a class name) works on
     * both loaders and across CSC versions.
     */
    public static boolean isCscTruss(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null && CSC_NAMESPACE.equals(id.getNamespace()) && id.getPath().contains("truss")) {
            return true;
        }
        // Older/relocated CSC builds may move the registry namespace but keep the package.
        return block.getClass().getName().startsWith("com.concertstage.");
    }
}
