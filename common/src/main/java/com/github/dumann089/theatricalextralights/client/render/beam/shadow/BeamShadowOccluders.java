package com.github.dumann089.theatricalextralights.client.render.beam.shadow;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Occulteurs d'un faisceau pour les ombres portees dans le volume et sur la tache projetee :
 *
 * <ul>
 *   <li>une grille d'occupation des blocs autour du faisceau, envoyee au GPU comme texture 2D
 *       de {@code N} tranches empilees ({@code N x N*N}, rouge = solide ; vert/bleu/alpha =
 *       couleur de carte du bloc, utilisee par le gobo pour teinter sa tache projetee avec la
 *       vraie couleur de la surface touchee), reconstruite toutes les {@link #REBUILD_TICKS}
 *       ou quand le faisceau sort de la grille ;</li>
 *   <li>jusqu'a {@link #MAX_BOXES} boites d'entites qui traversent le cone, rafraichies a
 *       chaque appel.</li>
 * </ul>
 *
 * Une entree par projecteur, liberee quand il n'est plus rendu.
 */
public final class BeamShadowOccluders {

    /** Cellules par axe. */
    public static final int GRID = 40;
    public static final int MAX_BOXES = 8;
    private static final int REBUILD_TICKS = 10;
    private static final long EXPIRE_TICKS = 200;

    /** Ce que le renderer pousse dans les uniforms. */
    public record Snapshot(int textureId, Vec3 gridOrigin, float cell, int size, List<AABB> boxes, boolean anySolid) {
    }

    private static final class Variant {
        long tick = Long.MIN_VALUE;
        long used;
        double ox, oy, oz, dx, dy, dz;
        float len, rad;
        final List<AABB> boxes = new ArrayList<>();

        boolean matches(Vec3 o, Vec3 d, float l, float r) {
            return ox == o.x && oy == o.y && oz == o.z && dx == d.x && dy == d.y && dz == d.z
                    && len == l && rad == r;
        }

        void set(Vec3 o, Vec3 d, float l, float r) {
            ox = o.x; oy = o.y; oz = o.z;
            dx = d.x; dy = d.y; dz = d.z;
            len = l; rad = r;
        }
    }

    private static final class Entry {
        DynamicTexture texture;
        NativeImage image;
        Vec3 gridOrigin = Vec3.ZERO;
        int cell = 1;
        long lastBuildTick = Long.MIN_VALUE;
        long lastUseTick;
        /** true se a grade tem ao menos uma celula solida (grade vazia = sem sombra de blocos). */
        boolean anySolid;
        /**
         * Caixas de entidades por conjunto de parametros de chamada (o projetor de gobo e o feixe
         * volumetrico pedem o mesmo projetor com comprimentos diferentes). Cada variante guarda a
         * ultima consulta, valida durante o tick em que foi feita.
         */
        final Variant[] variants = {new Variant(), new Variant()};
        long variantClock;

        Variant variantFor(Vec3 o, Vec3 d, float len, float rad) {
            for (Variant v : variants) {
                if (v.matches(o, d, len, rad)) {
                    v.used = ++variantClock;
                    return v;
                }
            }
            Variant pick = variants[0].used <= variants[1].used ? variants[0] : variants[1];
            pick.set(o, d, len, rad);
            pick.tick = Long.MIN_VALUE;
            pick.used = ++variantClock;
            return pick;
        }

        boolean contains(Vec3 min, Vec3 max) {
            double ext = (double) cell * GRID;
            return min.x >= gridOrigin.x && min.y >= gridOrigin.y && min.z >= gridOrigin.z
                    && max.x <= gridOrigin.x + ext && max.y <= gridOrigin.y + ext && max.z <= gridOrigin.z + ext;
        }
    }

    private static final Map<BlockPos, Entry> CACHE = new HashMap<>();
    private static long lastSweepTick;

    private BeamShadowOccluders() {
    }

    /**
     * @param origin    origine du faisceau, monde
     * @param dir       direction normalisee
     * @param length    longueur balayee
     * @param endRadius rayon du cone a l'extremite
     */
    public static Snapshot get(Level level, BlockPos fixture, Vec3 origin, Vec3 dir, float length, float endRadius) {
        long tick = level.getGameTime();
        Entry e = CACHE.computeIfAbsent(fixture.immutable(), k -> new Entry());
        e.lastUseTick = tick;

        Vec3 end = origin.add(dir.scale(length));
        double margin = endRadius + 1.5;
        Vec3 bbMin = new Vec3(Math.min(origin.x, end.x) - margin, Math.min(origin.y, end.y) - margin, Math.min(origin.z, end.z) - margin);
        Vec3 bbMax = new Vec3(Math.max(origin.x, end.x) + margin, Math.max(origin.y, end.y) + margin, Math.max(origin.z, end.z) + margin);

        if (e.texture == null || tick - e.lastBuildTick >= REBUILD_TICKS || !e.contains(bbMin, bbMax)) {
            boolean first = e.texture == null;
            rebuildBlocks(level, fixture, e, origin, dir, length, endRadius, bbMin, bbMax);
            // Fase propria por projetor: sem isso todos reconstroem no mesmo tick e causam picos.
            e.lastBuildTick = first ? tick - Math.floorMod(fixture.hashCode(), REBUILD_TICKS) : tick;
        }
        // O mundo cliente so muda entre ticks: com origem, direcao e alcance identicos, a consulta
        // de entidades do tick atual ja vale para os demais frames dele.
        Variant variant = e.variantFor(origin, dir, length, endRadius);
        if (variant.tick != tick) {
            collectEntities(level, variant.boxes, origin, dir, length, endRadius, bbMin, bbMax);
            variant.tick = tick;
        }

        if (tick - lastSweepTick > 100) {
            lastSweepTick = tick;
            sweep(tick);
        }
        return new Snapshot(e.texture.getId(), e.gridOrigin, e.cell, GRID, variant.boxes, e.anySolid);
    }

    private static void rebuildBlocks(Level level, BlockPos fixture, Entry e, Vec3 origin, Vec3 dir, float length,
                                      float endRadius, Vec3 bbMin, Vec3 bbMax) {
        double extent = Math.max(bbMax.x - bbMin.x, Math.max(bbMax.y - bbMin.y, bbMax.z - bbMin.z));
        int cell = Math.max(1, (int) Math.ceil(extent / GRID));
        // Marge pour que les petits mouvements du faisceau ne forcent pas une reconstruction.
        double slack = (cell * GRID - extent) * 0.5;
        Vec3 gridOrigin = new Vec3(
                Math.floor((bbMin.x - slack) / cell) * cell,
                Math.floor((bbMin.y - slack) / cell) * cell,
                Math.floor((bbMin.z - slack) / cell) * cell);
        e.cell = cell;
        e.gridOrigin = gridOrigin;

        if (e.image == null) {
            e.image = new NativeImage(NativeImage.Format.RGBA, GRID, GRID * GRID, false);
            e.texture = new DynamicTexture(e.image);
        }
        NativeImage img = e.image;
        img.fillRect(0, 0, GRID, GRID * GRID, 0);
        e.anySolid = false;

        float tan = endRadius / Math.max(length, 0.001f);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        double half = cell * 0.5;
        // Caixa que contem todo centro de celula aceito pelo filtro de cone abaixo: o eixo vai de
        // -cell a length+cell e o raio permitido nunca passa de rMax. Celulas fora dela seriam
        // descartadas pelo filtro de qualquer jeito, entao o resultado e identico.
        double rMax = (length + cell) * tan + cell * 1.5;
        double ax = origin.x - dir.x * cell, ay = origin.y - dir.y * cell, az = origin.z - dir.z * cell;
        double bx = origin.x + dir.x * (length + cell), by = origin.y + dir.y * (length + cell),
                bz = origin.z + dir.z * (length + cell);
        int ixLo = cellIndexLo(Math.min(ax, bx) - rMax, gridOrigin.x, cell);
        int ixHi = cellIndexHi(Math.max(ax, bx) + rMax, gridOrigin.x, cell);
        int iyLo = cellIndexLo(Math.min(ay, by) - rMax, gridOrigin.y, cell);
        int iyHi = cellIndexHi(Math.max(ay, by) + rMax, gridOrigin.y, cell);
        int izLo = cellIndexLo(Math.min(az, bz) - rMax, gridOrigin.z, cell);
        int izHi = cellIndexHi(Math.max(az, bz) + rMax, gridOrigin.z, cell);
        for (int iz = izLo; iz <= izHi; iz++) {
            for (int iy = iyLo; iy <= iyHi; iy++) {
                for (int ix = ixLo; ix <= ixHi; ix++) {
                    double cx = gridOrigin.x + ix * cell + half;
                    double cy = gridOrigin.y + iy * cell + half;
                    double cz = gridOrigin.z + iz * cell + half;
                    // Filtre cone : distance a l'axe <= rayon local + une cellule et demie.
                    double dx = cx - origin.x, dy = cy - origin.y, dz = cz - origin.z;
                    double z = dx * dir.x + dy * dir.y + dz * dir.z;
                    if (z < -cell || z > length + cell) continue;
                    double px = dx - dir.x * z, py = dy - dir.y * z, pz = dz - dir.z * z;
                    double radial = Math.sqrt(px * px + py * py + pz * pz);
                    double allowed = Math.max(0.0, z) * tan + cell * 1.5;
                    if (radial > allowed) continue;

                    mp.set(Math.floor(cx), Math.floor(cy), Math.floor(cz));
                    if (mp.distSqr(fixture) <= 2.0) continue; // la lyre elle-meme
                    if (!level.isLoaded(mp)) continue;
                    BlockState state = level.getBlockState(mp);
                    if (state.isAir()) continue;
                    boolean solid = state.isSolidRender(level, mp)
                            || (!state.getCollisionShape(level, mp).isEmpty() && state.getLightBlock(level, mp) > 0);
                    if (solid) {
                        // Red = occlusion flag (both shaders only ever read .r, so this stays exactly as
                        // before). Green/Blue/Alpha are unused by both shaders otherwise, so we stash the
                        // block's own map colour there (same colour vanilla maps use to represent it) -
                        // the gobo shader can then tint its projected light with the real colour of
                        // whatever it is actually landing on, instead of a flat, unrelated colour.
                        int rgb = state.getMapColor(level, mp).calculateRGBColor(MapColor.Brightness.NORMAL);
                        int rgbR = (rgb >> 16) & 0xFF;
                        int rgbG = (rgb >> 8) & 0xFF;
                        int rgbB = rgb & 0xFF;
                        // NativeImage/GL texture byte order is R,G,B,A (R = lowest byte of the packed int).
                        int packed = 0xFF | (rgbR << 8) | (rgbG << 16) | (rgbB << 24);
                        img.setPixelRGBA(ix, iz * GRID + iy, packed);
                        e.anySolid = true;
                    }
                }
            }
        }
        e.texture.upload();
    }

    /** Menor indice de celula (com folga de 1) cujo centro pode ficar em >= {@code lo}. */
    private static int cellIndexLo(double lo, double gridMin, int cell) {
        int i = (int) Math.floor((lo - gridMin) / cell) - 1;
        return Math.max(0, Math.min(GRID, i));
    }

    /** Maior indice de celula (com folga de 1) cujo centro pode ficar em <= {@code hi}. */
    private static int cellIndexHi(double hi, double gridMin, int cell) {
        int i = (int) Math.ceil((hi - gridMin) / cell) + 1;
        return Math.max(-1, Math.min(GRID - 1, i));
    }

    private static void collectEntities(Level level, List<AABB> out, Vec3 origin, Vec3 dir, float length, float endRadius,
                                        Vec3 bbMin, Vec3 bbMax) {
        out.clear();
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float tan = endRadius / Math.max(length, 0.001f);
        List<Entity> found = level.getEntities((Entity) null, new AABB(bbMin, bbMax),
                en -> en.isAlive() && !en.isSpectator() && !en.isInvisible() && !(firstPerson && en == camera));
        int cap = found.size();
        AABB[] candidates = new AABB[cap];
        double[] dists = new double[cap];
        int count = 0;
        for (Entity en : found) {
            AABB box = en.getBoundingBox();
            Vec3 c = box.getCenter();
            double dx = c.x - origin.x, dy = c.y - origin.y, dz = c.z - origin.z;
            double z = dx * dir.x + dy * dir.y + dz * dir.z;
            if (z < 0 || z > length) continue;
            double px = dx - dir.x * z, py = dy - dir.y * z, pz = dz - dir.z * z;
            double radial = Math.sqrt(px * px + py * py + pz * pz);
            double reach = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5;
            if (radial > z * tan + reach) continue;
            // Insercao estavel por distancia crescente (mesma ordem do sort anterior).
            int j = count - 1;
            while (j >= 0 && dists[j] > z) {
                candidates[j + 1] = candidates[j];
                dists[j + 1] = dists[j];
                j--;
            }
            candidates[j + 1] = box;
            dists[j + 1] = z;
            count++;
        }
        // Os mais proximos da fonte primeiro: sao os que sombreiam mais volume.
        for (int i = 0; i < Math.min(MAX_BOXES, count); i++) {
            out.add(candidates[i]);
        }
    }

    private static void sweep(long tick) {
        Iterator<Map.Entry<BlockPos, Entry>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (tick - e.lastUseTick > EXPIRE_TICKS) {
                if (e.texture != null) e.texture.close();
                it.remove();
            }
        }
    }

    /** Libere toutes les textures (changement de monde). */
    public static void clear() {
        for (Entry e : CACHE.values()) {
            if (e.texture != null) e.texture.close();
        }
        CACHE.clear();
    }
}
