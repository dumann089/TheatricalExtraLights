package com.github.dumann089.theatricalextralights.client.render.beam.shadow;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 *       de {@code N} tranches empilees ({@code N x N*N}, rouge = solide), reconstruite toutes
 *       les {@link #REBUILD_TICKS} ou quand le faisceau sort de la grille ;</li>
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
    public record Snapshot(int textureId, Vec3 gridOrigin, float cell, int size, List<AABB> boxes) {
    }

    private static final class Entry {
        DynamicTexture texture;
        NativeImage image;
        Vec3 gridOrigin = Vec3.ZERO;
        int cell = 1;
        long lastBuildTick = Long.MIN_VALUE;
        long lastUseTick;
        final List<AABB> boxes = new ArrayList<>();

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
            rebuildBlocks(level, fixture, e, origin, dir, length, endRadius, bbMin, bbMax);
            e.lastBuildTick = tick;
        }
        collectEntities(level, e, origin, dir, length, endRadius, bbMin, bbMax);

        if (tick - lastSweepTick > 100) {
            lastSweepTick = tick;
            sweep(tick);
        }
        return new Snapshot(e.texture.getId(), e.gridOrigin, e.cell, GRID, e.boxes);
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

        float tan = endRadius / Math.max(length, 0.001f);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        double half = cell * 0.5;
        for (int iz = 0; iz < GRID; iz++) {
            for (int iy = 0; iy < GRID; iy++) {
                for (int ix = 0; ix < GRID; ix++) {
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
                        img.setPixelRGBA(ix, iz * GRID + iy, 0xFFFFFFFF);
                    }
                }
            }
        }
        e.texture.upload();
    }

    private static void collectEntities(Level level, Entry e, Vec3 origin, Vec3 dir, float length, float endRadius,
                                        Vec3 bbMin, Vec3 bbMax) {
        e.boxes.clear();
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float tan = endRadius / Math.max(length, 0.001f);
        List<Entity> found = level.getEntities((Entity) null, new AABB(bbMin, bbMax),
                en -> en.isAlive() && !en.isSpectator() && !en.isInvisible() && !(firstPerson && en == camera));
        List<AABB> candidates = new ArrayList<>();
        List<Double> dists = new ArrayList<>();
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
            candidates.add(box);
            dists.add(z);
        }
        // Les plus proches de la source d'abord : ce sont elles qui ombrent le plus de volume.
        Integer[] order = new Integer[candidates.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(dists.get(a), dists.get(b)));
        for (int i = 0; i < Math.min(MAX_BOXES, order.length); i++) {
            e.boxes.add(candidates.get(order[i]));
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
