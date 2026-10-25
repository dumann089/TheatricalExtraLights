package com.github.dumann089.theatricalextralights.client.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.PixelBarBlockEntity;
import com.github.dumann089.theatricalextralights.client.Beam2DRenderTypes;
import com.github.dumann089.theatricalextralights.client.ExtraLightsRenderTypes;
import com.github.dumann089.theatricalextralights.client.ModShaders;
import com.github.dumann089.theatricalextralights.client.StrobeVisualEffects;
import com.github.dumann089.theatricalextralights.client.render.beam.BeamRenderData;
import com.github.dumann089.theatricalextralights.client.render.beam.raymarch.RaymarchBeamRenderer;
import com.github.dumann089.theatricalextralights.config.TheatricalExtraLightsConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.imabad.theatrical.client.LazyRenderers;
import dev.imabad.theatrical.config.TheatricalConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Rendu commun des barres LED a pixels (RGB Bar, Vertical RGB Bar, Moving Bar) : par pixel la
 * cellule eclairee, la LED en point net et additif avec un coeur brule, une lueur douce tournee
 * vers la camera, et une nappe par plage de LED allumees (volumetrique, ou plate sans le
 * volumetrique). La geometrie des pixels vient de {@link #strip()} ; le placement de la tete de
 * {@code preparePoseStack}, comme pour le modele.
 *
 * <p>Une nappe volumetrique porte les couleurs de ses LED : chaque LED peint sa couleur, fois son
 * niveau, dans une rampe d'une ligne que le shader lit le long de la largeur de la nappe. Une
 * seule nappe par plage, donc une seule passe de rendu, avec des bandes de couleur dedans.
 *
 * <p>Des barres du meme type posees bout a bout le long de leurs pixels forment une chaine : la
 * barre de tete dessine les nappes pour toute la chaine, avec les pixels de toutes les barres en
 * une seule rangee, si bien que deux barres voisines allumees fondent leurs faisceaux en une
 * nappe large.
 *
 * <p>Reference : barre LED au sol sur une video de concert, des points brillants dans un ruban de
 * lumiere, et la lumiere d'une barre qui se lit comme une lame plate dans la haze.
 */
public abstract class PixelBarRenderer<T extends PixelBarBlockEntity> extends ExtraLightsFixtureRenderer<T> {

    /**
     * Geometrie des pixels dans le repere local de la tete, apres {@code preparePoseStack}.
     *
     * @param vertical   pixels le long de Y (barre verticale) au lieu de X
     * @param first      centre du premier pixel le long de l'axe, par rapport au centre de la face
     * @param pitch      pas entre deux pixels
     * @param halfAlong  demi-taille d'une cellule le long de l'axe
     * @param halfAcross demi-taille d'une cellule en travers
     * @param dotHalf    demi-taille du point LED, plus petit que la cellule
     * @param faceX      centre de la face
     * @param faceY      centre de la face
     * @param faceZ      face avant, -Z devant
     */
    protected record Strip(boolean vertical, float first, float pitch, float halfAlong, float halfAcross,
                           float dotHalf, float faceX, float faceY, float faceZ) {
        float along(int pixel) {
            return first + pixel * pitch;
        }

        /** Lueur proportionnelle a la cellule : 0.30 bloc pour une cellule de 4/16. */
        float glowRadius() {
            return halfAlong * 2.4f;
        }

        /** Barre dense : LED plus petites qu'un quart de bloc, lueurs basse resolution et sans bloom serre. */
        boolean dense() {
            return pitch < 0.1f;
        }

        /** Texels de rampe par bloc : au moins trois par LED, pour que chaque LED garde sa couleur. */
        int rampTexelsPerBlock() {
            return Math.max(RAMP_TEXELS_PER_BLOCK, (int) Math.ceil(3f / pitch));
        }

        float x(float along, float across) {
            return vertical ? across : along;
        }

        float y(float along, float across) {
            return vertical ? along : across;
        }
    }

    /**
     * Quasi nul : le moteur trace un rayon max(rayon de base, distance x tan), une nappe garde donc
     * sa largeur puis s'ouvre en cone la ou le second terme depasse le premier, avec un coude
     * visible a mi-faisceau. Avec ce demi-angle le coude tombe a plus de cent blocs pour une LED
     * seule et la nappe reste parallele sur toute sa longueur.
     */
    private static final float PIXEL_HALF_ANGLE_DEG = 0.05f;
    /**
     * Intensite de base d'une nappe, multipliee par le reglage « Bar beam » de la config. Une nappe
     * n'a que 0.08 bloc d'epaisseur : un rayon y traverse bien moins de haze que dans un cone rond,
     * d'ou une base bien plus haute que les 8 % de l'ancien eventail.
     */
    private static final float PIXEL_BEAM_INTENSITY = 0.32f;
    /** Demi-epaisseur de la nappe en travers, en blocs : une ligne, pas un volume. */
    private static final float SHEET_HALF_THICKNESS = 0.04f;
    /**
     * Epaisseur minimale d'une nappe a l'ecran, en pixels. Une nappe de 0.08 bloc vue de loin ou de
     * face fait moins d'un pixel : la plupart des rayons la ratent, les autres n'y traversent presque
     * rien, et la ligne scintille puis disparait des qu'on s'eloigne de la barre. L'epaisseur suit
     * donc la distance a la camera, et l'intensite baisse d'autant pour garder la meme luminosite.
     */
    private static final float MIN_SHEET_PIXELS = 3.0f;
    /** Resolution de la rampe de couleur d'une nappe, en texels par bloc, et sa largeur maximale. */
    private static final int RAMP_TEXELS_PER_BLOCK = 16;
    private static final int RAMP_MAX_TEXELS = 2048;
    /** Ecart maximal, en blocs, entre deux barres voisines pour enchainer leurs nappes. */
    private static final int CHAIN_MAX_GAP_BLOCKS = 3;
    /** Deux LED allumees sont contigues (meme barre ou barres voisines) jusqu'a ce multiple du pas. */
    private static final float CHAIN_CONTIGUOUS_PITCHES = 2.2f;
    /** Jonction minimale entre deux barres, en blocs : sans elle, deux barres denses voisines ne s'uniraient jamais. */
    private static final float CHAIN_BRIDGE_MIN_BLOCKS = 0.45f;
    /** Lueur large et douce autour de chaque LED ; les lueurs voisines se rejoignent en un ruban. */
    private static final float DOT_GLOW_STRENGTH = 0.65f;
    /** Bloom serre autour de la LED : un halo vif, a une fraction du rayon de la lueur large. */
    private static final float DOT_BLOOM_RADIUS_SCALE = 0.45f;
    private static final float DOT_BLOOM_STRENGTH = 1.0f;
    /**
     * Coeur de la LED : un point plus petit tire vers le blanc, par-dessus le point colore. Une LED
     * regardee en face sature au centre, la couleur reste sur le bord.
     */
    private static final float DOT_CORE_SCALE = 0.5f;
    private static final float DOT_CORE_WHITEN = 0.6f;
    /** La cellule (lentille) autour de la LED s'eclaire. */
    private static final float CELL_ALPHA = 0.40f;
    /**
     * Faisceau plat : une nappe fine par plage, a sa couleur, couchee dans l'axe du faisceau et qui
     * s'eteint sur la longueur configuree. Pas de cone.
     */
    private static final float FLAT_BEAM_ALPHA = 0.28f;
    /** Demi-largeur de la nappe plate a son extremite, en multiple de la demi-cellule : 1 = parallele. */
    private static final float FLAT_BEAM_SPREAD = 1.0f;
    /** Demi-epaisseur de la nappe : deux feuilles rapprochees, pour un peu de corps de profil. */
    private static final float FLAT_BEAM_HALF_THICKNESS = 0.015f;

    private static final ResourceLocation OPEN_GOBO = new ResourceLocation("theatricalextralights", "textures/gobos/generic_1/open.png");

    /** Rampes de couleur des nappes de chaque barre de tete, une par plage, reutilisees d'une image a l'autre. */
    private final WeakHashMap<T, List<DynamicTexture>> ramps = new WeakHashMap<>();

    protected PixelBarRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /** Geometrie des pixels de cette barre. */
    protected abstract Strip strip();

    /** Une barre de la chaine et son decalage, en blocs, le long de l'axe des pixels depuis la tete. */
    private record Link<T>(T bar, float offset) {
    }

    /** Un pixel de la chaine : position le long de l'axe (repere de la tete), couleur brute, niveau effectif. */
    private record ChainPixel(float along, int colour, int level) {
    }

    @Override
    public void beforeRenderBeam(T blockEntity, PoseStack poseStack, VertexConsumer vertexConsumer, MultiBufferSource multiBufferSource, Direction facing, float partialTicks, boolean isFlipped, BlockState blockstate, boolean isHanging, int packedLight, int packedOverlay) {
        if (blockEntity.getIntensity() <= 0) {
            return;
        }
        // En mode pixel il n'y a pas de dimmer general : chaque pixel porte le sien (dans son niveau
        // et dans getPixelDimmer), et intensity ne sert qu'a savoir si la barre est allumee.
        boolean pixelMode = blockEntity.isPixelMode();
        boolean volumetric = TheatricalExtraLightsConfig.isVolumetricBeamEnabled();
        float beamGain = TheatricalExtraLightsConfig.getRgbBarBeamIntensity();
        Strip s = strip();
        int count = blockEntity.getPixelCount();

        // Barres voisines alignees : la tete de la chaine dessine les nappes de toutes.
        List<Link<T>> chain = chain(blockEntity, s, facing, partialTicks, isFlipped, blockstate, isHanging);
        boolean leader = chain.get(0).bar() == blockEntity;
        List<ChainPixel> pixels = leader ? chainPixels(chain, s) : List.of();

        if (volumetric && leader) {
            // Une plage de LED voisines allumees = une seule nappe, aux couleurs de ses LED : une LED
            // seule fait une ligne fine, chaque voisine allumee elargit la nappe, une LED eteinte la
            // coupe, et la plage continue d'une barre a la suivante. Une seule passe de rendu par
            // plage : la courbe de tonalite du shader s'applique a la nappe entiere.
            int[] index = {0};
            forEachLitSpan(pixels, s, (span, alongStart, alongEnd, meanColour, meanLevel) ->
                    submitSheet(blockEntity, s, facing, partialTicks, isFlipped, blockstate, isHanging,
                            span, alongStart, alongEnd, meanColour, meanLevel, beamGain, index[0]++));
        }

        LazyRenderers.addLazyRender(new LazyRenderers.LazyRenderer() {
            @Override
            public void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, Camera camera, float partialTick) {
                poseStack.pushPose();
                Vec3 offset = Vec3.atLowerCornerOf(blockEntity.getBlockPos()).subtract(camera.getPosition());
                poseStack.translate(offset.x, offset.y, offset.z);
                preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);

                float intensity = com.github.dumann089.theatricalextralights.client.StrobeRenderHelper.displayIntensity(blockEntity, partialTick);
                float alpha = pixelMode ? 1f : intensity / 255f;

                poseStack.translate(s.faceX(), s.faceY(), s.faceZ());
                Matrix4f m = poseStack.last().pose();
                Matrix3f normal = poseStack.last().normal();
                float hx = s.x(s.halfAlong(), s.halfAcross());
                float hy = s.y(s.halfAlong(), s.halfAcross());

                // Les deux render types partagent le tampon de repli du BufferSource : une passe
                // par type, sinon les quads d'une passe partent dans le lot de l'autre.
                VertexConsumer builder = bufferSource.getBuffer(Beam2DRenderTypes.getBeam());
                for (int i = 0; i < count; i++) {
                    int colour = blockEntity.getPixelColour(i);
                    if (blockEntity.getPixelLevel(i) <= 0) continue;
                    int r = (colour >> 16) & 0xFF;
                    int g = (colour >> 8) & 0xFF;
                    int b = colour & 0xFF;
                    float cx = s.x(s.along(i), 0f);
                    float cy = s.y(s.along(i), 0f);
                    int cellA = (int) (alpha * blockEntity.getPixelDimmer(i) / 255f * CELL_ALPHA * 255);
                    addVertex(builder, m, normal, r, g, b, cellA, cx - hx, cy + hy, 0f);
                    addVertex(builder, m, normal, r, g, b, cellA, cx + hx, cy + hy, 0f);
                    addVertex(builder, m, normal, r, g, b, cellA, cx + hx, cy - hy, 0f);
                    addVertex(builder, m, normal, r, g, b, cellA, cx - hx, cy - hy, 0f);
                }
                VertexConsumer dots = StrobeVisualEffects.glowBuffer(bufferSource);
                float d = s.dotHalf();
                for (int i = 0; i < count; i++) {
                    int colour = blockEntity.getPixelColour(i);
                    if (blockEntity.getPixelLevel(i) <= 0) continue;
                    int r = (colour >> 16) & 0xFF;
                    int g = (colour >> 8) & 0xFF;
                    int b = colour & 0xFF;
                    float cx = s.x(s.along(i), 0f);
                    float cy = s.y(s.along(i), 0f);
                    int dotA = (int) (alpha * blockEntity.getPixelDimmer(i));
                    StrobeVisualEffects.glowVertex(dots, m, cx - d, cy + d, -0.002f, r, g, b, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx + d, cy + d, -0.002f, r, g, b, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx + d, cy - d, -0.002f, r, g, b, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx - d, cy - d, -0.002f, r, g, b, dotA);
                    // Coeur surexpose, additif par-dessus le point.
                    int cr = r + (int) ((255 - r) * DOT_CORE_WHITEN);
                    int cg = g + (int) ((255 - g) * DOT_CORE_WHITEN);
                    int cb = b + (int) ((255 - b) * DOT_CORE_WHITEN);
                    float c = d * DOT_CORE_SCALE;
                    StrobeVisualEffects.glowVertex(dots, m, cx - c, cy + c, -0.003f, cr, cg, cb, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx + c, cy + c, -0.003f, cr, cg, cb, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx + c, cy - c, -0.003f, cr, cg, cb, dotA);
                    StrobeVisualEffects.glowVertex(dots, m, cx - c, cy - c, -0.003f, cr, cg, cb, dotA);
                }
                poseStack.popPose();

                // Lueurs : positions des LED dans le repere camera (sans la vue), disques dessines dans la vue.
                PoseStack world = new PoseStack();
                world.translate(offset.x, offset.y, offset.z);
                preparePoseStack(blockEntity, world, facing, partialTick, isFlipped, blockstate, isHanging);
                world.translate(s.faceX(), s.faceY(), s.faceZ() - 0.01f);
                Matrix4f wm = world.last().pose();
                for (int i = 0; i < count; i++) {
                    int colour = blockEntity.getPixelColour(i);
                    int level = blockEntity.getPixelLevel(i);
                    if (level <= 0) continue;
                    Vector4f c = wm.transform(new Vector4f(s.x(s.along(i), 0f), s.y(s.along(i), 0f), 0f, 1f));
                    Vector3f centre = new Vector3f(c.x, c.y, c.z);
                    // Deux disques : un bloom serre et vif contre la LED, puis la lueur large et douce.
                    // Barre dense : la seule lueur large, basse resolution ; ses voisines la rejoignent.
                    if (!s.dense()) {
                        StrobeVisualEffects.renderGlowDot(bufferSource, poseStack, camera, centre, colour,
                                s.glowRadius() * DOT_BLOOM_RADIUS_SCALE, alpha * level / 255f * DOT_BLOOM_STRENGTH);
                    }
                    StrobeVisualEffects.renderGlowDot(bufferSource, poseStack, camera, centre, colour,
                            s.glowRadius(), alpha * level / 255f * DOT_GLOW_STRENGTH, s.dense());
                }

                if (!volumetric && leader) {
                    // Sans volumetrique : nappes plates 2D, sans haze, pour toute la chaine.
                    poseStack.pushPose();
                    poseStack.translate(offset.x, offset.y, offset.z);
                    preparePoseStack(blockEntity, poseStack, facing, partialTick, isFlipped, blockstate, isHanging);
                    poseStack.translate(s.faceX(), s.faceY(), s.faceZ());
                    renderFlatBeams(bufferSource, poseStack, pixels, s, beamGain);
                    poseStack.popPose();
                }
            }

            @Override
            public Vec3 getPos(float partialTick) {
                return blockEntity.getBlockPos().getCenter();
            }
        });
    }

    // ── Chaine de barres ─────────────────────────────────────────────────────

    /**
     * Barres du meme bloc, alignees bout a bout le long de l'axe de leurs pixels, avec la meme
     * orientation, les memes angles et allumees, triees le long de l'axe. La premiere est la tete.
     * Seules les barres dont l'axe suit un axe du monde s'enchainent : une barre mobile en pan
     * quelconque reste seule.
     */
    private List<Link<T>> chain(T blockEntity, Strip s, Direction facing, float partialTicks,
                                boolean isFlipped, BlockState blockstate, boolean isHanging) {
        List<Link<T>> links = new ArrayList<>();
        links.add(new Link<>(blockEntity, 0f));
        Level level = blockEntity.getLevel();
        if (level == null) {
            return links;
        }
        PoseStack probe = new PoseStack();
        preparePoseStack(blockEntity, probe, facing, partialTicks, isFlipped, blockstate, isHanging);
        Matrix4f m = probe.last().pose();
        Vector3f axis = s.vertical()
                ? new Vector3f(m.m10(), m.m11(), m.m12())
                : new Vector3f(m.m00(), m.m01(), m.m02());
        if (axis.lengthSquared() < 1.0e-6f) {
            return links;
        }
        axis.normalize();
        int sx = Math.round(axis.x), sy = Math.round(axis.y), sz = Math.round(axis.z);
        if (Math.abs(sx) + Math.abs(sy) + Math.abs(sz) != 1
                || Math.abs(axis.x * sx + axis.y * sy + axis.z * sz) < 0.99f) {
            return links;
        }
        for (int dir = -1; dir <= 1; dir += 2) {
            BlockPos pos = blockEntity.getBlockPos();
            float offset = 0f;
            while (true) {
                T next = null;
                int gap = 0;
                for (int k = 1; k <= CHAIN_MAX_GAP_BLOCKS && next == null; k++) {
                    BlockPos candidate = pos.offset(sx * k * dir, sy * k * dir, sz * k * dir);
                    next = sameBar(blockEntity, level.getBlockEntity(candidate));
                    gap = k;
                }
                if (next == null) {
                    break;
                }
                pos = next.getBlockPos();
                offset += gap * dir;
                links.add(new Link<>(next, offset));
            }
        }
        links.sort(Comparator.comparingDouble(link -> link.offset()));
        return links;
    }

    /** L'autre bloc est une barre identique (meme bloc et etat, memes pan/tilt) et allumee. */
    @SuppressWarnings("unchecked")
    private T sameBar(T reference, BlockEntity other) {
        if (other == null || other.getClass() != reference.getClass()) {
            return null;
        }
        T bar = (T) other;
        if (bar.getBlockState() != reference.getBlockState()) {
            return null;
        }
        if (bar.getPan() != reference.getPan() || bar.getTilt() != reference.getTilt()) {
            return null;
        }
        if (bar.getIntensity() <= 0) {
            return null;
        }
        return bar;
    }

    /**
     * Tous les pixels de la chaine en une rangee, dans le repere de la tete (un bloc = une unite),
     * avec le niveau effectif de chaque pixel : son niveau, fois le dimmer general de sa barre en
     * mode classique.
     */
    private List<ChainPixel> chainPixels(List<Link<T>> chain, Strip s) {
        List<ChainPixel> pixels = new ArrayList<>();
        for (Link<T> link : chain) {
            T bar = link.bar();
            float master = bar.isPixelMode() ? 1f : bar.getIntensity() / 255f;
            for (int i = 0; i < bar.getPixelCount(); i++) {
                int level = Math.round(bar.getPixelLevel(i) * master);
                pixels.add(new ChainPixel(link.offset() + s.along(i), bar.getPixelColour(i), level));
            }
        }
        pixels.sort(Comparator.comparingDouble(p -> p.along()));
        return pixels;
    }

    /** Une plage de LED voisines allumees, toutes couleurs confondues, en unites le long de l'axe. */
    @FunctionalInterface
    protected interface LitSpan {
        /**
         * @param span       les LED de la plage, dans l'ordre
         * @param alongStart centre de la premiere LED de la plage
         * @param alongEnd   centre de la derniere
         * @param meanColour couleur brute moyenne, ponderee par le niveau de chaque LED
         */
        void accept(List<ChainPixel> span, float alongStart, float alongEnd, int meanColour, float meanLevel);
    }

    /**
     * Parcourt les plages de LED allumees contigues. Une plage ne s'etend que sur des LED allumees
     * voisines : l'ecart maximal ne joue donc qu'a la jonction de deux barres, ou il n'y a pas de LED.
     * Il vaut quelques pas, et au moins {@link #CHAIN_BRIDGE_MIN_BLOCKS} pour les barres denses.
     */
    private static void forEachLitSpan(List<ChainPixel> pixels, Strip s, LitSpan consumer) {
        float maxGap = Math.max(s.pitch() * CHAIN_CONTIGUOUS_PITCHES, CHAIN_BRIDGE_MIN_BLOCKS);
        int n = pixels.size();
        int start = 0;
        while (start < n) {
            if (pixels.get(start).level() <= 0) {
                start++;
                continue;
            }
            int end = start;
            while (end + 1 < n && pixels.get(end + 1).level() > 0
                    && pixels.get(end + 1).along() - pixels.get(end).along() <= maxGap) {
                end++;
            }
            long r = 0, g = 0, b = 0, weight = 0;
            for (int i = start; i <= end; i++) {
                ChainPixel p = pixels.get(i);
                r += (long) ((p.colour() >> 16) & 0xFF) * p.level();
                g += (long) ((p.colour() >> 8) & 0xFF) * p.level();
                b += (long) (p.colour() & 0xFF) * p.level();
                weight += p.level();
            }
            int mean = weight > 0
                    ? ((int) (r / weight) << 16) | ((int) (g / weight) << 8) | (int) (b / weight)
                    : 0;
            consumer.accept(pixels.subList(start, end + 1), pixels.get(start).along(), pixels.get(end).along(),
                    mean, weight / (float) (end - start + 1));
            start = end + 1;
        }
    }

    // ── Nappes ───────────────────────────────────────────────────────────────

    /**
     * Une nappe volumetrique couvrant les LED de {@code alongStart} a {@code alongEnd}. Avec le
     * moteur raymarch, la nappe est blanche et porte les couleurs de ses LED dans une rampe ;
     * sinon elle prend la couleur moyenne.
     */
    private void submitSheet(T blockEntity, Strip s, Direction facing, float partialTicks, boolean isFlipped,
                             BlockState blockstate, boolean isHanging, List<ChainPixel> span,
                             float alongStart, float alongEnd, int meanColour, float meanLevel, float beamGain,
                             int beamIndex) {
        float centre = (alongStart + alongEnd) * 0.5f;
        float halfRun = (alongEnd - alongStart) * 0.5f + s.halfAlong();
        PoseStack beamPose = new PoseStack();
        preparePoseStack(blockEntity, beamPose, facing, partialTicks, isFlipped, blockstate, isHanging);
        beamPose.translate(s.faceX() + s.x(centre, 0f), s.faceY() + s.y(centre, 0f), s.faceZ());
        // Rayon de base = demi-largeur de la plage ; en travers, une epaisseur fixe, au moins
        // quelques pixels a l'ecran. Une nappe epaissie pour l'ecran garde la meme lumiere, donc
        // une densite moindre.
        float half = sheetHalfThickness(blockEntity);
        float thin = half / halfRun;
        float gain = SHEET_HALF_THICKNESS / half * beamGain * PIXEL_BEAM_INTENSITY;

        boolean ramp = TheatricalExtraLightsConfig.isRaymarchEngine() && ModShaders.canUseRaymarch();
        if (!ramp) {
            float widthScale = s.vertical() ? thin : 1f;
            float heightScale = s.vertical() ? 1f : thin;
            submitVolumetricBeam(blockEntity, beamPose, partialTicks, PIXEL_HALF_ANGLE_DEG, PIXEL_HALF_ANGLE_DEG,
                    null, 0, 0f, widthScale, heightScale, beamIndex, normalise(meanColour),
                    meanLevel / 255f * gain, halfRun,
                    (float) Math.min(blockEntity.getDistance(), TheatricalExtraLightsConfig.getRgbBarBeamLength()));
            return;
        }

        // Rampe : la couleur fois le niveau de chaque LED, le long de la nappe. L'axe U du faisceau
        // est l'axe des pixels quelle que soit l'orientation de la barre, l'axe V la traverse.
        DynamicTexture texture = rampTexture(blockEntity, beamIndex, span,
                alongStart - s.halfAlong(), alongEnd + s.halfAlong());
        Matrix4f m = beamPose.last().pose();
        Vec3 origin = new Vec3(m.m30(), m.m31(), m.m32());
        Vec3 along = (s.vertical() ? new Vec3(m.m10(), m.m11(), m.m12()) : new Vec3(m.m00(), m.m01(), m.m02())).normalize();
        Vec3 across = (s.vertical() ? new Vec3(m.m00(), m.m01(), m.m02()) : new Vec3(m.m10(), m.m11(), m.m12())).normalize();
        Vec3 dir = new Vec3(-m.m20(), -m.m21(), -m.m22()).normalize();
        float tanHalfAngle = (float) Math.tan(Math.toRadians(PIXEL_HALF_ANGLE_DEG));
        BeamRenderData data = new BeamRenderData(
                blockEntity.getBlockPos(), origin, dir, along, across,
                0f, (float) blockEntity.getDistance(), tanHalfAngle, 0xFFFFFF, gain,
                OPEN_GOBO, OPEN_GOBO, 0f, 0f, blockEntity.getLevel(),
                1f, thin, halfRun);
        // Longueur de la nappe : le reglage rgbBarBeamLength, comme les nappes plates, bornee par l'obstacle.
        RaymarchBeamRenderer.submit(data, texture.getId(), TheatricalExtraLightsConfig.getRgbBarBeamLength());
    }

    /**
     * Texture d'une ligne, une texel par tranche de bloc, ou chaque tranche prend la couleur de la
     * LED la plus proche fois son niveau. Reutilisee d'une image a l'autre pour la meme plage.
     */
    private DynamicTexture rampTexture(T blockEntity, int index, List<ChainPixel> span, float from, float to) {
        List<DynamicTexture> list = ramps.computeIfAbsent(blockEntity, k -> new ArrayList<>());
        int width = Math.max(1, Math.min(RAMP_MAX_TEXELS, Math.round((to - from) * strip().rampTexelsPerBlock())));
        while (list.size() <= index) {
            list.add(null);
        }
        DynamicTexture texture = list.get(index);
        if (texture == null || texture.getPixels() == null || texture.getPixels().getWidth() != width) {
            if (texture != null) {
                texture.close();
            }
            texture = new DynamicTexture(width, 1, false);
            list.set(index, texture);
        }
        NativeImage image = texture.getPixels();
        for (int x = 0; x < width; x++) {
            float along = from + (x + 0.5f) * (to - from) / width;
            ChainPixel nearest = span.get(0);
            float best = Float.MAX_VALUE;
            for (ChainPixel p : span) {
                float distance = Math.abs(p.along() - along);
                if (distance < best) {
                    best = distance;
                    nearest = p;
                }
            }
            int colour = nearest.colour();
            int level = nearest.level();
            int r = ((colour >> 16) & 0xFF) * level / 255;
            int g = ((colour >> 8) & 0xFF) * level / 255;
            int b = (colour & 0xFF) * level / 255;
            // NativeImage range les canaux en ABGR.
            image.setPixelRGBA(x, 0, 0xFF000000 | (b << 16) | (g << 8) | r);
        }
        texture.upload();
        return texture;
    }

    /** Demi-epaisseur de la nappe pour cette image : la valeur physique, ou ce que couvrent MIN_SHEET_PIXELS. */
    private static float sheetHalfThickness(PixelBarBlockEntity blockEntity) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double distance = cam.distanceTo(blockEntity.getBlockPos().getCenter());
        double fov = Math.toRadians(mc.options.fov().get());
        double blocksPerPixel = 2.0 * distance * Math.tan(fov * 0.5) / Math.max(1, mc.getWindow().getHeight());
        return (float) Math.max(SHEET_HALF_THICKNESS, blocksPerPixel * MIN_SHEET_PIXELS * 0.5);
    }

    /** Nappes plates : une par plage de LED allumees, couleur moyenne, alpha au depart, zero au bout. */
    private void renderFlatBeams(MultiBufferSource.BufferSource bufferSource, PoseStack stack,
                                 List<ChainPixel> pixels, Strip s, float gain) {
        float length = TheatricalExtraLightsConfig.getRgbBarBeamLength();
        float opacity = (float) TheatricalConfig.INSTANCE.CLIENT.beamOpacity;
        VertexConsumer vc = bufferSource.getBuffer(Beam2DRenderTypes.getBeam());
        Matrix4f m = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        forEachLitSpan(pixels, s, (span, alongStart, alongEnd, meanColour, meanLevel) -> {
            int colour = normalise(meanColour);
            int r = (colour >> 16) & 0xFF;
            int g = (colour >> 8) & 0xFF;
            int b = colour & 0xFF;
            int a = Math.min(255, (int) (gain * meanLevel / 255f * FLAT_BEAM_ALPHA * opacity * 255));
            if (a <= 0) return;
            float c = (alongStart + alongEnd) * 0.5f;
            float w0 = (alongEnd - alongStart) * 0.5f + s.halfAlong();
            // La divergence est celle d'une cellule, quelle que soit la largeur de la plage.
            float w1 = w0 + s.halfAlong() * (FLAT_BEAM_SPREAD - 1f);
            for (float t : new float[]{FLAT_BEAM_HALF_THICKNESS, -FLAT_BEAM_HALF_THICKNESS}) {
                addVertex(vc, m, normal, r, g, b, a, s.x(c - w0, t), s.y(c - w0, t), 0f);
                addVertex(vc, m, normal, r, g, b, a, s.x(c + w0, t), s.y(c + w0, t), 0f);
                addVertex(vc, m, normal, r, g, b, 0, s.x(c + w1, t), s.y(c + w1, t), -length);
                addVertex(vc, m, normal, r, g, b, 0, s.x(c - w1, t), s.y(c - w1, t), -length);
            }
        });
    }

    /** Ramene la composante la plus forte a 255 : la couleur reste saturee, la luminosite va dans l'intensite. */
    private static int normalise(int colour) {
        int r = (colour >> 16) & 0xFF, g = (colour >> 8) & 0xFF, b = colour & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max <= 0) return 0;
        return (r * 255 / max << 16) | (g * 255 / max << 8) | (b * 255 / max);
    }

    @Override
    protected void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f nm,
                             int r, int g, int b, int a,
                             float x, float y, float z) {
        if (Beam2DRenderTypes.isShadersActive()) {
            builder.vertex(m, x, y, z)
                    .color(r, g, b, a)
                    .uv(0f, 0f)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .endVertex();
        } else {
            super.addVertex(builder, m, nm, r, g, b, a, x, y, z);
        }
    }
}
