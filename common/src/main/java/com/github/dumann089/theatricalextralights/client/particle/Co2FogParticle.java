package com.github.dumann089.theatricalextralights.client.particle;

import com.github.dumann089.theatricalextralights.firework.FireworkRenderDistances;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Brouillard CO2 : une bouffee de gaz froid poussee par le jet, freinee par l'air, qui grossit en se
 * diluant puis retombe un peu (le CO2 est plus lourd que l'air). Partage par la colonne dense du
 * coeur et les volutes du panache.
 *
 * <p>Reference : photos de jets CO2 de scene. Colonne quasi opaque, blanche a la sortie, ombree de
 * gris sur son flanc non eclaire, detente eclair a la buse (un demi-bloc de large en quelques
 * dizaines de centimetres) puis cone de 6 degres, tete dechiquetee qui se dilue en volutes.
 */
@Environment(EnvType.CLIENT)
abstract class Co2FogParticle extends TextureSheetParticle {
    /** La fumee est eclairee par la scene : jamais noire dans une salle sombre. */
    private static final int MIN_BLOCK_LIGHT = 13;
    /** Fondu quand la camera entre dans une volute, sinon les sprites coupes par le plan proche clignotent. */
    private static final float CAMERA_FADE_START = 0.3f;
    private static final float CAMERA_FADE_LENGTH = 1.1f;
    private static final float SINK = 0.0010f;
    /** Etirement du sprite le long de la vitesse : a 0.85 bloc/tick la trainee fait 4 fois sa largeur. */
    private static final float STRETCH_PER_SPEED = 3.5f;
    /** Contre une surface, la fumee s'etale et se dilue vite. */
    private static final float SURFACE_FADE = 0.88f;
    /** Ombrage de volume : luminosite du flanc oppose a la lumiere (camera + dessus). */
    private static final float SHADE_DARK = 0.58f;
    /** Detente eclair a la sortie : la bouffee atteint sa taille de detente en ~2 ticks. */
    private static final float FLASH_RATE = 0.5f;

    private final float startSize;
    private final float flashSize;
    private final float peakSize;
    private final float baseAlpha;
    private final float drag;
    private final float turbulence;
    private final float shadeStrength;
    private final float spin;
    /** Direction radiale de la bouffee par rapport a l'axe du jet, pour l'ombrage de volume. */
    private final float lateralX;
    private final float lateralY;
    private final float lateralZ;
    private boolean collided;
    private float surfaceFade = 1f;

    protected Co2FogParticle(
            ClientLevel level,
            Vec3 pos,
            Vec3 velocity,
            Vector3f axis,
            SpriteSet sprites,
            RandomSource random,
            float startSize,
            float flashSize,
            float peakSize,
            float baseAlpha,
            float drag,
            float turbulence,
            float shadeStrength,
            int lifetime
    ) {
        super(level, pos.x, pos.y, pos.z);
        xd = velocity.x;
        yd = velocity.y;
        zd = velocity.z;
        hasPhysics = true;
        gravity = 0f;
        this.lifetime = Math.max(2, lifetime);
        float distanceScale = FireworkRenderDistances.flameParticleSizeScale(pos.x, pos.y, pos.z);
        this.startSize = startSize * distanceScale;
        this.flashSize = flashSize * distanceScale;
        this.peakSize = peakSize * distanceScale;
        this.baseAlpha = baseAlpha;
        this.drag = drag;
        this.turbulence = turbulence;
        this.shadeStrength = shadeStrength;
        quadSize = this.startSize;
        alpha = 0f;
        float tint = 0.96f + random.nextFloat() * 0.04f;
        rCol = tint;
        gCol = tint;
        bCol = 1f;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        spin = (random.nextFloat() - 0.5f) * 0.06f;

        // Composante de la vitesse perpendiculaire a l'axe : de quel cote du panache est la bouffee.
        double along = velocity.x * axis.x() + velocity.y * axis.y() + velocity.z * axis.z();
        double lx = velocity.x - axis.x() * along;
        double ly = velocity.y - axis.y() * along;
        double lz = velocity.z - axis.z() * along;
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (len < 1.0e-5) {
            Vec3 helper = Math.abs(axis.y()) < 0.9f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 perp = new Vec3(axis.x(), axis.y(), axis.z()).cross(helper).normalize();
            double angle = random.nextDouble() * Mth.TWO_PI;
            Vec3 perp2 = new Vec3(axis.x(), axis.y(), axis.z()).cross(perp).normalize();
            Vec3 lateral = perp.scale(Math.cos(angle)).add(perp2.scale(Math.sin(angle)));
            lx = lateral.x;
            ly = lateral.y;
            lz = lateral.z;
        } else {
            lx /= len;
            ly /= len;
            lz /= len;
        }
        lateralX = (float) lx;
        lateralY = (float) ly;
        lateralZ = (float) lz;
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ExtraLightsRenderTypes.co2JetRenderType();
    }

    @Override
    public int getLightColor(float partialTick) {
        int packed = super.getLightColor(partialTick);
        return LightTexture.pack(Math.max(LightTexture.block(packed), MIN_BLOCK_LIGHT), LightTexture.sky(packed));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float life = (float) age / (float) lifetime;

        xd *= drag;
        yd *= drag;
        zd *= drag;
        float jitter = turbulence * (0.35f + life);
        xd += (random.nextFloat() - 0.5f) * jitter;
        yd += (random.nextFloat() - 0.5f) * jitter;
        zd += (random.nextFloat() - 0.5f) * jitter;
        yd -= SINK * life;
        move(xd, yd, zd);
        if (collided) {
            xd *= 0.82;
            zd *= 0.82;
            surfaceFade *= SURFACE_FADE;
        }

        roll += spin * (1f - 0.5f * life);
        // Detente eclair a la buse, puis la taille suit la distance parcourue (1 - drag^age) : le cone.
        float flash = 1f - (float) Math.pow(FLASH_RATE, age);
        float travel = 1f - (float) Math.pow(drag, age);
        quadSize = startSize + (flashSize - startSize) * flash + (peakSize - flashSize) * travel;

        // Dilution : la meme quantite de gaz etalee sur une bouffee plus grande est plus transparente.
        float dilution = Math.min(1f, (float) Math.pow(flashSize / quadSize, 1.2));
        float fadeIn = Math.min(1f, age / 2f);
        float fadeOut = 1f;
        if (life > 0.5f) {
            float t = (life - 0.5f) / 0.5f;
            fadeOut = 1f - t * t;
        }
        alpha = baseAlpha * dilution * fadeIn * fadeOut * surfaceFade;

        long gameTime = level.getGameTime();
        float dissipation = Flow2JetDissipation.alphaMultiplier(x, y, z, gameTime);
        if (dissipation < 1f) {
            alpha *= dissipation;
            float damp = Flow2JetDissipation.motionDamping(dissipation);
            xd *= damp;
            yd *= damp;
            zd *= damp;
            if (dissipation < 0.1f) {
                remove();
                return;
            }
        }
        if (age > 3 && alpha < 0.006f) {
            remove();
        }
    }

    /**
     * Collision blocs sans le gel vanilla : une particule qui bute verticalement reste libre de
     * s'etaler le long de la surface au lieu de se figer sur place.
     */
    @Override
    public void move(double moveX, double moveY, double moveZ) {
        double wantX = moveX;
        double wantY = moveY;
        double wantZ = moveZ;
        if (hasPhysics && (moveX != 0.0 || moveY != 0.0 || moveZ != 0.0)) {
            Vec3 allowed = Entity.collideBoundingBox(null, new Vec3(moveX, moveY, moveZ), getBoundingBox(), level, List.of());
            moveX = allowed.x;
            moveY = allowed.y;
            moveZ = allowed.z;
        }
        if (moveX != 0.0 || moveY != 0.0 || moveZ != 0.0) {
            setBoundingBox(getBoundingBox().move(moveX, moveY, moveZ));
            setLocationFromBoundingbox();
        }
        onGround = wantY != moveY && wantY < 0.0;
        if (wantX != moveX) {
            xd = 0.0;
            collided = true;
        }
        if (wantY != moveY) {
            yd = 0.0;
            collided = true;
        }
        if (wantZ != moveZ) {
            zd = 0.0;
            collided = true;
        }
    }

    /**
     * Sprite etire le long de sa vitesse (le gaz rapide se lit en trainees, pas en boules), ombre
     * selon son cote du panache : clair face a la camera et au-dessus, gris sur le flanc oppose.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, xo, x) - cam.x);
        float py = (float) (Mth.lerp(partialTicks, yo, y) - cam.y);
        float pz = (float) (Mth.lerp(partialTicks, zo, z) - cam.z);
        float distance = Mth.sqrt(px * px + py * py + pz * pz);
        float fade = Mth.clamp((distance - CAMERA_FADE_START) / CAMERA_FADE_LENGTH, 0f, 1f);
        if (fade <= 0f) {
            return;
        }
        float inverseDistance = 1f / Math.max(distance, 1.0e-4f);
        float wx = px * inverseDistance;
        float wy = py * inverseDistance;
        float wz = pz * inverseDistance;

        // Base du quad : axe long le long de la vitesse projetee a l'ecran, sinon repere camera + roll.
        float vx = (float) xd;
        float vy = (float) yd;
        float vz = (float) zd;
        float speed = Mth.sqrt(vx * vx + vy * vy + vz * vz);
        float ax = 0f;
        float ay = 0f;
        float az = 0f;
        float projected = 0f;
        if (speed > 1.0e-4f) {
            float dot = (vx * wx + vy * wy + vz * wz) / speed;
            ax = vx / speed - wx * dot;
            ay = vy / speed - wy * dot;
            az = vz / speed - wz * dot;
            projected = Mth.sqrt(ax * ax + ay * ay + az * az);
        }
        float stretch = 1f + STRETCH_PER_SPEED * speed * projected;
        float sx;
        float sy;
        float sz;
        if (projected > 0.05f && stretch > 1.02f) {
            ax /= projected;
            ay /= projected;
            az /= projected;
            sx = wy * az - wz * ay;
            sy = wz * ax - wx * az;
            sz = wx * ay - wy * ax;
        } else {
            stretch = 1f;
            Vector3f up = camera.getUpVector();
            Vector3f left = camera.getLeftVector();
            float angle = Mth.lerp(partialTicks, oRoll, roll);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            ax = up.x() * cos + left.x() * sin;
            ay = up.y() * cos + left.y() * sin;
            az = up.z() * cos + left.z() * sin;
            sx = left.x() * cos - up.x() * sin;
            sy = left.y() * cos - up.y() * sin;
            sz = left.z() * cos - up.z() * sin;
        }

        // Lumiere : depuis la camera, relevee vers le haut.
        float lx = -wx;
        float ly = -wy + 0.6f;
        float lz = -wz;
        float lightLen = Mth.sqrt(lx * lx + ly * ly + lz * lz);
        float lit = (lateralX * lx + lateralY * ly + lateralZ * lz) / lightLen;
        float dark = 1f - (1f - SHADE_DARK) * shadeStrength;
        float shade = dark + (1f - dark) * (0.5f + 0.5f * lit);
        float r = rCol * shade;
        float g = gCol * shade;
        float b = bCol * shade;

        float half = getQuadSize(partialTicks);
        float along = half * stretch;
        float a = alpha * fade / Mth.sqrt(stretch);
        int light = getLightColor(partialTicks);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        vertex(buffer, px - ax * along - sx * half, py - ay * along - sy * half, pz - az * along - sz * half, u1, v1, r, g, b, a, light);
        vertex(buffer, px - ax * along + sx * half, py - ay * along + sy * half, pz - az * along + sz * half, u1, v0, r, g, b, a, light);
        vertex(buffer, px + ax * along + sx * half, py + ay * along + sy * half, pz + az * along + sz * half, u0, v0, r, g, b, a, light);
        vertex(buffer, px + ax * along - sx * half, py + ay * along - sy * half, pz + az * along - sz * half, u0, v1, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, float r, float g, float b, float a, int light) {
        buffer.vertex(x, y, z).uv(u, v).color(r, g, b, a).uv2(light).endVertex();
    }
}
