package com.github.dumann089.theatricalextralights.client.render.beam.shadow;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
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
 *   <li>jusqu'a {@link #MAX_BOXES} boites d'entites qui traversent le cone.</li>
 * </ul>
 *
 * <p>Plusieurs grilles par projecteur : un appareil a plusieurs faisceaux, ou un gobo et sa
 * tache, demande des volumes differents. Avec une seule grille, chaque appel qui sortait de
 * la grille du precedent la reconstruisait, plusieurs fois par image. Un appel reprend la
 * premiere grille qui le contient, exactement comme il reprenait l'unique grille quand elle
 * le contenait.
 *
 * <p>Couts evites sans changer le resultat : texture renvoyee au GPU seulement si son contenu
 * change, requete d'entites une fois par tick et par grille (les boites d'entites ne bougent
 * qu'au tick), sections de chunk vides sautees, et rafraichissements periodiques etales sur
 * plusieurs images au lieu de tomber tous dans la meme.
 */
public final class BeamShadowOccluders {

    /** Cellules par axe. */
    public static final int GRID = 40;
    public static final int MAX_BOXES = 8;
    private static final int REBUILD_TICKS = 10;
    private static final long EXPIRE_TICKS = 200;
    /** Grilles gardees par projecteur (faisceaux + tache de gobo). */
    private static final int MAX_ENTRIES_PER_FIXTURE = 6;
    /**
     * Rafraichissements periodiques (grille encore valide, seulement agee) par fenetre de
     * {@link #STALE_WINDOW_NANOS} : au-dela, la grille actuelle sert une image de plus.
     */
    private static final int STALE_REBUILDS_PER_WINDOW = 3;
    private static final long STALE_WINDOW_NANOS = 6_000_000L;

    /**
     * Ce que le renderer pousse dans les uniforms.
     *
     * @param solidAny une cellule au moins est solide
     * @param solidMin coin min de la boite des cellules solides, monde
     * @param solidMax coin max de la boite des cellules solides, monde
     */
    public record Snapshot(int textureId, Vec3 gridOrigin, float cell, int size, List<AABB> boxes,
                           boolean solidAny, Vec3 solidMin, Vec3 solidMax) {
    }

    private static final class Entry {
        DynamicTexture texture;
        NativeImage image;
        Vec3 gridOrigin = Vec3.ZERO;
        int cell = 1;
        long lastBuildTick = Long.MIN_VALUE;
        long lastUseTick;
        /** Cellules solides de la derniere construction, pour eviter un envoi identique. */
        long[] solid = new long[(GRID * GRID * GRID + 63) / 64];
        long[] scratch = new long[solid.length];
        boolean solidAny;
        Vec3 solidMin = Vec3.ZERO;
        Vec3 solidMax = Vec3.ZERO;
        /** Entites de la boite de la grille, relevees une fois par tick. */
        final List<Entity> entities = new ArrayList<>();
        long entitiesTick = Long.MIN_VALUE;

        boolean contains(Vec3 min, Vec3 max) {
            double ext = (double) cell * GRID;
            return min.x >= gridOrigin.x && min.y >= gridOrigin.y && min.z >= gridOrigin.z
                    && max.x <= gridOrigin.x + ext && max.y <= gridOrigin.y + ext && max.z <= gridOrigin.z + ext;
        }

        AABB bounds() {
            double ext = (double) cell * GRID;
            return new AABB(gridOrigin.x, gridOrigin.y, gridOrigin.z,
                    gridOrigin.x + ext, gridOrigin.y + ext, gridOrigin.z + ext);
        }

        void close() {
            if (texture != null) {
                texture.close();
                texture = null;
                image = null;
            }
        }
    }

    private static final Map<BlockPos, List<Entry>> CACHE = new HashMap<>();
    private static long lastSweepTick;
    private static long staleWindowStart;
    private static int staleRebuildsInWindow;

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
        List<Entry> entries = CACHE.computeIfAbsent(fixture.immutable(), k -> new ArrayList<>(2));

        Vec3 end = origin.add(dir.scale(length));
        double margin = endRadius + 1.5;
        Vec3 bbMin = new Vec3(Math.min(origin.x, end.x) - margin, Math.min(origin.y, end.y) - margin, Math.min(origin.z, end.z) - margin);
        Vec3 bbMax = new Vec3(Math.max(origin.x, end.x) + margin, Math.max(origin.y, end.y) + margin, Math.max(origin.z, end.z) + margin);

        Entry e = null;
        for (Entry candidate : entries) {
            if (candidate.texture != null && candidate.contains(bbMin, bbMax)) {
                e = candidate;
                break;
            }
        }
        if (e == null) {
            e = pickSlot(entries);
            rebuildBlocks(level, fixture, e, origin, dir, length, endRadius, bbMin, bbMax);
            e.lastBuildTick = tick;
        } else if (tick - e.lastBuildTick >= REBUILD_TICKS && takeStaleBudget()) {
            rebuildBlocks(level, fixture, e, origin, dir, length, endRadius, bbMin, bbMax);
            e.lastBuildTick = tick;
        }
        e.lastUseTick = tick;
        List<AABB> boxes = collectEntities(level, e, tick, origin, dir, length, endRadius, bbMin, bbMax);

        if (tick - lastSweepTick > 100) {
            lastSweepTick = tick;
            sweep(tick);
        }
        return new Snapshot(e.texture.getId(), e.gridOrigin, e.cell, GRID, boxes,
                e.solidAny, e.solidMin, e.solidMax);
    }

    /** Une grille libre, ou la moins recemment utilisee quand le projecteur en a deja assez. */
    private static Entry pickSlot(List<Entry> entries) {
        if (entries.size() < MAX_ENTRIES_PER_FIXTURE) {
            Entry fresh = new Entry();
            entries.add(fresh);
            return fresh;
        }
        Entry oldest = entries.get(0);
        for (Entry candidate : entries) {
            if (candidate.lastUseTick < oldest.lastUseTick) {
                oldest = candidate;
            }
        }
        return oldest;
    }

    private static boolean takeStaleBudget() {
        long now = System.nanoTime();
        if (now - staleWindowStart > STALE_WINDOW_NANOS) {
            staleWindowStart = now;
            staleRebuildsInWindow = 0;
        }
        if (staleRebuildsInWindow >= STALE_REBUILDS_PER_WINDOW) {
            return false;
        }
        staleRebuildsInWindow++;
        return true;
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

        long[] solid = e.scratch;
        Arrays.fill(solid, 0L);
        int minX = GRID, minY = GRID, minZ = GRID, maxX = -1, maxY = -1, maxZ = -1;

        float tan = endRadius / Math.max(length, 0.001f);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        SectionCursor cursor = new SectionCursor(level);
        double half = cell * 0.5;
        // Boite qui contient tout centre de cellule accepte par le filtre cone ci-dessous : l'axe
        // va de -cell a length+cell et le rayon permis ne depasse jamais rMax. Les cellules hors
        // de cette boite seraient rejetees de toute facon : meme resultat, moins de cellules.
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
                    BlockState state = cursor.state(mp);
                    if (state == null || state.isAir()) continue;
                    boolean isSolid = state.isSolidRender(level, mp)
                            || (!state.getCollisionShape(level, mp).isEmpty() && state.getLightBlock(level, mp) > 0);
                    if (isSolid) {
                        int bit = (iz * GRID + iy) * GRID + ix;
                        solid[bit >>> 6] |= 1L << (bit & 63);
                        if (ix < minX) minX = ix;
                        if (iy < minY) minY = iy;
                        if (iz < minZ) minZ = iz;
                        if (ix > maxX) maxX = ix;
                        if (iy > maxY) maxY = iy;
                        if (iz > maxZ) maxZ = iz;
                    }
                }
            }
        }

        boolean sameGrid = e.texture != null && e.cell == cell && e.gridOrigin.equals(gridOrigin)
                && Arrays.equals(e.solid, solid);
        e.cell = cell;
        e.gridOrigin = gridOrigin;
        e.solidAny = maxX >= 0;
        if (e.solidAny) {
            e.solidMin = new Vec3(gridOrigin.x + minX * cell, gridOrigin.y + minY * cell, gridOrigin.z + minZ * cell);
            e.solidMax = new Vec3(gridOrigin.x + (maxX + 1) * cell, gridOrigin.y + (maxY + 1) * cell,
                    gridOrigin.z + (maxZ + 1) * cell);
        } else {
            e.solidMin = gridOrigin;
            e.solidMax = gridOrigin;
        }
        if (sameGrid) {
            return; // meme contenu : la texture deja sur le GPU est exacte
        }
        e.scratch = e.solid;
        e.solid = solid;

        if (e.image == null) {
            e.image = new NativeImage(NativeImage.Format.RGBA, GRID, GRID * GRID, false);
            e.texture = new DynamicTexture(e.image);
        }
        NativeImage img = e.image;
        img.fillRect(0, 0, GRID, GRID * GRID, 0);
        if (e.solidAny) {
            for (int iz = minZ; iz <= maxZ; iz++) {
                for (int iy = minY; iy <= maxY; iy++) {
                    for (int ix = minX; ix <= maxX; ix++) {
                        int bit = (iz * GRID + iy) * GRID + ix;
                        if ((solid[bit >>> 6] & (1L << (bit & 63))) != 0) {
                            img.setPixelRGBA(ix, iz * GRID + iy, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }
        e.texture.upload();
    }

    /** Plus petit indice de cellule (avec une de marge) dont le centre peut etre >= {@code lo}. */
    private static int cellIndexLo(double lo, double gridMin, int cell) {
        int i = (int) Math.floor((lo - gridMin) / cell) - 1;
        return Math.max(0, Math.min(GRID, i));
    }

    /** Plus grand indice de cellule (avec une de marge) dont le centre peut etre <= {@code hi}. */
    private static int cellIndexHi(double hi, double gridMin, int cell) {
        int i = (int) Math.ceil((hi - gridMin) / cell) + 1;
        return Math.max(-1, Math.min(GRID - 1, i));
    }

    /**
     * Lecture des blocs par section : une section vide ou un chunk absent sont reconnus une
     * fois, sans repasser par la recherche de chunk pour chaque cellule.
     */
    private static final class SectionCursor {
        private final Level level;
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ = Integer.MIN_VALUE;
        private LevelChunk chunk;
        private int sectionIndex = Integer.MIN_VALUE;
        private LevelChunkSection section;

        SectionCursor(Level level) {
            this.level = level;
        }

        /** Etat du bloc, ou null si son chunk n'est pas charge ; air pour une section vide. */
        BlockState state(BlockPos pos) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            if (cx != chunkX || cz != chunkZ) {
                chunkX = cx;
                chunkZ = cz;
                // Chunk du cache client, ou null s'il n'est pas charge (meme regle que isLoaded).
                ChunkAccess access = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                chunk = access instanceof LevelChunk loaded ? loaded : null;
                sectionIndex = Integer.MIN_VALUE;
            }
            if (chunk == null) {
                return null;
            }
            int index = level.getSectionIndex(pos.getY());
            if (index != sectionIndex) {
                sectionIndex = index;
                LevelChunkSection[] sections = chunk.getSections();
                section = index >= 0 && index < sections.length ? sections[index] : null;
            }
            if (section == null || section.hasOnlyAir()) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
            return section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }
    }

    private static List<AABB> collectEntities(Level level, Entry e, long tick, Vec3 origin, Vec3 dir, float length,
                                              float endRadius, Vec3 bbMin, Vec3 bbMax) {
        if (e.entitiesTick != tick) {
            // Boite de la grille : elle contient toujours la boite du faisceau de cet appel.
            e.entities.clear();
            e.entities.addAll(level.getEntities((Entity) null, e.bounds(), en -> true));
            e.entitiesTick = tick;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float tan = endRadius / Math.max(length, 0.001f);
        AABB query = new AABB(bbMin, bbMax);
        List<AABB> candidates = new ArrayList<>();
        List<Double> dists = new ArrayList<>();
        for (Entity en : e.entities) {
            if (!en.isAlive() || en.isSpectator() || en.isInvisible() || (firstPerson && en == camera)) continue;
            AABB box = en.getBoundingBox();
            if (!box.intersects(query)) continue;
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
        Arrays.sort(order, (a, b) -> Double.compare(dists.get(a), dists.get(b)));
        List<AABB> boxes = new ArrayList<>(Math.min(MAX_BOXES, order.length));
        for (int i = 0; i < Math.min(MAX_BOXES, order.length); i++) {
            boxes.add(candidates.get(order[i]));
        }
        return boxes;
    }

    private static void sweep(long tick) {
        Iterator<Map.Entry<BlockPos, List<Entry>>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            List<Entry> entries = it.next().getValue();
            entries.removeIf(e -> {
                if (tick - e.lastUseTick > EXPIRE_TICKS) {
                    e.close();
                    return true;
                }
                return false;
            });
            if (entries.isEmpty()) {
                it.remove();
            }
        }
    }

    /** Libere toutes les textures (changement de monde). */
    public static void clear() {
        for (List<Entry> entries : CACHE.values()) {
            for (Entry e : entries) {
                e.close();
            }
        }
        CACHE.clear();
    }
}
