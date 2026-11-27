package com.github.dumann089.theatricalextralights.items;

import com.github.dumann089.theatricalextralights.entities.BroadcastCameraEntity;
import com.github.dumann089.theatricalextralights.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Coloca uma câmera de broadcast no chão; a lente aponta para longe de quem a colocou. */
public class BroadcastCameraItem extends Item {

    private final Supplier<? extends EntityType<? extends BroadcastCameraEntity>> entityType;

    public BroadcastCameraItem(Properties properties) {
        this(properties, ModEntities.BROADCAST_CAMERA);
    }

    /** Usado também pela jib camera: mesmo item, outro tipo de entidade. */
    public BroadcastCameraItem(Properties properties,
                               Supplier<? extends EntityType<? extends BroadcastCameraEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = new BlockPlaceContext(context).getClickedPos();
        Vec3 at = Vec3.atBottomCenterOf(pos);
        BroadcastCameraEntity camera = entityType.get().create(serverLevel);
        if (camera == null) {
            return InteractionResult.FAIL;
        }
        camera.moveTo(at.x, at.y, at.z, context.getHorizontalDirection().toYRot(), 0f);
        if (!serverLevel.noCollision(camera)) {
            return InteractionResult.FAIL;
        }
        serverLevel.addFreshEntity(camera);
        serverLevel.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, at);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
