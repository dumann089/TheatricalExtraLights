package com.github.dumann089.theatricalextralights.blockentities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Configuração da mesa de corte: tecla de cada câmera, teclas dos modos (corte seco / com fade) e a
 * duração do fade. As teclas são códigos GLFW (-1 = sem tecla); as câmeras são identificadas pelo UUID.
 */
public class VideoSwitcherConfig {

    public static final int MAX_BINDINGS = 16;
    public static final int MIN_FADE_SECONDS = 1;
    public static final int MAX_FADE_SECONDS = 10;
    /** GLFW_KEY_C e GLFW_KEY_F (literais para não depender do LWJGL no servidor). */
    public static final int DEFAULT_CUT_KEY = 67;
    public static final int DEFAULT_FADE_KEY = 70;

    public static final class Binding {
        public int key;
        public UUID camera;
        public String label;

        public Binding(int key, UUID camera, String label) {
            this.key = key;
            this.camera = camera;
            this.label = label;
        }
    }

    public final List<Binding> bindings = new ArrayList<>();
    public int cutKey = DEFAULT_CUT_KEY;
    public int fadeKey = DEFAULT_FADE_KEY;
    public int fadeSeconds = 2;
    /** Modo atual: false = corte seco, true = corte com fade. */
    public boolean fadeMode;
    /** Proporção da imagem da mesa: 0 = a de cada câmera, 1 = 16:9, 2 = 4:3. */
    public int aspect;

    public Binding bindingForCamera(UUID camera) {
        for (Binding binding : bindings) {
            if (binding.camera.equals(camera)) {
                return binding;
            }
        }
        return null;
    }

    public Binding bindingForKey(int key) {
        if (key < 0) {
            return null;
        }
        for (Binding binding : bindings) {
            if (binding.key == key) {
                return binding;
            }
        }
        return null;
    }

    /** Uma tecla só serve a uma função: solta ela de qualquer outro uso. */
    private void releaseKey(int key) {
        if (key < 0) {
            return;
        }
        bindings.removeIf(binding -> binding.key == key);
        if (cutKey == key) {
            cutKey = -1;
        }
        if (fadeKey == key) {
            fadeKey = -1;
        }
    }

    /** Vincula a tecla à câmera (key -1 desvincula). */
    public void bindCamera(UUID camera, String label, int key) {
        bindings.removeIf(binding -> binding.camera.equals(camera));
        if (key >= 0 && bindings.size() < MAX_BINDINGS) {
            releaseKey(key);
            bindings.add(new Binding(key, camera, label));
        }
    }

    public void setCutKey(int key) {
        releaseKey(key);
        cutKey = key;
    }

    public void setFadeKey(int key) {
        releaseKey(key);
        fadeKey = key;
    }

    public void changeFadeSeconds(int delta) {
        fadeSeconds = Mth.clamp(fadeSeconds + delta, MIN_FADE_SECONDS, MAX_FADE_SECONDS);
    }

    public VideoSwitcherConfig copy() {
        VideoSwitcherConfig copy = new VideoSwitcherConfig();
        for (Binding binding : bindings) {
            copy.bindings.add(new Binding(binding.key, binding.camera, binding.label));
        }
        copy.cutKey = cutKey;
        copy.fadeKey = fadeKey;
        copy.fadeSeconds = fadeSeconds;
        copy.fadeMode = fadeMode;
        copy.aspect = aspect;
        return copy;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Binding binding : bindings) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Key", binding.key);
            entry.putUUID("Camera", binding.camera);
            entry.putString("Label", binding.label == null ? "" : binding.label);
            list.add(entry);
        }
        tag.put("Bindings", list);
        tag.putInt("CutKey", cutKey);
        tag.putInt("FadeKey", fadeKey);
        tag.putInt("FadeSeconds", fadeSeconds);
        tag.putBoolean("FadeMode", fadeMode);
        tag.putInt("Aspect", aspect);
    }

    /** Lê (e saneia: o servidor não confia no que o cliente manda). */
    public void load(CompoundTag tag) {
        bindings.clear();
        ListTag list = tag.getList("Bindings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && bindings.size() < MAX_BINDINGS; i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Camera")) {
                continue;
            }
            UUID camera = entry.getUUID("Camera");
            int key = entry.getInt("Key");
            String label = entry.getString("Label");
            if (key >= 0 && bindingForKey(key) == null && bindingForCamera(camera) == null) {
                bindings.add(new Binding(key, camera, label.length() > 64 ? label.substring(0, 64) : label));
            }
        }
        cutKey = tag.contains("CutKey") ? tag.getInt("CutKey") : DEFAULT_CUT_KEY;
        fadeKey = tag.contains("FadeKey") ? tag.getInt("FadeKey") : DEFAULT_FADE_KEY;
        fadeSeconds = Mth.clamp(tag.contains("FadeSeconds") ? tag.getInt("FadeSeconds") : 2,
                MIN_FADE_SECONDS, MAX_FADE_SECONDS);
        fadeMode = tag.getBoolean("FadeMode");
        aspect = Mth.clamp(tag.getInt("Aspect"), 0, 2);
    }
}
