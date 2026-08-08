package dev.imabad.theatrical.api;

import dev.imabad.theatrical.lighting.LightManager;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_761;
import org.joml.Vector3f;

public interface DynamicLightProvider {

    class_2338 getOwnerPos();
    Vector3f getLightPos();
    class_1937 getLightWorld();
    default boolean isLightEnabled() {
        return LightManager.containsLightSource(this);
    }
    default void setLightEnabled(boolean enabled) {
        resetLight();
        if(enabled){
            LightManager.addLightSource(this);
        } else {
            LightManager.removeLightSource(this);
        }
    }
    void resetLight();
    int getLightLuminance();
    void lightTick();
    boolean shouldUpdateLight();
    boolean updateDynamicLight(class_761 renderer);
    void scheduleTrackedChunksRebuild(class_761 renderer);
    int getLightColour();
    float getLightSpread();
}
