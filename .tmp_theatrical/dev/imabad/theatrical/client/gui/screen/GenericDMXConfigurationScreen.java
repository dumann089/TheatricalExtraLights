package dev.imabad.theatrical.client.gui.screen;

import dev.imabad.theatrical.Theatrical;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import dev.imabad.theatrical.client.gui.widgets.BetterStringWidget;
import dev.imabad.theatrical.client.gui.widgets.LabeledEditBox;
import dev.imabad.theatrical.net.UpdateDMXFixture;
import dev.imabad.theatrical.net.UpdateNetworkId;
import dev.imabad.theatrical.util.UUIDUtil;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5676;
import net.minecraft.class_7843;
import net.minecraft.class_7849;

public class GenericDMXConfigurationScreen<T extends DMXConsumer> extends class_437 {
    private final class_2960 GUI = new class_2960(Theatrical.MOD_ID, "textures/gui/blank.png");

    protected final int imageWidth;
    protected final int imageHeight;
    protected int xCenter;
    protected int yCenter;
    private LabeledEditBox dmxAddress, dmxUniverse;
    private UUID networkId = UUIDUtil.NULL;
    private final T be;
    private final class_2338 blockPos;
    private final String titleTranslationKey;
    protected class_7849 layout;

    public GenericDMXConfigurationScreen(T be, class_2338 pos, String titleTranslationKey) {
        super(class_2561.method_43471(titleTranslationKey));
        this.imageWidth = 176;
        this.imageHeight = 126;
        this.be = be;
        this.blockPos = pos;
        this.titleTranslationKey = titleTranslationKey;
        this.networkId = be.getNetworkId();
    }

    public void addExtraWidgetsToUI(){}

    @Override
    protected void method_25426() {
        super.method_25426();
        layout = new class_7849(imageWidth, 156, class_7849.class_7851.field_40790);
        layout.method_46500().method_46467().method_46474().method_46464(10);
        layout.method_46495(new BetterStringWidget(class_2561.method_43471(titleTranslationKey), this.field_22793).method_46438(4210752).setShadow(false));
        this.dmxAddress = new LabeledEditBox(this.field_22793, xCenter, yCenter, 50, 10, class_2561.method_43471("fixture.dmxStart"));
        this.dmxAddress.method_1852(Integer.toString(this.be.getChannelStart()));
        this.method_25395(this.dmxAddress);
        layout.method_46495(dmxAddress);
        this.dmxUniverse = new LabeledEditBox(this.field_22793, xCenter, yCenter, 50, 10, class_2561.method_43471("artneti.dmxUniverse"));
        this.dmxUniverse.method_1852(Integer.toString(this.be.getUniverse()));
        layout.method_46495(dmxUniverse);
        addExtraWidgetsToUI();
        layout.method_46495(new class_5676.class_5677<UUID>((networkId) ->
        {
            if (TheatricalClient.getArtNetManager().getKnownNetworks().containsKey(networkId)) {
                return class_2561.method_43470(TheatricalClient.getArtNetManager().getKnownNetworks().get(networkId));
            }
            return class_2561.method_43470("Unknown");
        }
        ).method_42729(class_5676.class_5680.method_32627(Stream.concat(Stream.of(UUIDUtil.NULL),
                        TheatricalClient.getArtNetManager().getKnownNetworks().keySet().stream()).collect(Collectors.toList())))
                .method_32616().method_32619(networkId)
                .method_32617(xCenter, yCenter, 150, 20,
                        class_2561.method_43471("screen.artnetconfig.network"), (obj, val) -> {
                            this.networkId = val;
                        }));
        layout.method_46495(
                new class_4185.class_7840(class_2561.method_43471("artneti.save"), button -> this.update())
                        .method_46433(xCenter, yCenter)
                        .method_46437(100, 20)
                        .method_46431()
        );
        refreshLayout();
        this.method_48640();
    }
    protected void refreshLayout(){
        if(layout == null)
            return;
        layout.method_48222();
        layout.method_48206(this::method_37063);
    }

    protected void method_48640() {
        class_7843.method_48634(this.layout, this.method_48202());
    }


    protected void update(){
        try {
            int dmx = Integer.parseInt(this.dmxAddress.method_1882());
            if (dmx > 512 || dmx < 0) {
                return;
            }

            int universe = Integer.parseInt(this.dmxUniverse.method_1882());
            if (universe < 0) {
                return;
            }
            new UpdateDMXFixture(blockPos, dmx, universe).sendToServer();
            new UpdateNetworkId(blockPos, networkId).sendToServer();
        } catch(NumberFormatException ignored) {
            //We need a nicer way to show that this is invalid?
        }
    }

    @Override
    public void method_25420(class_332 guiGraphics) {
        super.method_25420(guiGraphics);
        this.renderWindow(guiGraphics);
    }

    @Override
    public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.method_25420(guiGraphics);
        super.method_25394(guiGraphics, mouseX, mouseY, partialTick);
        this.renderLabels(guiGraphics);
    }

    private void renderWindow(class_332 guiGraphics){
        int layoutHeight = 0;
        if(layout != null) {
            layoutHeight = layout.method_25364();
        }
        int relX = (this.field_22789 - this.imageWidth) / 2;
        int relY = (this.field_22790 - layoutHeight) / 2;
        guiGraphics.method_25293(GUI, relX, relY, imageWidth, layoutHeight, 0, 0, this.imageWidth, this.imageHeight, 256,256);
    }

    protected void renderLabels(class_332 guiGraphics) {
//        renderLabel(guiGraphics, titleTranslationKey, 5,5);
//        renderLabel(guiGraphics, "fixture.dmxStart", 0,15);
//        renderLabel(guiGraphics, "artneti.dmxUniverse", 0,40);
//        renderLabel(guiGraphics, "artneti.network", 0,60);
    }

    protected void renderLabel(class_332 guiGraphics, String translationKey, int offSetX, int offSetY){
        class_5250 translatable = class_2561.method_43471(translationKey);
        guiGraphics.method_51439(field_22793, translatable, xCenter + (this.imageWidth / 2) - (this.field_22793.method_1727(translatable.getString()) / 2), yCenter + offSetY, 0x404040, false);
    }

    @Override
    public boolean method_25421() {
        return false;
    }
}
