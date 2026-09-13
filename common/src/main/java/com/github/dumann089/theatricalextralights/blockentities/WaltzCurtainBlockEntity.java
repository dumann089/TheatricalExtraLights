package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

public class WaltzCurtainBlockEntity extends ExtraLightsLightBlockEntity
        implements HasJetHeight, HasJetThickness {

    public double smoothedHeight = 0.0;

    private float jetHeight = 9.0f;
    private float jetThickness = 0.1f;

    public static final float MIN_THICKNESS = 0.05f;
    public static final float MAX_THICKNESS = 9.5f;
    public static final float MIN_JET_HEIGHT = 0.1f;
    public static final float MAX_JET_HEIGHT = 99.0f;

    /** Ángulo actual (suavizado) — fuente única de verdad para modelo y partículas. */
    public float currentAngle = 0;

    /** Acumulador de tiempo para el modo de oscilación automática. */
    public float swayTime = 0;

    public WaltzCurtainBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.WALTZ_CURTAIN.get(), pos, state);
        setChannelCount(3);
    }

    // -------------------
    // GETTERS / SETTERS
    // -------------------

    @Override
    public float getJetThickness() {
        return jetThickness;
    }

    @Override
    public void setJetThickness(float thickness) {
        this.jetThickness = Mth.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    public float prevAngle = 0;

    /** Valor DMX del sistema Swing (Canal 3). */
    public int swingChannel = 0;

    // Método para interpolar el ángulo en el renderizador
    public float getRenderAngle(float partialTicks) {
        return prevAngle + (currentAngle - prevAngle) * partialTicks;
    }

    @Override
    public float getJetHeight() {
        return jetHeight;
    }

    @Override
    public void setJetHeight(float h) {
        this.jetHeight = Mth.clamp(h, MIN_JET_HEIGHT, MAX_JET_HEIGHT);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    // -------------------
    // DMX
    // -------------------

    public float currentSwing = 0;

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;

        // 1. Evitamos errores de fuera de rango, pero no limitamos la lectura por getChannelCount()
        if (dmxValues == null || start >= dmxValues.length) return;

        // Guardamos el valor previo de nuestro canal personalizado
        int prevSwing = this.swingChannel;
        this.dmxTimeoutCounter = TIMEOUT_LIMIT;

        // 2. Leemos los canales directamente del universo DMX de forma segura (& 0xFF convierte a int)
        if (start < dmxValues.length) {
            this.intensity = dmxValues[start] & 0xFF;        // Canal 1: Dimmer
        }
        if (start + 1 < dmxValues.length) {
            this.tilt = dmxValues[start + 1] & 0xFF;         // Canal 2: Tilt manual
        }
        if (start + 2 < dmxValues.length) {
            this.swingChannel = dmxValues[start + 2] & 0xFF; // Canal 3: Velocidad del Swing
        }

        // 3. storePrev() revisa los cambios de la clase base (intensidad y tilt)
        boolean baseChanged = storePrev();
        boolean swingChanged = (prevSwing != this.swingChannel);

        // 4. Si CUALQUIER canal cambió (incluyendo el nuestro), forzamos la actualización visual
        if (baseChanged || swingChanged) {
            if (level != null && !level.isClientSide) {
                BlockEntitySync.sendData(this);
            }
            setChanged();
        }
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    // -------------------
    // TICK — Lógica de movimiento
    // -------------------

    public int dmxTimeoutCounter = 0;
    private static final int TIMEOUT_LIMIT = 20; // 1 segundo aprox (si 20 ticks = 1 seg)

    public void tick() {
        if (this.level.isClientSide) {
            // Lógica de detección de pérdida de señal
            if (dmxTimeoutCounter > 0) {
                dmxTimeoutCounter--;
            } else if (dmxTimeoutCounter == 0 && this.getTilt() != 128) {
                // Si el contador llega a 0, forzamos a 128 (la posición central)
                this.setTilt(128);
            }
        }
    }

    // Llama a esto cada vez que recibas un valor DMX válido desde tu consola
    public void onDmxReceived() {
        this.dmxTimeoutCounter = TIMEOUT_LIMIT;
    }

    // -------------------
    // FIXTURE OVERRIDES
    // -------------------

    @Override
    public Fixture getFixture() {
        return Fixtures.WALTZ_CURTAIN.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Vase Water Jet";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.WALTZ_CURTAIN.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    // -------------------
    // NBT
    // -------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        tag.putInt("SwingChannel", swingChannel); // Guardamos estado del swing
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("JetHeight")) jetHeight = tag.getFloat("JetHeight");
        if (tag.contains("JetThickness")) jetThickness = tag.getFloat("JetThickness");
        if (tag.contains("SwingChannel")) swingChannel = tag.getInt("SwingChannel");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        tag.putInt("SwingChannel", swingChannel); // Sincroniza al cliente inicial
        return tag;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.WaltzesWaterJet";
    }
}