package dev.imabad.theatrical.blockentities.light;

import ch.bildspur.artnet.rdm.RDMDeviceId;
import dev.imabad.theatrical.Constants;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import dev.imabad.theatrical.dmx.DMXNetwork;
import dev.imabad.theatrical.dmx.DMXNetworkData;
import dev.imabad.theatrical.util.RndUtils;
import dev.imabad.theatrical.util.UUIDUtil;
import java.util.Random;
import java.util.UUID;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2591;
import net.minecraft.class_2680;

public abstract class BaseDMXConsumerLightBlockEntity extends BaseLightBlockEntity implements DMXConsumer {

    private int channelCount, channelStartPoint, dmxUniverse;
    private RDMDeviceId deviceId;
    private UUID networkId = UUIDUtil.NULL;

    public BaseDMXConsumerLightBlockEntity(class_2591<?> blockEntityType, class_2338 blockPos, class_2680 blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public void write(class_2487 compoundTag) {
        super.write(compoundTag);
        compoundTag.method_10569("channelCount", channelCount);
        compoundTag.method_10569("channelStartPoint", channelStartPoint);
        compoundTag.method_10569("dmxUniverse", dmxUniverse);
        if(deviceId != null) {
            compoundTag.method_10570("deviceId", deviceId.toBytes());
        }
        compoundTag.method_25927("network", networkId);
    }

    @Override
    public void read(class_2487 compoundTag) {
        super.read(compoundTag);
        channelCount = compoundTag.method_10550("channelCount");
        channelStartPoint = compoundTag.method_10550("channelStartPoint");
        if(compoundTag.method_10545("dmxUniverse")){
            dmxUniverse = compoundTag.method_10550("dmxUniverse");
        }
        if(compoundTag.method_10545("deviceId")){
            deviceId = new RDMDeviceId(compoundTag.method_10547("deviceId"));
        }
        if(compoundTag.method_10545("network")){
            networkId = compoundTag.method_25926("network");
        }
    }

    private void generateDeviceId(){
        byte[] bytes = new byte[4];
        if(field_11863 != null) {
            RndUtils.nextBytes(field_11863.method_8409(), bytes);
        } else {
            new Random().nextBytes(bytes);
        }
        deviceId = new RDMDeviceId(Constants.MANUFACTURER_ID, bytes);
        method_5431();
        field_11863.method_8413(method_11016(), method_11010(), method_11010(), class_2248.field_31028);
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public int getChannelStart() {
        return channelStartPoint;
    }

    @Override
    public int getUniverse() {
        return dmxUniverse;
    }

    @Override
    public RDMDeviceId getDeviceId() {
        return deviceId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setUniverse(int dmxUniverse) {
        if(this.dmxUniverse == dmxUniverse){
            return;
        }
        removeConsumer();
        this.dmxUniverse = dmxUniverse;
        addConsumer();
        method_5431();
        field_11863.method_8413(method_11016(), method_11010(), method_11010(), class_2248.field_31028);
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public void setChannelStartPoint(int channelStartPoint) {
        if(this.channelStartPoint == channelStartPoint){
            return;
        }
        this.channelStartPoint = channelStartPoint;
        updateConsumer();
        method_5431();
        field_11863.method_8413(method_11016(), method_11010(), method_11010(), class_2248.field_31028);
    }
    private void updateConsumer(){
        var dmxData = DMXNetworkData.getInstance(field_11863.method_8503().method_30002()).getNetwork(networkId);
        if (dmxData != null) {
            dmxData.updateConsumer(this);
        }
    }
    private void removeConsumer(){
        var dmxData = DMXNetworkData.getInstance(field_11863.method_8503().method_30002()).getNetwork(networkId);
        if (dmxData != null) {
            dmxData.removeConsumer(this, method_11016());
        }
    }
    private void addConsumer(){
        var dmxData = DMXNetworkData.getInstance(field_11863.method_8503().method_30002()).getNetwork(networkId);
        if (dmxData != null) {
            if(deviceId == null){
                generateDeviceId();
            }
            dmxData.addConsumer(method_11016(), this);
        }
    }

    @Override
    public void method_31662(class_1937 level) {
        super.method_31662(level);
        if(level != null && !level.field_9236) {
            addConsumer();
        }
    }

    @Override
    public void method_11012() {
        if(field_11863 != null && !field_11863.field_9236) {
            removeConsumer();
        }
        super.method_11012();
    }

    public void setNetworkId(UUID networkId) {
        if(networkId == this.networkId){
            return;
        }
        removeConsumer();
        this.networkId = networkId;
        addConsumer();
        method_5431();
        field_11863.method_8413(method_11016(), method_11010(), method_11010(), class_2248.field_31028);
    }
}
