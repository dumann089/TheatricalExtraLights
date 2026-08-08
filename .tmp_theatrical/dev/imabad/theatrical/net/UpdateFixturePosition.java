package dev.imabad.theatrical.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import net.minecraft.class_2338;
import net.minecraft.class_2540;
import net.minecraft.class_2586;

public class UpdateFixturePosition extends BaseC2SMessage {

    private class_2338 pos;
    private int tilt,pan;

    public UpdateFixturePosition(class_2338 blockPos, int tilt, int pan){
        this.pos = blockPos;
        this.tilt = tilt;
        this.pan = pan;
    }

    UpdateFixturePosition(class_2540 buf){
        pos = buf.method_10811();
        tilt = buf.readInt();
        pan = buf.readInt();
    }

    @Override
    public MessageType getType() {
        return TheatricalNet.UPDATE_FIXTURE_POS;
    }

    @Override
    public void write(class_2540 buf) {
        buf.method_10807(pos);
        buf.writeInt(tilt);
        buf.writeInt(pan);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        class_2586 be = context.getPlayer().method_37908().method_8321(pos);
        if(be instanceof BaseLightBlockEntity baseLightBlockEntity){
            baseLightBlockEntity.setPan(pan);
            baseLightBlockEntity.setTilt(tilt);
        }
    }
}
