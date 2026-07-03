package com.github.dumann089.theatricalextralights.fabric;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsClient;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonClientSetup;
import com.github.dumann089.theatricalextralights.client.ConfettiCannonItemRenderer;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.gobo.GoboSynchronizer;
import com.github.dumann089.theatricalextralights.client.gobo.GobosAtlasManager;
import com.github.dumann089.theatricalextralights.client.model.ConfettiCannonModel;
import com.github.dumann089.theatricalextralights.items.Items;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.architectury.registry.ReloadListenerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.github.dumann089.theatricalextralights.client.gobo.GlobalGoboRegistry;

// ---> IMPORT DEL PUENTE IRL-CORE <---
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.github.dumann089.theatricalextralights.render.light.core.LightManager;
import net.minecraft.server.packs.PackType;
// ------------------------------------

public class TheatricalExtraLightsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Inicialización original

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                // 1. Sincronizamos los archivos primero
                GoboSynchronizer.sync();

                // 2. Cargamos el registro (el tuyo)
                GlobalGoboRegistry.initializeRegistry();

                // 3. FORZAMOS A IRLIGHTS (La parte clave)
                Class<?> cookieArrayClass = Class.forName("org.qualet.irlredactor.light.cookie.CookieArray");

                // Buscamos la instancia estática real que IRLights usa
                Object instance = null;
                for (java.lang.reflect.Field field : cookieArrayClass.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && cookieArrayClass.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        instance = field.get(null);
                        break;
                    }
                }

                // Si no encontramos instancia estática, creamos una (fallback)
                if (instance == null) {
                    instance = cookieArrayClass.getDeclaredConstructor().newInstance();
                }

                // ¡AQUÍ ESTÁ LA MAGIA!
                // Invocamos los métodos de carga en el orden correcto
                // 'load' suele escanear la carpeta, 'reload' sube a la GPU.
                for (String methodName : new String[]{"load", "init", "reload"}) {
                    try {
                        java.lang.reflect.Method m = cookieArrayClass.getDeclaredMethod(methodName);
                        m.setAccessible(true);
                        m.invoke(instance);
                        System.out.println("[TEL] Método " + methodName + " ejecutado exitosamente.");
                    } catch (NoSuchMethodException e) {
                        // Algunos métodos pueden no existir, ignoramos
                    }
                }

            } catch (Exception e) {
                System.err.println("[TEL-ERROR] Fallo al forzar IRLights: " + e.getMessage());
                e.printStackTrace();
            }
        });

        EntityModelLayerRegistry.registerModelLayer(
                ConfettiCannonModel.LAYER_LOCATION,
                ConfettiCannonModel::createBodyLayer
        );
        TheatricalExtraLightsClient.init();
        registerConfettiCannonItemRenderer();

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new GobosAtlasManager());

        com.github.dumann089.theatricalextralights.fabric.FollowspotCameraFabric.init();

        // Registro de los Core Shaders para la GPU
        CoreShaderRegistrationCallback.EVENT.register(context -> {

            // 1. Shader Original del Gobo Projector
            context.register(
                    new ResourceLocation("theatricalextralights", "gobo_projector"),
                    DefaultVertexFormat.POSITION_COLOR,
                    shader -> ModShaders.goboProjectorShader = shader
            );
            // 2. NUEVO: Shader del Volumetric Beam
            // IMPORTANTE: Utiliza POSITION_COLOR_TEX porque enviamos coordenadas UV
            context.register(
                    new ResourceLocation("theatricalextralights", "volumetric_beam"),
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    shader -> ModShaders.volumetricBeamShader = shader
            );

        });

        // =========================================================================================
        // <--- INTEGRACIÓN IRL-CORE: EL BOTÓN DE ENVIAR (FLUSH) --->
        // Este evento se dispara exactamente después de que Minecraft dibuja las entidades.
        // En este punto, tu MovingVL2CBeamsRenderer ya calculó las matrices y guardó los datos en la RAM.
        // =========================================================================================
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            // 1. Forzamos a la GPU a seleccionar tu array de texturas tridimensional
            RenderSystem.setShaderTexture(0, GobosAtlasManager.SHADER_ATLAS_TARGET);

            // 2. Ahora sí, que IRLights lea la GPU (que acaba de recibir tu Array) y dibuje los haces.
            LightManager.getInstance().processFrame();
        });// =========================================================================================
    }

    private static void registerConfettiCannonItemRenderer() {
        BuiltinItemRendererRegistry.INSTANCE.register(
                Items.CONFETTI_CANNON.get(),
                (stack, displayContext, poseStack, buffer, packedLight, packedOverlay) -> getConfettiCannonItemRenderer()
                        .renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay)
        );
    }

    private static ConfettiCannonItemRenderer confettiCannonItemRenderer;

    private static ConfettiCannonItemRenderer getConfettiCannonItemRenderer() {
        if (confettiCannonItemRenderer == null) {
            Minecraft minecraft = Minecraft.getInstance();
            confettiCannonItemRenderer = new ConfettiCannonItemRenderer(
                    minecraft.getBlockEntityRenderDispatcher(),
                    minecraft.getEntityModels()
            );
        }
        return confettiCannonItemRenderer;
    }
}