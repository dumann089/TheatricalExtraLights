package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasExtendedBeamChannels;
import com.github.dumann089.theatricalextralights.client.StrobeRenderHelper;
import com.github.dumann089.theatricalextralights.util.FixtureMountTransform;
import com.github.dumann089.theatricalextralights.util.FollowspotDmxHelper;
import com.github.dumann089.theatricalextralights.util.TheatricalDmxFrameBridge;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base DMX Extra Lights : préserve tous les prev* au sync client et les avance côté client
 * à chaque tick (évite clignotement pan/tilt/intensité et désync jusqu'au clic).
 */
public abstract class ExtraLightsLightBlockEntity extends BaseDMXConsumerLightBlockEntity {

    private float mountOffsetX;
    private float mountOffsetY;
    private float mountOffsetZ;
    private float mountYaw;
    private float mountPitch;
    private float mountRoll;

    protected ExtraLightsLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public float getMountOffsetX() {
        return mountOffsetX;
    }

    public float getMountOffsetY() {
        return mountOffsetY;
    }

    public float getMountOffsetZ() {
        return mountOffsetZ;
    }

    public float getMountYaw() {
        return mountYaw;
    }

    public float getMountPitch() {
        return mountPitch;
    }

    public float getMountRoll() {
        return mountRoll;
    }

    public boolean hasMountTransform() {
        return mountOffsetX != 0.0F
                || mountOffsetY != 0.0F
                || mountOffsetZ != 0.0F
                || mountYaw != 0.0F
                || mountPitch != 0.0F
                || mountRoll != 0.0F;
    }

    public void setMountTransform(float offsetX, float offsetY, float offsetZ,
                                  float yaw, float pitch, float roll) {
        mountOffsetX = FixtureMountTransform.clampOffset(offsetX);
        mountOffsetY = FixtureMountTransform.clampOffset(offsetY);
        mountOffsetZ = FixtureMountTransform.clampOffset(offsetZ);
        mountYaw = FixtureMountTransform.clampAngle(yaw);
        mountPitch = FixtureMountTransform.clampAngle(pitch);
        mountRoll = FixtureMountTransform.clampAngle(roll);
    }

    public void resetMountTransform() {
        setMountTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public void syncMountTransformToClients() {
        if (level == null || level.isClientSide) {
            return;
        }
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void writeMountTransform(CompoundTag tag) {
        if (hasMountTransform()) {
            tag.putFloat("MountOffsetX", mountOffsetX);
            tag.putFloat("MountOffsetY", mountOffsetY);
            tag.putFloat("MountOffsetZ", mountOffsetZ);
            tag.putFloat("MountYaw", mountYaw);
            tag.putFloat("MountPitch", mountPitch);
            tag.putFloat("MountRoll", mountRoll);
        }
    }

    private void readMountTransform(CompoundTag tag) {
        mountOffsetX = tag.contains("MountOffsetX") ? tag.getFloat("MountOffsetX")
                : tag.contains("mountOffsetX") ? tag.getFloat("mountOffsetX") : 0.0F;
        mountOffsetY = tag.contains("MountOffsetY") ? tag.getFloat("MountOffsetY")
                : tag.contains("mountOffsetY") ? tag.getFloat("mountOffsetY") : 0.0F;
        mountOffsetZ = tag.contains("MountOffsetZ") ? tag.getFloat("MountOffsetZ")
                : tag.contains("mountOffsetZ") ? tag.getFloat("mountOffsetZ") : 0.0F;
        mountYaw = tag.contains("MountYaw") ? tag.getFloat("MountYaw")
                : tag.contains("mountYaw") ? tag.getFloat("mountYaw") : 0.0F;
        mountPitch = tag.contains("MountPitch") ? tag.getFloat("MountPitch")
                : tag.contains("mountPitch") ? tag.getFloat("mountPitch") : 0.0F;
        mountRoll = tag.contains("MountRoll") ? tag.getFloat("MountRoll")
                : tag.contains("mountRoll") ? tag.getFloat("mountRoll") : 0.0F;
    }

    /** Capture les prev* serveur avant lecture DMX. Retourne true si prev* étaient en retard. */
    protected boolean beginDmxUpdate() {
        return storePrev();
    }

    /**
     * DMX channels beyond the standard 7 (e.g. prism / gobo on 10ch personalities) are not
     * included in Theatrical's batched DmxFrame payload — force a block-entity sync so the
     * client renderer sees prism beam count, zoom, and rotation.
     */
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return getChannelCount() > 7;
    }

    /**
     * Sync client si valeurs changées OU si prev* serveur ont rattrapé (pattern Theatrical).
     * Utilise le batch DMXFrame si Theatrical récent est présent, sinon vanilla.
     */
    protected void finishDmxUpdate(boolean valuesChanged, boolean prevAdvanced) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (valuesChanged || prevAdvanced) {
            boolean batchQueued = !hasExtraDmxChannelsBeyondBatch()
                    && TheatricalDmxFrameBridge.markDirtyIfBatchEnabled(getBlockPos());
            if (!batchQueued) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (valuesChanged) {
            setChanged();
        }
    }

    /** Sync pan/tilt for operator mode — keeps prev* aligned to avoid interpolation flicker. */
    public void syncOperatorAngles(float pan, float tilt) {
        int pi = FollowspotDmxHelper.quantizePan(pan);
        int ti = FollowspotDmxHelper.quantizeTilt(tilt);
        setPan(pi);
        setTilt(ti);
        prevPan = pi;
        prevTilt = ti;
    }

    /** Exact server-side values from the console — avoids DMX round-trip drift on pan/tilt. */
    public void applyDirectControl(int intensity, int red, int green, int blue, int focus, int pan, int tilt) {
        if (level == null || level.isClientSide) {
            return;
        }
        int qi = FollowspotDmxHelper.quantizePan(pan);
        int qt = FollowspotDmxHelper.quantizeTilt(tilt);
        boolean changed = (int) this.intensity != intensity
                || this.red != red
                || this.green != green
                || this.blue != blue
                || this.focus != focus
                || this.pan != qi
                || this.tilt != qt;
        if (!changed) {
            return;
        }
        this.intensity = intensity;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.focus = focus;
        this.pan = qi;
        this.tilt = qt;
        prevPan = qi;
        prevTilt = qt;
        prevFocus = focus;
        prevIntensity = intensity;
        prevRed = red;
        prevGreen = green;
        prevBlue = blue;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * Rayon de la tache lumineuse, cale sur la section du cone la ou la lumiere arrive.
     *
     * <p>Theatrical derive ce rayon du seul canal focus, sans tenir compte de la distance : un
     * cone serre eclairant a 60 blocs produisait la meme tache qu'a 3 blocs, alors que le
     * faisceau dessine, lui, s'elargit avec la distance. Les deux tailles ne coincidaient qu'a
     * une distance precise.
     *
     * <p>Ne s'applique qu'aux projecteurs dont le renderer publie un cone, c'est-a-dire les
     * lyres a focus ou a zoom. Pour tous les autres — PAR a cone fixe notamment — aucun cone
     * n'est publie et le comportement d'origine est conserve. Les strobes et blinders
     * surchargent deja cette methode et ne passent pas ici.
     */
    @Override
    public float getLightSpread() {
        float spread = com.github.dumann089.theatricalextralights.client.render.beam.BeamSpotLighting
                .spotRadius(getLevel(), getBlockPos(), getDistance());
        return Float.isNaN(spread) ? super.getLightSpread() : spread;
    }

    /** Pan/tilt only — leaves intensity / RGB / focus to Art-Net / desk software. */
    public void applyDirectPanTilt(int pan, int tilt) {
        if (level == null || level.isClientSide) {
            return;
        }
        int qi = FollowspotDmxHelper.quantizePan(pan);
        int qt = FollowspotDmxHelper.quantizeTilt(tilt);
        if (this.pan == qi && this.tilt == qt) {
            return;
        }
        this.pan = qi;
        this.tilt = qt;
        prevPan = qi;
        prevTilt = qt;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide) {
            if (com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.isControlling(getBlockPos())
                    || com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.shouldPreserveExitAngles(getBlockPos())) {
                return;
            }
            prevPan = pan;
            prevTilt = tilt;
            prevFocus = focus;
            prevIntensity = intensity;
            prevRed = red;
            prevGreen = green;
            prevBlue = blue;
            if (needsContinuousClientRender()) {
                StrobeRenderHelper.markSectionDirty(getBlockPos());
            }
        }
    }

    /**
     * Sodium met en cache le rendu des block entities — les faisceaux/lentilles passent
     * par LazyRenderers depuis {@code beforeRenderBeam}, donc il faut invalider le chunk
     * tant que la fixture est visuellement active.
     */
    protected boolean needsContinuousClientRender() {
        if (intensity > 0) {
            return true;
        }
        if (getChannelCount() > 7 && this instanceof HasExtendedBeamChannels ext) {
            return ext.getGobo() > 0 || ext.getGoboSpin() > 0;
        }
        return false;
    }

    // Point d'entree du DmxFrame etendu de Theatrical. Pas de @Override ni d'appel a super :
    // BaseDMXConsumerLightBlockEntity ne declare ces methodes que dans les builds Theatrical
    // portant DmxFrameExtendedFixture, absente de la derniere version publiee
    // (alpha.28.120). On applique donc les valeurs directement sur les champs protected de
    // BaseLightBlockEntity — meme resultat, et la dependance reste souple comme le veut
    // ExtraLightsMixinPlugin, qui n'ajoute l'interface que lorsque l'API existe.

    public void applyDmxFrameBase(int intensity, int red, int green, int blue,
                                  int prevIntensity, int prevRed, int prevGreen, int prevBlue) {
        this.intensity = intensity;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.prevIntensity = prevIntensity;
        this.prevRed = prevRed;
        this.prevGreen = prevGreen;
        this.prevBlue = prevBlue;
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    public void applyDmxFramePanTiltFocus(int pan, int tilt, int focus,
                                          int prevPan, int prevTilt, int prevFocus) {
        this.pan = pan;
        this.tilt = tilt;
        this.focus = focus;
        this.prevPan = prevPan;
        this.prevTilt = prevTilt;
        this.prevFocus = prevFocus;
        if (level != null && level.isClientSide) {
            StrobeRenderHelper.markSectionDirty(getBlockPos());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeMountTransform(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("prevPan", prevPan);
        tag.putInt("prevTilt", prevTilt);
        tag.putInt("prevFocus", prevFocus);
        writeMountTransform(tag);
    }

    @Override
    public void read(CompoundTag tag) {
        boolean preserveAngles = level != null && level.isClientSide
                && (com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.isControlling(getBlockPos())
                || com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.shouldPreserveExitAngles(getBlockPos()));
        int savedPan = pan;
        int savedTilt = tilt;
        int savedPrevPan = pan;
        int savedPrevTilt = tilt;
        if (preserveAngles) {
            if (com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.isControlling(getBlockPos())) {
                savedPan = pan;
                savedTilt = tilt;
                savedPrevPan = prevPan;
                savedPrevTilt = prevTilt;
            } else {
                savedPan = com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.getExitPan(getBlockPos());
                savedTilt = com.github.dumann089.theatricalextralights.client.followspot.FollowspotFixtureCameraSession.getExitTilt(getBlockPos());
                savedPrevPan = savedPan;
                savedPrevTilt = savedTilt;
            }
        } else {
            savedPrevPan = tag.contains("prevPan") ? tag.getInt("prevPan") : prevPan;
            savedPrevTilt = tag.contains("prevTilt") ? tag.getInt("prevTilt") : prevTilt;
        }
        int savedPrevFocus = tag.contains("prevFocus") ? tag.getInt("prevFocus") : prevFocus;
        int savedPrevIntensity = tag.contains("prevIntensity") ? tag.getInt("prevIntensity") : prevIntensity;
        int savedPrevRed = tag.contains("prevRed") ? tag.getInt("prevRed") : prevRed;
        int savedPrevGreen = tag.contains("prevGreen") ? tag.getInt("prevGreen") : prevGreen;
        int savedPrevBlue = tag.contains("prevBlue") ? tag.getInt("prevBlue") : prevBlue;

        super.read(tag);

        if (preserveAngles) {
            pan = savedPan;
            tilt = savedTilt;
            prevPan = savedPan;
            prevTilt = savedTilt;
        } else {
            prevPan = savedPrevPan;
            prevTilt = savedPrevTilt;
        }
        prevFocus = savedPrevFocus;
        prevIntensity = savedPrevIntensity;
        prevRed = savedPrevRed;
        prevGreen = savedPrevGreen;
        prevBlue = savedPrevBlue;

        readMountTransform(tag);
    }
}
