package com.github.dumann089.theatricalextralights.items;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class FixtureWrenchItem extends Item {

    public FixtureWrenchItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .arch$tab(TheatricalExtraLights.TAB));
    }
}
