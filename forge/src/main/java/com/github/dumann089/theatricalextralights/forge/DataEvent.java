package com.github.dumann089.theatricalextralights.forge;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import com.github.dumann089.theatricalextralights.blocks.Blocks;
import dev.imabad.theatrical.Theatrical;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

public class DataEvent {
    public static void onData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();

        gen.addProvider(event.includeClient(), new BlockState(output, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new Item(output, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(), new Lang(output, "en_us"));
    }

    public static class BlockState extends BlockStateProvider {
        public BlockState(PackOutput output, ExistingFileHelper exFileHelper) {
            super(output, TheatricalExtraLights.MOD_ID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {

        }

    }

    public static class Item extends ItemModelProvider {

        public Item(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, TheatricalExtraLights.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            withExistingParent(Blocks.MOVING_VL2C_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_vl2c/moving_vl2c_whole"));
            withExistingParent(Blocks.MOVING_BEAM_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_beam/moving_beam_whole"));
            withExistingParent(Blocks.MOVING_SCAN_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_scan/moving_scan_whole"));
            withExistingParent(Blocks.RGB_BAR.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/ledbar/ledbar_whole"));
            withExistingParent(Blocks.MOVING_VL6_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving_vl6/moving_vl6_whole"));
            withExistingParent(Blocks.LED_FOUNTAIN.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/fountain_lamp/fountain_lamp_whole"));
            withExistingParent(Blocks.LED_PANEL_2.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/ledpanel/led_panel_body"));
            withExistingParent(Blocks.BIG_PANEL.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/bigpanel3x3/bigpanel_body"));
            withExistingParent(Blocks.BIG_PANEL2.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/bigpanel2x3/bigpanel2x3_body"));
            withExistingParent(Blocks.PAR_LED.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/parled/parled_body_whole"));
            withExistingParent(Blocks.BLINDER.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/4x2_blinder/4x2_blinder_whole"));
            withExistingParent(Blocks.BLINDER1X1_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/blinder1x1/blinder1x1_whole"));
            withExistingParent(Blocks.LASER_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/laser/laser_whole"));
            withExistingParent(Blocks.TRUSS_3LIGHTS.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/truss3x3lights/truss3x3light_whole"));
            withExistingParent(Blocks.STROBE.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/strobe/strobe_whole"));
            withExistingParent(Blocks.BEAM_7R_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/seven_beam/seven_whole"));
            withExistingParent(Blocks.SOURCE_FOUR_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/spotlight/source_whole"));
            withExistingParent(Blocks.MAC_VIP_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/macvip/macvip_whole"));
            withExistingParent(Blocks.SHARPLUS_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/sharplus/sharplus_whole"));
            withExistingParent(Blocks.MOVING500_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/moving500/moving500_whole"));
            withExistingParent(Blocks.PAR1000_RED_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_red_whole"));
            withExistingParent(Blocks.PAR1000_BLUE_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_blue_whole"));
            withExistingParent(Blocks.PAR1000_GREEN_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_green_whole"));
            withExistingParent(Blocks.PAR1000_MAGENTA_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_magenta_whole"));
            withExistingParent(Blocks.PAR1000_AMBER_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_amber_whole"));
            withExistingParent(Blocks.PAR1000_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_warm_whole"));
            withExistingParent(Blocks.PAR1000_PURPLE_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_purple_whole"));
            withExistingParent(Blocks.PAR1000_LIGHTBLUE_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_lightblue_whole"));
            withExistingParent(Blocks.PAR1000_WHITE_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/par1000/par1000_white_whole"));
            withExistingParent(Blocks.ROBITSPOT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/robitspot/robitspot_whole"));
            withExistingParent(Blocks.VERVESPOT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/vervespot/vervespot_whole"));
            withExistingParent(Blocks.VERTICALBAR_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/barvertical/barvertical_whole"));
            withExistingParent(Blocks.SEARCHLIGHT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/searchlight/searchlight_whole"));
            withExistingParent(Blocks.BLINDER_WARM_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/4x2_blinder/4x2_blinder_whole"));
            withExistingParent(Blocks.SEARCHLIGHT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/searchlight/searchlight_whole"));
            withExistingParent(Blocks.WASHLIGHT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/washlight/washlight_whole"));
            withExistingParent(Blocks.ATOMICTILT_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomictilt/atomictilt_whole"));
            withExistingParent(Blocks.MINIWASH_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/miniwash/miniwash_whole"));
            withExistingParent(Blocks.X8PAR_RED_BLOCK.getId().getPath(), new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/x8par64/x8par64_red_whole"));

        }
    }

    public static class Lang extends LanguageProvider {

        public Lang(PackOutput output, String locale) {
            super(output, TheatricalExtraLights.MOD_ID, locale);
        }

        @Override
        protected void addTranslations() {
            addBlock(Blocks.MOVING_VL2C_BLOCK, "Moving VL2");
            addBlock(Blocks.MOVING_BEAM_BLOCK, "Moving Beam");
            addBlock(Blocks.MOVING_SCAN_BLOCK, "Moving Scan");
            addBlock(Blocks.MOVING_VL6_BLOCK, "Moving VL6");
            addBlock(Blocks.LED_FOUNTAIN, "Led Fountain");
            addBlock(Blocks.LED_PANEL_2, "LED Panel 2");
            addBlock(Blocks.BIG_PANEL, "Big Panel 3x3");
            addBlock(Blocks.BIG_PANEL2, "Big Panel 3x2 ");
            addBlock(Blocks.PAR_LED, "LED Pair");
            addBlock(Blocks.RGB_BAR, "RGB Bar");
            addBlock(Blocks.VERTICALBAR_BLOCK, "Vertical RGB Bar");
            addBlock(Blocks.BLINDER, "Blinder 4x2");
            addBlock(Blocks.BLINDER_WARM_BLOCK, "Blinder 4x2 Warm");
            addBlock(Blocks.BLINDER1X1_BLOCK, "Blinder 1x1");
            addBlock(Blocks.LASER_BLOCK, "Laser");
            addBlock(Blocks.TRUSS_3LIGHTS, "Truss 3x3 Lights");
            addBlock(Blocks.STROBE, "Strobe");
            addBlock(Blocks.BEAM_7R_BLOCK, "Beam 7r");
            addBlock(Blocks.SOURCE_FOUR_BLOCK, "Source Four");
            addBlock(Blocks.MAC_VIP_BLOCK, "Mac Vip");
            addBlock(Blocks.SHARPLUS_BLOCK, "Sharplus");
            addBlock(Blocks.MOVING500_BLOCK, "Moving 500");
            addBlock(Blocks.ROBITSPOT_BLOCK, "Robit Spot");
            addBlock(Blocks.VERVESPOT_BLOCK, "Verve Spot");
            addBlock(Blocks.SEARCHLIGHT_BLOCK, "Searchlight");
            addBlock(Blocks.WASHLIGHT_BLOCK, "Wash FX648");
            addBlock(Blocks.WASHLIGHT_BLOCK, "Mini Wash");
            addBlock(Blocks.ATOMICTILT_BLOCK, "Atomic Tilt");
            addBlock(Blocks.PAR1000_RED_BLOCK, "Par 1000 Red");
            addBlock(Blocks.PAR1000_BLUE_BLOCK, "Par 1000 Blue");
            addBlock(Blocks.PAR1000_GREEN_BLOCK, "Par 1000 Green");
            addBlock(Blocks.PAR1000_MAGENTA_BLOCK, "Par 1000 Magenta");
            addBlock(Blocks.PAR1000_AMBER_BLOCK, "Par 1000 Amber");
            addBlock(Blocks.PAR1000_PURPLE_BLOCK, "Par 1000 Purple");
            addBlock(Blocks.PAR1000_BLOCK, "Par 1000");
            addBlock(Blocks.PAR1000_LIGHTBLUE_BLOCK, "Par 1000 Light Blue");
            addBlock(Blocks.PAR1000_LIGHTBLUE_BLOCK, "Par 1000 White");
            addBlock(Blocks.X8PAR_RED_BLOCK, "x8 Par 64");

            





            add("itemGroup.theatricalextralights", "Theatrical: Extra Lights");
        }
    }

}
