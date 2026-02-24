package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.TimedCache;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 城市地块规划器。
 * 在区域级别按固定 seed 规划 footprint，并缓存核心区结果。
 */
public final class PlotPlanner {

    private static final int PLAN_CORE_SIZE = 32;
    private static final int MAX_FOOTPRINT_SIZE = 4;
    private static final int PLAN_MARGIN = MAX_FOOTPRINT_SIZE + 1;

    private static final Supplier<Integer> CACHE_TIMEOUT = () -> 300;
    private static final TimedCache<PlanKey, PlannedRegion> PLAN_CACHE = new TimedCache<>(CACHE_TIMEOUT);

    private PlotPlanner() {
    }

    @FunctionalInterface
    public interface ChunkPredicate {
        boolean test(int chunkX, int chunkZ);
    }

    public static FootprintPlacement resolve(IDimensionInfo provider,
                                             ChunkCoord coord,
                                             ChunkPredicate buildablePredicate,
                                             LostCityProfile profile) {
        if (provider == null || coord == null || buildablePredicate == null) {
            return FootprintPlacement.none();
        }

        long seed = provider.getSeed();
        int chunkX = coord.chunkX();
        int chunkZ = coord.chunkZ();
        int regionX = Math.floorDiv(chunkX, PLAN_CORE_SIZE);
        int regionZ = Math.floorDiv(chunkZ, PLAN_CORE_SIZE);

        PlanKey key = new PlanKey(
                coord.dimension(),
                seed,
                regionX,
                regionZ,
                settingsSignature(profile));

        PlannedRegion region = PLAN_CACHE.computeIfAbsent(key,
                k -> planRegion(seed, regionX, regionZ, buildablePredicate, profile));
        if (region == null) {
            return FootprintPlacement.none();
        }

        return region.get(chunkX, chunkZ);
    }

    public static void cleanupCache() {
        PLAN_CACHE.cleanup();
    }

    public static void resetCache() {
        PLAN_CACHE.clear();
    }

    private static PlannedRegion planRegion(long worldSeed,
                                            int regionX,
                                            int regionZ,
                                            ChunkPredicate buildablePredicate,
                                            LostCityProfile profile) {
        int coreMinX = regionX * PLAN_CORE_SIZE;
        int coreMinZ = regionZ * PLAN_CORE_SIZE;
        int coreMaxX = coreMinX + PLAN_CORE_SIZE - 1;
        int coreMaxZ = coreMinZ + PLAN_CORE_SIZE - 1;

        int extMinX = coreMinX - PLAN_MARGIN;
        int extMinZ = coreMinZ - PLAN_MARGIN;
        int extMaxX = coreMaxX + PLAN_MARGIN;
        int extMaxZ = coreMaxZ + PLAN_MARGIN;

        boolean allowCrossChunk = profile == null || profile.isAllowCrossChunkAllBuildings();
        boolean prioritizeStreetConnectivity = profile == null || profile.isStreetConnectivityPriorityHigh();
        boolean emergencyFallbackToOne = profile != null && profile.isFallbackTo1x1OnConflict();
        float coverage = clamp01(profile == null ? 0.45f : profile.getTargetBuildingCoverage());
        if (prioritizeStreetConnectivity) {
            coverage = Math.min(coverage, 0.55f);
        }

        FootprintWeights weights = new FootprintWeights(
                profile == null ? 0.0f : profile.getFootprintWeight1x1(),
                profile == null ? 35.0f : profile.getFootprintWeight2x2(),
                profile == null ? 35.0f : profile.getFootprintWeight3x2(),
                profile == null ? 30.0f : profile.getFootprintWeight4x4());

        List<AnchorCandidate> candidates = new ArrayList<>();
        for (int anchorX = extMinX - MAX_FOOTPRINT_SIZE + 1; anchorX <= extMaxX; anchorX++) {
            for (int anchorZ = extMinZ - MAX_FOOTPRINT_SIZE + 1; anchorZ <= extMaxZ; anchorZ++) {
                if (!buildablePredicate.test(anchorX, anchorZ)) {
                    continue;
                }
                long hash = placementHash(worldSeed, anchorX, anchorZ, 0x736F6D6570736575L);
                float roll = unitFloat(hash);
                if (roll > coverage) {
                    continue;
                }

                List<FootprintSize> fallback = buildFallbackSequence(hash, allowCrossChunk, profile, weights);
                if (fallback.isEmpty()) {
                    continue;
                }
                long priority = placementHash(worldSeed, anchorX, anchorZ, 0x646F72616E646F6DL);
                candidates.add(new AnchorCandidate(anchorX, anchorZ, priority, fallback, fallback.get(0).width() * fallback.get(0).depth()));
            }
        }

        candidates.sort(Comparator
                .comparingInt(AnchorCandidate::preferredArea).reversed()
                .thenComparingLong(AnchorCandidate::priority).reversed());

        Map<Long, AcceptedFootprint> occupied = new HashMap<>();
        int coreArea = PLAN_CORE_SIZE * PLAN_CORE_SIZE;
        int targetOccupied = Math.max(0, Math.min(coreArea, Math.round(coreArea * coverage)));
        int occupiedCoreCells = 0;
        for (AnchorCandidate candidate : candidates) {
            if (occupiedCoreCells >= targetOccupied) {
                break;
            }
            boolean placed = false;
            for (FootprintSize size : candidate.fallbackSizes()) {
                int gain = tryOccupy(occupied, buildablePredicate, candidate, size,
                        extMinX, extMaxX, extMinZ, extMaxZ,
                        coreMinX, coreMaxX, coreMinZ, coreMaxZ);
                if (gain <= 0) {
                    continue;
                }
                occupiedCoreCells += gain;
                placed = true;
                break;
            }
            if (!placed && emergencyFallbackToOne) {
                int gain = tryOccupy(occupied, buildablePredicate, candidate, FootprintSize.ONE_BY_ONE,
                        extMinX, extMaxX, extMinZ, extMaxZ,
                        coreMinX, coreMaxX, coreMinZ, coreMaxZ);
                if (gain > 0) {
                    occupiedCoreCells += gain;
                }
            }
        }

        Map<Long, FootprintPlacement> core = new HashMap<>();
        for (int x = coreMinX; x <= coreMaxX; x++) {
            for (int z = coreMinZ; z <= coreMaxZ; z++) {
                AcceptedFootprint accepted = occupied.get(pack(x, z));
                if (accepted == null) {
                    continue;
                }
                int localX = x - accepted.anchorX();
                int localZ = z - accepted.anchorZ();
                FootprintPlacement placement = new FootprintPlacement(
                        true,
                        accepted.anchorX(),
                        accepted.anchorZ(),
                        accepted.width(),
                        accepted.depth(),
                        localX,
                        localZ,
                        accepted.priority());
                core.put(pack(x, z), placement);
            }
        }

        return new PlannedRegion(core);
    }

    private static boolean isBuildableArea(ChunkPredicate predicate, int anchorX, int anchorZ, int width, int depth) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (!predicate.test(anchorX + dx, anchorZ + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasConflict(Map<Long, AcceptedFootprint> occupied, int anchorX, int anchorZ, int width, int depth) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                if (occupied.containsKey(pack(anchorX + dx, anchorZ + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int coreGain(Map<Long, AcceptedFootprint> occupied,
                                int anchorX,
                                int anchorZ,
                                int width,
                                int depth,
                                int coreMinX,
                                int coreMaxX,
                                int coreMinZ,
                                int coreMaxZ) {
        int gain = 0;
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int x = anchorX + dx;
                int z = anchorZ + dz;
                if (x < coreMinX || x > coreMaxX || z < coreMinZ || z > coreMaxZ) {
                    continue;
                }
                if (!occupied.containsKey(pack(x, z))) {
                    gain++;
                }
            }
        }
        return gain;
    }

    private static void occupy(Map<Long, AcceptedFootprint> occupied,
                               int anchorX,
                               int anchorZ,
                               int width,
                               int depth,
                               long priority) {
        AcceptedFootprint accepted = new AcceptedFootprint(anchorX, anchorZ, width, depth, priority);
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                occupied.put(pack(anchorX + dx, anchorZ + dz), accepted);
            }
        }
    }

    private static List<FootprintSize> buildFallbackSequence(long hash,
                                                             boolean allowCrossChunk,
                                                             LostCityProfile profile,
                                                             FootprintWeights weights) {
        List<FootprintSize> sequence = new ArrayList<>();
        if (!allowCrossChunk) {
            sequence.add(FootprintSize.ONE_BY_ONE);
            return sequence;
        }

        FootprintSize first = choosePrimarySize(hash, weights);
        sequence.add(first);
        if (first != FootprintSize.FOUR_BY_FOUR) {
            sequence.add(FootprintSize.FOUR_BY_FOUR);
        }
        if (first != FootprintSize.THREE_BY_TWO && first != FootprintSize.TWO_BY_THREE) {
            boolean horizontal = ((hash >>> 17) & 1L) == 0L;
            sequence.add(horizontal ? FootprintSize.THREE_BY_TWO : FootprintSize.TWO_BY_THREE);
        }
        if (first != FootprintSize.TWO_BY_TWO) {
            sequence.add(FootprintSize.TWO_BY_TWO);
        }
        // 常规流程不再回落到 1x1；仅在候选全部失败时使用紧急回退。
        return sequence;
    }

    private static FootprintSize choosePrimarySize(long hash, FootprintWeights weights) {
        float w1 = Math.max(0.0f, weights.oneByOne()) * 1.0f;
        float w2 = Math.max(0.0f, weights.twoByTwo()) * 4.0f;
        float w32 = Math.max(0.0f, weights.threeByTwo()) * 6.0f;
        float w4 = Math.max(0.0f, weights.fourByFour()) * 16.0f;
        float total = w1 + w2 + w32 + w4;
        if (total <= 0.0001f) {
            return FootprintSize.TWO_BY_TWO;
        }

        float roll = unitFloat(hash ^ 0x9E3779B97F4A7C15L) * total;
        float cursor = w1;
        if (roll < cursor) {
            return FootprintSize.ONE_BY_ONE;
        }
        cursor += w2;
        if (roll < cursor) {
            return FootprintSize.TWO_BY_TWO;
        }
        cursor += w32;
        if (roll < cursor) {
            boolean horizontal = ((hash >>> 17) & 1L) == 0L;
            return horizontal ? FootprintSize.THREE_BY_TWO : FootprintSize.TWO_BY_THREE;
        }
        return FootprintSize.FOUR_BY_FOUR;
    }

    private static int tryOccupy(Map<Long, AcceptedFootprint> occupied,
                                 ChunkPredicate buildablePredicate,
                                 AnchorCandidate candidate,
                                 FootprintSize size,
                                 int extMinX,
                                 int extMaxX,
                                 int extMinZ,
                                 int extMaxZ,
                                 int coreMinX,
                                 int coreMaxX,
                                 int coreMinZ,
                                 int coreMaxZ) {
        if (size.width() <= 0 || size.depth() <= 0) {
            return 0;
        }
        if (candidate.anchorX() + size.width() - 1 < extMinX || candidate.anchorX() > extMaxX
                || candidate.anchorZ() + size.depth() - 1 < extMinZ || candidate.anchorZ() > extMaxZ) {
            return 0;
        }
        if (!isBuildableArea(buildablePredicate, candidate.anchorX(), candidate.anchorZ(), size.width(), size.depth())) {
            return 0;
        }
        if (hasConflict(occupied, candidate.anchorX(), candidate.anchorZ(), size.width(), size.depth())) {
            return 0;
        }
        int gain = coreGain(occupied, candidate.anchorX(), candidate.anchorZ(), size.width(), size.depth(),
                coreMinX, coreMaxX, coreMinZ, coreMaxZ);
        if (gain <= 0) {
            return 0;
        }
        occupy(occupied, candidate.anchorX(), candidate.anchorZ(), size.width(), size.depth(), candidate.priority());
        return gain;
    }

    private static long settingsSignature(LostCityProfile profile) {
        if (profile == null) {
            return 0L;
        }
        long hash = 0x6A09E667F3BCC909L;
        hash = mix(hash, profile.isAllowCrossChunkAllBuildings() ? 1L : 0L);
        hash = mix(hash, Float.floatToIntBits(profile.getFootprintWeight1x1()));
        hash = mix(hash, Float.floatToIntBits(profile.getFootprintWeight2x2()));
        hash = mix(hash, Float.floatToIntBits(profile.getFootprintWeight3x2()));
        hash = mix(hash, Float.floatToIntBits(profile.getFootprintWeight4x4()));
        hash = mix(hash, Float.floatToIntBits(profile.getTargetBuildingCoverage()));
        hash = mix(hash, profile.isStreetConnectivityPriorityHigh() ? 1L : 0L);
        hash = mix(hash, profile.isFallbackTo1x1OnConflict() ? 1L : 0L);
        return hash;
    }

    private static long mix(long seed, long value) {
        long hash = seed ^ value;
        hash ^= (hash >>> 29);
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= (hash >>> 31);
        return hash;
    }

    private static long placementHash(long worldSeed, int chunkX, int chunkZ, long salt) {
        long hash = worldSeed ^ salt;
        hash ^= (long) chunkX * 0x9E3779B97F4A7C15L;
        hash ^= (long) chunkZ * 0x94D049BB133111EBL;
        hash ^= (hash >>> 29);
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= (hash >>> 31);
        return hash;
    }

    private static float unitFloat(long hash) {
        return ((hash >>> 40) & 0xFFFFFFL) / (float) 0x1000000L;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return Math.min(value, 1.0f);
    }

    private static long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    private enum FootprintSize {
        ONE_BY_ONE(1, 1),
        TWO_BY_TWO(2, 2),
        THREE_BY_TWO(3, 2),
        TWO_BY_THREE(2, 3),
        FOUR_BY_FOUR(4, 4);

        private final int width;
        private final int depth;

        FootprintSize(int width, int depth) {
            this.width = width;
            this.depth = depth;
        }

        int width() {
            return width;
        }

        int depth() {
            return depth;
        }
    }

    public record FootprintPlacement(boolean active,
                                     int anchorX,
                                     int anchorZ,
                                     int width,
                                     int depth,
                                     int localX,
                                     int localZ,
                                     long priority) {
        public static FootprintPlacement none() {
            return new FootprintPlacement(false, 0, 0, 0, 0, 0, 0, Long.MIN_VALUE);
        }
    }

    private record PlanKey(String dimension, long seed, int regionX, int regionZ, long settingsHash) {
    }

    private record PlannedRegion(Map<Long, FootprintPlacement> placements) {
        FootprintPlacement get(int chunkX, int chunkZ) {
            FootprintPlacement placement = placements.get(pack(chunkX, chunkZ));
            return placement == null ? FootprintPlacement.none() : placement;
        }
    }

    private record AnchorCandidate(int anchorX,
                                   int anchorZ,
                                   long priority,
                                   List<FootprintSize> fallbackSizes,
                                   int preferredArea) {
    }

    private record AcceptedFootprint(int anchorX, int anchorZ, int width, int depth, long priority) {
    }

    private record FootprintWeights(float oneByOne, float twoByTwo, float threeByTwo, float fourByFour) {
    }
}
