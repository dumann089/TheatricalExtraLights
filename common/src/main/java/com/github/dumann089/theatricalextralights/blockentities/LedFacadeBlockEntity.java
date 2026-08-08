package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.net.LedFacadeFramesPacket;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseLightBlockEntity;
import dev.imabad.theatrical.dmx.DMXNetwork;
import dev.imabad.theatrical.dmx.DMXNetworkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Façade LED : matrice de pixels de résolution configurable (16²..256²). Chaque pixel dessiné
 * (bit dans {@link #activePixels}) est un LED live piloté par 4 canaux DMX (dimmer/R/G/B).
 * Adressage <b>compact</b> : le k-ième pixel actif (balayage haut-gauche → bas-droite) occupe
 * 4 canaux à partir de {@code baseAddress + k*4}, à cheval sur autant d'univers que nécessaire
 * à partir de {@code baseUniverse}.
 *
 * <p>Le mod de base lie un {@code DMXConsumer} à un seul univers ; on enregistre donc
 * {@link LedFacadeUniverseConsumer} par univers additionnel au même {@code BlockPos}. Les trames
 * brutes captées côté serveur sont diffusées aux clients ({@link LedFacadeFramesPacket}) qui
 * reconstruisent la couleur par pixel pour le rendu.
 */
public class LedFacadeBlockEntity extends ExtraLightsLightBlockEntity {

    public static final int[] RESOLUTIONS = {16, 32, 64, 128, 256};
    public static final int DEFAULT_RESOLUTION = 32; // grille fixe (le bouton règle désormais le lissage)
    public static final int CHANNELS_PER_PIXEL = 4;
    public static final int DMX_CHANNELS_PER_UNIVERSE = 512;

    public static final int MAX_SMOOTHING = 2; // 0 = net, 1 = doux, 2 = très doux

    private int resolution = DEFAULT_RESOLUTION;
    /** Pixels allumés, row-major : index = row * resolution + col. */
    private BitSet activePixels = new BitSet(DEFAULT_RESOLUTION * DEFAULT_RESOLUTION);
    /** Niveau de lissage / anti-aliasing du rendu en jeu (0..MAX_SMOOTHING). */
    private int smoothing = 0;

    // ─── Serveur : trames DMX + sous-consommateurs ────────────────────────────
    private byte[][] universeFrames = new byte[1][];
    private final BitSet changedUniverses = new BitSet();
    private boolean framesDirty;
    private long tickCounter;
    private final List<LedFacadeUniverseConsumer> subConsumers = new ArrayList<>();

    // ─── Client : couleurs par pixel calculées depuis les trames ──────────────
    private int[] pixelColors = new int[0];
    private int colorVersion;

    public LedFacadeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Ne pas appeler setChannelStartPoint ici : la méthode de base déréférence le niveau (null au ctor).
        // channelStart = 0 est traité comme « canal 1 » par l'adressage ; l'éditeur/carte le fixe au runtime.
        updateChannelFootprint();
    }

    public LedFacadeBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntities.LED_FACADE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LedFacadeBlockEntity be) {
        // Logique de base : ray-trace (serveur) + lumière dynamique Shimmer (client).
        BaseLightBlockEntity.tick(level, pos, state, be);
        be.serverTick();
    }

    // ─── État grille ────────────────────────────────────────────────────────

    public int getResolution() {
        return resolution;
    }

    public static boolean isValidResolution(int res) {
        for (int r : RESOLUTIONS) {
            if (r == res) return true;
        }
        return false;
    }

    public void setResolution(int newResolution) {
        if (!isValidResolution(newResolution) || newResolution == resolution) {
            return;
        }
        resolution = newResolution;
        activePixels = new BitSet(resolution * resolution);
        onGridChanged();
    }

    public BitSet getActivePixels() {
        return activePixels;
    }

    public int getDrawnCount() {
        return activePixels.cardinality();
    }

    public boolean hasActivePixels() {
        return !activePixels.isEmpty();
    }

    public boolean isPixelActive(int index) {
        return index >= 0 && index < resolution * resolution && activePixels.get(index);
    }

    public int getSmoothing() {
        return smoothing;
    }

    /** Règle le niveau de lissage du rendu (0=net, 1=doux, 2=très doux). N'efface pas le dessin. */
    public void setSmoothing(int value) {
        int v = Math.max(0, Math.min(MAX_SMOOTHING, value));
        if (v == smoothing) {
            return;
        }
        smoothing = v;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void setActivePixels(BitSet pixels) {
        int cells = resolution * resolution;
        BitSet clamped = pixels.get(0, cells);
        if (clamped.equals(activePixels)) {
            return;
        }
        activePixels = clamped;
        onGridChanged();
    }

    private void onGridChanged() {
        updateChannelFootprint();
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            rebuildSubConsumers();
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } else {
            recomputePixelColors();
        }
    }

    private void updateChannelFootprint() {
        setChannelCount(channelsInUniverse(0));
    }

    // ─── Adressage compact multi-univers ──────────────────────────────────────

    /** Nombre d'univers couverts par le dessin courant (≥ 1). */
    public int universesSpanned() {
        int total = getDrawnCount() * CHANNELS_PER_PIXEL;
        if (total == 0) {
            return 1;
        }
        int lastByte = Math.max(0, getChannelStart() - 1) + total - 1;
        return lastByte / DMX_CHANNELS_PER_UNIVERSE + 1;
    }

    /** Nombre de canaux utilisés dans l'univers d'offset donné (0 = univers de base). */
    public int channelsInUniverse(int offset) {
        int total = getDrawnCount() * CHANNELS_PER_PIXEL;
        int base = Math.max(0, getChannelStart() - 1);
        int uniStart = offset * DMX_CHANNELS_PER_UNIVERSE;
        int lo = Math.max(base, uniStart);
        int hi = Math.min(base + total, uniStart + DMX_CHANNELS_PER_UNIVERSE);
        return Math.max(0, hi - lo);
    }

    // ─── DMX (serveur) ─────────────────────────────────────────────────────────

    /** Univers de base : ce BlockEntity est lui-même le consommateur (via la classe de base). */
    @Override
    public void consume(byte[] dmxValues) {
        storeFrame(0, dmxValues);
    }

    /** Stocke la trame d'un univers (offset). Appelé par {@link #consume} et les sous-consommateurs. */
    public void storeFrame(int offset, byte[] frame) {
        if (level == null || level.isClientSide || offset < 0) {
            return;
        }
        ensureFrameCapacity(offset + 1);
        byte[] buf = new byte[DMX_CHANNELS_PER_UNIVERSE];
        System.arraycopy(frame, 0, buf, 0, Math.min(DMX_CHANNELS_PER_UNIVERSE, frame.length));
        if (!Arrays.equals(buf, universeFrames[offset])) {
            universeFrames[offset] = buf;
            changedUniverses.set(offset);
            framesDirty = true;
        }
    }

    private void ensureFrameCapacity(int size) {
        if (universeFrames.length < size) {
            universeFrames = Arrays.copyOf(universeFrames, size);
        }
    }

    private void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        tickCounter++;
        boolean fullResend = tickCounter % 40 == 0;      // ~2 s : couvre les arrivants tardifs
        boolean sendChanged = framesDirty && tickCounter % 2 == 0; // throttle ~10 Hz
        if (sendChanged || fullResend) {
            broadcastFrames(fullResend);
            framesDirty = false;
        }
    }

    private void broadcastFrames(boolean full) {
        if (!(level instanceof ServerLevel serverLevel) || universeFrames.length == 0) {
            return;
        }
        List<Integer> offsets = new ArrayList<>();
        List<byte[]> frames = new ArrayList<>();
        for (int i = 0; i < universeFrames.length; i++) {
            if (universeFrames[i] != null && (full || changedUniverses.get(i))) {
                offsets.add(i);
                frames.add(universeFrames[i]);
            }
        }
        changedUniverses.clear();
        if (offsets.isEmpty()) {
            return;
        }

        int[] offsetArray = offsets.stream().mapToInt(Integer::intValue).toArray();
        byte[][] frameArray = frames.toArray(new byte[0][]);
        LedFacadeFramesPacket packet = new LedFacadeFramesPacket(getBlockPos(), offsetArray, frameArray);

        double reachSqr = 128.0 * 128.0;
        double cx = getBlockPos().getX() + 0.5, cy = getBlockPos().getY() + 0.5, cz = getBlockPos().getZ() + 0.5;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(cx, cy, cz) <= reachSqr) {
                packet.sendTo(player);
            }
        }
    }

    // ─── Sous-consommateurs (serveur) ──────────────────────────────────────────

    private DMXNetwork network() {
        if (level == null || level.getServer() == null) {
            return null;
        }
        DMXNetworkData data = DMXNetworkData.getInstance(level.getServer().overworld());
        return data == null ? null : data.getNetwork(getNetworkId());
    }

    private void rebuildSubConsumers() {
        if (level == null || level.isClientSide || level.getServer() == null) {
            return;
        }
        removeAllSubConsumers();
        int span = Math.min(universesSpanned(), maxUniverses());
        ensureFrameCapacity(span);
        DMXNetwork net = network();
        if (net == null) {
            return;
        }
        for (int i = 1; i < span; i++) {
            LedFacadeUniverseConsumer sub = new LedFacadeUniverseConsumer(this, i);
            subConsumers.add(sub);
            net.addConsumer(getBlockPos(), sub);
        }
    }

    private void removeAllSubConsumers() {
        DMXNetwork net = network();
        if (net != null) {
            for (LedFacadeUniverseConsumer sub : subConsumers) {
                net.removeConsumer(sub, getBlockPos());
            }
        }
        subConsumers.clear();
    }

    private static int maxUniverses() {
        return Math.max(1, TheatricalExtraLightsConfig.getLedFacadeMaxUniverses());
    }

    // ─── Overrides DMX de base (ré-enregistrent les sous-consommateurs) ─────────

    @Override
    public void setUniverse(int dmxUniverse) {
        removeAllSubConsumers();
        super.setUniverse(dmxUniverse);
        rebuildSubConsumers();
    }

    @Override
    public void setNetworkId(java.util.UUID networkId) {
        removeAllSubConsumers();
        super.setNetworkId(networkId);
        rebuildSubConsumers();
    }

    @Override
    public void setChannelStartPoint(int channelStartPoint) {
        removeAllSubConsumers();
        super.setChannelStartPoint(channelStartPoint);
        updateChannelFootprint();
        rebuildSubConsumers();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level); // la classe de base enregistre CE consommateur (univers de base)
        if (level != null && !level.isClientSide) {
            rebuildSubConsumers(); // enregistre les univers additionnels (activePixels déjà chargés)
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            removeAllSubConsumers();
        }
        super.setRemoved();
    }

    // ─── Couleurs par pixel (client) ────────────────────────────────────────────

    /** Applique les trames reçues et recalcule les couleurs (client). */
    public void applyFrames(int[] offsets, byte[][] frames) {
        int max = universesSpanned();
        for (int off : offsets) {
            if (off + 1 > max) {
                max = off + 1;
            }
        }
        ensureFrameCapacity(max);
        for (int i = 0; i < offsets.length; i++) {
            int off = offsets[i];
            if (off >= 0 && off < universeFrames.length) {
                universeFrames[off] = frames[i];
            }
        }
        recomputePixelColors();
    }

    private void recomputePixelColors() {
        int cells = resolution * resolution;
        int[] colors = new int[cells];
        int base = Math.max(0, getChannelStart() - 1);
        int k = 0;
        long sumR = 0, sumG = 0, sumB = 0;
        int lit = 0, maxDim = 0;
        for (int index = 0; index < cells; index++) {
            if (activePixels.get(index)) {
                int start = base + k * CHANNELS_PER_PIXEL;
                int dimmer = frameByte(start);
                int r = frameByte(start + 1);
                int g = frameByte(start + 2);
                int b = frameByte(start + 3);
                float scale = dimmer / 255f;
                colors[index] = 0xFF000000 | ((int) (r * scale) << 16) | ((int) (g * scale) << 8) | (int) (b * scale);
                if (dimmer > 0 && (r | g | b) != 0) {
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    lit++;
                    if (dimmer > maxDim) {
                        maxDim = dimmer;
                    }
                }
                k++;
            }
        }
        pixelColors = colors;
        colorVersion++;

        // Lumière dynamique agrégée (Shimmer) : couleur moyenne des LED allumées, intensité = dimmer
        // MAX (puissance comparable au panneau RGB), focus plein pour une portée maximale.
        if (lit > 0) {
            red = (int) (sumR / lit);
            green = (int) (sumG / lit);
            blue = (int) (sumB / lit);
            intensity = maxDim;
            focus = 255;
        } else {
            intensity = 0;
            focus = 0;
        }
        prevRed = red;
        prevGreen = green;
        prevBlue = blue;
        prevIntensity = intensity;
        prevFocus = focus;
    }

    private int frameByte(int globalByte) {
        int uni = globalByte / DMX_CHANNELS_PER_UNIVERSE;
        int off = globalByte % DMX_CHANNELS_PER_UNIVERSE;
        if (uni < 0 || uni >= universeFrames.length || universeFrames[uni] == null) {
            return 0;
        }
        return Byte.toUnsignedInt(universeFrames[uni][off]);
    }

    /** Couleur ARGB d'un pixel (0 si éteint / hors bornes). Utilisé par le rendu et l'éditeur. */
    public int getPixelArgb(int index) {
        if (index < 0 || index >= pixelColors.length) {
            return 0;
        }
        return pixelColors[index];
    }

    public int getColorVersion() {
        return colorVersion;
    }

    // ─── Émission lumière (Shimmer) : au bloc DEVANT la façade, pas le long du ray-trace ─────
    // Le ray-trace de base part le long de FACING (à l'opposé du joueur) → la lumière irait
    // derrière/au loin. Une façade éclaire devant elle (côté joueur = FACING.getOpposite()).

    private BlockPos frontBlock() {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        return getBlockPos().relative(facing.getOpposite());
    }

    @Override
    public BlockPos getEmissionBlock() {
        return frontBlock();
    }

    @Override
    public Vector3f getLightPos() {
        return Vec3.atCenterOf(frontBlock()).toVector3f();
    }

    // ─── NBT ────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("resolution", resolution);
        tag.putInt("smoothing", smoothing);
        tag.putByteArray("activePixels", activePixels.toByteArray());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("resolution") && isValidResolution(tag.getInt("resolution"))) {
            resolution = tag.getInt("resolution");
        }
        if (tag.contains("smoothing")) {
            smoothing = Math.max(0, Math.min(MAX_SMOOTHING, tag.getInt("smoothing")));
        }
        if (tag.contains("activePixels")) {
            activePixels = BitSet.valueOf(tag.getByteArray("activePixels"));
        }
        updateChannelFootprint();
        if (level != null && !level.isClientSide) {
            rebuildSubConsumers();
        } else if (level != null) {
            recomputePixelColors();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("resolution", resolution);
        tag.putInt("smoothing", smoothing);
        tag.putByteArray("activePixels", activePixels.toByteArray());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ─── Identité DMXConsumer / Fixture ───────────────────────────────────────

    @Override public Fixture getFixture()        { return Fixtures.LED_FACADE.get(); }
    @Override public ResourceLocation getFixtureId() { return Fixtures.LED_FACADE.getId(); }
    @Override public int getActivePersonality()  { return 0; }
    @Override public int getDeviceTypeId()       { return 0x02; }
    @Override public String getModelName()       { return "LED Facade"; }
    @Override public String getTranslationKey()  { return "block.theatricalextralights.led_facade"; }
}
