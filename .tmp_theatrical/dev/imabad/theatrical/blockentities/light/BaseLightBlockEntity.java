package dev.imabad.theatrical.blockentities.light;

import dev.imabad.theatrical.api.DynamicLightProvider;
import dev.imabad.theatrical.api.FixtureProvider;
import dev.imabad.theatrical.api.Support;
import dev.imabad.theatrical.blockentities.ClientSyncBlockEntity;
import dev.imabad.theatrical.blocks.HangableBlock;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.config.TheatricalConfig;
import dev.imabad.theatrical.lighting.LightManager;
import dev.imabad.theatrical.mixin.ClipContextAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3f;

import java.util.Optional;
import net.minecraft.class_1937;
import net.minecraft.class_2335;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_761;

public abstract class BaseLightBlockEntity extends ClientSyncBlockEntity implements FixtureProvider, DynamicLightProvider {
    class_238 INFINITE_EXTENT_AABB = new class_238(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    private double distance = 0;
    protected int pan, tilt, focus, intensity, red, green, blue = 0;
    protected int prevTilt, prevPan, prevFocus, prevIntensity, prevRed, prevGreen, prevBlue, prevColour = 0;
    protected float prevSpread = 0;
    private long tickTimer = 0;
    private class_2338 emissionBlock, prevEmissionBlock;
    private int prevLuminance;
    private LongOpenHashSet trackedLitChunkPos = new LongOpenHashSet();

    public BaseLightBlockEntity(class_2591<?> blockEntityType, class_2338 blockPos, class_2680 blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public void write(class_2487 compoundTag) {
        if (compoundTag == null) {
            compoundTag = new class_2487();
        }
        compoundTag.method_10569("pan", this.pan);
        compoundTag.method_10569("tilt", this.tilt);
        compoundTag.method_10569("focus", this.focus);
        compoundTag.method_10544("timer", tickTimer);
        compoundTag.method_10549("distance", distance);
        compoundTag.method_10569("intensity", intensity);
        compoundTag.method_10569("prevIntensity", prevIntensity);
        compoundTag.method_10569("red", red);
        compoundTag.method_10569("green", green);
        compoundTag.method_10569("blue", blue);
        compoundTag.method_10569("prevRed", prevRed);
        compoundTag.method_10569("prevGreen", prevGreen);
        compoundTag.method_10569("prevBlue", prevBlue);
    }

    @Override
    public void read(class_2487 compoundTag) {
        pan = compoundTag.method_10550("pan");
        tilt = compoundTag.method_10550("tilt");
        focus = compoundTag.method_10550("focus");
        prevPan = pan;
        prevTilt = tilt;
        prevFocus = focus;
        tickTimer = compoundTag.method_10537("timer");
        distance = compoundTag.method_10574("distance");
        intensity = compoundTag.method_10550("intensity");
        prevIntensity = compoundTag.method_10550("prevIntensity");
        red = compoundTag.method_10550("red");
        green = compoundTag.method_10550("green");
        blue = compoundTag.method_10550("blue");
        prevRed = compoundTag.method_10550("prevRed");
        prevGreen = compoundTag.method_10550("prevGreen");
        prevBlue = compoundTag.method_10550("prevBlue");
    }

    public double getDistance() {
        return distance;
    }

    public class_238 getRenderBoundingBox(){
        return INFINITE_EXTENT_AABB;
    }

    public class_2338 getEmissionBlock(){
        return emissionBlock;
    }

    public class_2338 getPrevEmissionBlock() {
        return prevEmissionBlock;
    }

    public void setPrevEmissionBlock(class_2338 prevEmissionBlock) {
        this.prevEmissionBlock = prevEmissionBlock;
    }

    public int getPrevLuminance() {
        return prevLuminance;
    }

    public void setPrevLuminance(int prevLuminance) {
        this.prevLuminance = prevLuminance;
    }

    public LongOpenHashSet getTrackedLitChunkPos() {
        return trackedLitChunkPos;
    }

    public void setTrackedLitChunkPos(LongOpenHashSet trackedLitChunkPos) {
        this.trackedLitChunkPos = trackedLitChunkPos;
    }

    protected boolean storePrev(){
        boolean hasChanged = false;
        if(tilt != prevTilt){
            prevTilt = tilt;
            hasChanged = true;
        }
        if(pan != prevPan){
            prevPan = pan;
            hasChanged = true;
        }
        if(focus != prevFocus){
            prevFocus = focus;
            hasChanged = true;
        }
        if(intensity != prevIntensity){
            prevIntensity =  intensity;
            hasChanged = true;
        }
        if(red != prevRed){
            prevRed = red;
            hasChanged = true;
        }
        if(green != prevGreen){
            prevGreen = green;
            hasChanged = true;
        }
        if(blue != prevBlue){
            prevBlue = blue;
            hasChanged = true;
        }
        return hasChanged;
    }

    @Override
    public float getIntensity() {
        return intensity;
    }

    @Override
    public float getMaxLightDistance() {
        return TheatricalConfig.INSTANCE.COMMON.defaultMaxLightDist;
    }

    @Override
    public boolean shouldTrace() {
        return getIntensity() > 0;
    }

    @Override
    public boolean emitsLight() {
        return !method_11010().method_11654(HangableBlock.BROKEN) && TheatricalConfig.INSTANCE.COMMON.shouldEmitLight;
    }

    @Override
    public boolean isUpsideDown() {
        return false;
    }

    public static <T extends class_2586> void tick(class_1937 level, class_2338 pos, class_2680 state, T be) {
        BaseLightBlockEntity tile = (BaseLightBlockEntity) be;
//        if(!level.isClientSide){
            tile.tickTimer++;
            if(tile.tickTimer >= 5){
//                if(tile.storePrev()){
//                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
//                }

                tile.tickTimer = 0;
            }
            if(tile.shouldTrace()){
                tile.distance = tile.doRayTrace();
            }
            if (level.method_8608() && LightManager.shouldUpdateDynamicLight()) {
                if (tile.method_11015()) {
                    tile.setLightEnabled(false);
                } else {
                    tile.lightTick();
                    LightManager.updateTracking(tile);
                }
            }
//        } else {
//        }
    }

    public int getPan() {
        return pan;
    }

    public int getTilt() {
        return tilt;
    }

    public int getFocus() {
        return focus;
    }

    public int getPrevTilt() {
        return prevTilt;
    }

    public int getPrevPan() {
        return prevPan;
    }

    public int getPrevFocus() {
        return prevFocus;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getPrevIntensity() {
        return prevIntensity;
    }

    public int getPrevRed() {
        return prevRed;
    }

    public int getPrevGreen() {
        return prevGreen;
    }

    public int getPrevBlue() {
        return prevBlue;
    }

    public int getColorHex(){
        return (getRed() << 16) | (getGreen() << 8) | getBlue();
    }

    public int getPrevColor(){
        return (getPrevRed() << 16) | (getPrevGreen() << 8) | getPrevBlue();
    }

    public int getPrevColour() {
        return prevColour;
    }

    public void setPrevColour(int prevColour) {
        this.prevColour = prevColour;
    }

    public Optional<class_2680> getSupportingStructure(){
        if(method_10997() != null){
            class_2680 blockState = method_10997().method_8320(method_11016()
                    .method_10093(method_11010().method_11654(HangableBlock.HANG_DIRECTION)));
            if(blockState.method_26204() instanceof Support) {
                return Optional.of(blockState);
            }
        }
        return Optional.empty();
    }

    public int getBasePan(){
        if(isHangingNonVertically(method_11010())){
            class_2350 facing = method_11010().method_11654(BaseLightBlock.field_11177);
            return switch (facing) {
                case field_11043, field_11035 -> 90;
                case field_11039 -> 180;
                default -> 0;
            };
        }
        return 0;
    }

    public int calculatePartialColour(float partialTicks){
        int r = (int) (getPrevRed() + ((getRed()) - getPrevRed()) * partialTicks);
        int g = (int) (getPrevGreen() + ((getGreen()) - getPrevGreen()) * partialTicks);
        int b = (int) (getPrevBlue() + ((getBlue()) - getPrevBlue()) * partialTicks);
        return (r << 16) | (g << 8) | b;
    }

    public int getColour(){
        return (getRed() << 16) | (getGreen() << 8) | getBlue();
    }

    public static final class_243 calculateViewVector(float xRot, float yRot) {
        float f = xRot * 0.017453292F;
        float g = -yRot * 0.017453292F;
        float h = class_3532.method_15362(g);
        float i = class_3532.method_15374(g);
        float j = class_3532.method_15362(f);
        float k = class_3532.method_15374(f);
        return new class_243((double)(i * j), (double)(-k), (double)(h * j));
    }

    public static boolean isHangingNonVertically(class_2680 blockState){
        return isHangingNonVertically(blockState.method_11654(BaseLightBlock.HANG_DIRECTION),
                blockState.method_11654(BaseLightBlock.HANGING));
    }
    public static boolean isHangingNonVertically(class_2350 hangDirection, boolean isHanging){
        return (hangDirection != class_2350.field_11033 && hangDirection != class_2350.field_11036) && isHanging;
    }

    public static class_243 rayTraceDir(BaseLightBlockEntity be){
        class_2680 blockState = be.method_11010();
        class_2350 hangDirection = blockState.method_11654(BaseLightBlock.HANG_DIRECTION);
        class_2350 direction = blockState.method_11654(BaseLightBlock.field_11177);
        boolean isHangingNonVertically = isHangingNonVertically(hangDirection, blockState.method_11654(BaseLightBlock.HANGING));
        // TODO: Come back and try make this use the same code for both.
        if(!isHangingNonVertically) {
            float tilt = be.getTilt();
            if (be.isUpsideDown() || be.getFixture().invertTilt()) {
                tilt = -tilt;
            }
            if(be instanceof LEDPanelBlockEntity){
                if(blockState.method_11654(BaseLightBlock.HANG_DIRECTION) == class_2350.field_11033){
                    tilt = -90;
                } else if (blockState.method_11654(BaseLightBlock.HANG_DIRECTION) == class_2350.field_11036){
                    tilt = 90;
                }
            }
            float pan = (direction.method_10144() - be.getPan());
            if(direction.method_10166() == class_2350.field_11039.method_10166()){
                pan -= 180;
            }
            if(be.getFixture().invertPan()){
                pan *= -1;
            }
            if (be.isUpsideDown()) {
                if (direction.method_10166() == class_2350.class_2351.field_11048) {
                    pan = (direction.method_10153().method_10144() + be.getPan());
                } else {
                    pan = (direction.method_10144() + be.getPan());
                }
            }
            return BaseLightBlockEntity.calculateViewVector(tilt, pan);
        } else {
            // Kindly put together with help from @Hekera & @Mikey
            class_2350 opposite = hangDirection.method_10153();
            int step = opposite.method_10171().method_10181();
            float offset = 0;
            float toRad = 3.14159F / 180;
            float pan = be.getBasePan() + be.getPan();
            float tilt = be.getTilt();
            pan *= step * toRad;
            tilt *= -step * toRad;
            float sinPan = class_3532.method_15374(pan + offset);
            float cosPan = class_3532.method_15362(pan + offset);
            float cosTilt = class_3532.method_15362(tilt);
            float x = sinPan * cosTilt;
            float y = class_3532.method_15374(tilt);
            float z = cosPan * cosTilt;
            class_2335 cycle = class_2335.field_10960[(opposite.method_10166().ordinal() + 2) % class_2335.field_10960.length];
            return new class_243(cycle.method_35819(x, y, z, class_2350.class_2351.field_11048), cycle.method_35819(x, y, z, class_2350.class_2351.field_11052), cycle.method_35819(x, y, z, class_2350.class_2351.field_11051));
        }
    }

    public double doRayTrace() {
        class_243 viewVector = BaseLightBlockEntity.rayTraceDir(this);
        double distance = getMaxLightDistance();
        class_243 vec3 = method_11016().method_46558();
        class_243 vec33 = vec3.method_1031(viewVector.field_1352 * distance, viewVector.field_1351 * distance, viewVector.field_1350 * distance);
        class_3959 context = new class_3959(vec3, vec33, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, null);
        ((ClipContextAccessor) context).setCollisionContext(new LightCollisionContext(method_11016()));
        class_3965 result = this.field_11863.method_17742(context);
        class_2338 lightPos = result.method_17777();
        if (result.method_17783() != class_239.class_240.field_1333 && !result.method_17781()) {
            distance = result.method_17784().method_1022(vec3);
            if (!result.method_17777().equals(method_11016())) {
                lightPos = result.method_17777().method_10079(result.method_17780(), 1);
            }
        }
        distance = new class_243(lightPos.method_10263(), lightPos.method_10264(), lightPos.method_10260()).method_1022(new class_243(method_11016().method_10263(), method_11016().method_10264(), method_11016().method_10260()));
        emissionBlock = lightPos;
        return distance;
    }

    @Override
    public void method_11012() {
        if(emissionBlock != null){
            this.setLightEnabled(false);
            emissionBlock = null;
        }
        super.method_11012();
    }

    public void setTilt(int tilt){
        this.prevTilt = this.tilt;
        this.tilt = tilt;
    }

    public void setPan(int pan){
        this.prevPan = this.pan;
        this.pan = pan;
    }

    @Override
    public int getLightLuminance() {
        float newVal = intensity / 255f;
        return (int) (newVal * 15f);
    }

    @Override
    public Vector3f getLightPos() {
        return class_243.method_24953(emissionBlock).method_46409();
    }

    @Override
    public boolean isLightEnabled() {
        return DynamicLightProvider.super.isLightEnabled();
    }

    @Override
    public void resetLight() {

    }

    @Override
    public void lightTick() {

    }

    @Override
    public boolean shouldUpdateLight() {
        return LightManager.shouldUpdateDynamicLight() && emitsLight() && emissionBlock != null;
    }

    @Override
    public boolean updateDynamicLight(class_761 renderer) {
        if (!this.shouldUpdateLight())
            return false;
        return LightManager.updateDynamicLight(this, renderer);
    }

    @Override
    public void scheduleTrackedChunksRebuild(class_761 renderer) {
        if (class_310.method_1551().field_1687 == this.field_11863) {
            for (long pos : this.trackedLitChunkPos) {
                LightManager.scheduleChunkRebuild(renderer, pos);
            }
        }
    }

    @Override
    public class_2338 getOwnerPos() {
        return method_11016();
    }

    @Override
    public int getLightColour() {
        return ((int)getIntensity() << 24) | getColour();
    }

    public float getPrevSpread() {
        return prevSpread;
    }

    public void setPrevSpread(float prevSpread) {
        this.prevSpread = prevSpread;
    }

    @Override
    public float getLightSpread() {
        float focus = (getFocus() / 255f);
        float minRadius = 1;
        float maxRadius = (float) getFixture().getLightRadius();
        float clampedSpread = class_3532.method_15363(focus, 0.05f, 1.0f);
        return class_3532.method_16439(clampedSpread, minRadius, maxRadius);
    }

    @Override
    public class_1937 getLightWorld() {
        return method_10997();
    }
}
