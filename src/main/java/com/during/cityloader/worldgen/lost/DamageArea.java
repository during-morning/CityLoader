package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.util.TimedCache;
import com.during.cityloader.worldgen.IDimensionInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * 区块级破坏区域缓存。
 * 将爆炸中心/半径预先确定，保证同 seed + chunk 生成稳定一致。
 */
public final class DamageArea {

    private static final Supplier<Integer> CACHE_TIMEOUT = () -> 300;
    private static final TimedCache<DamageKey, DamageArea> CACHE = new TimedCache<>(CACHE_TIMEOUT);
    private static final long MAIN_EXPLOSION_X_MULTIPLIER = 797003437L;
    private static final long MAIN_EXPLOSION_Z_MULTIPLIER = 295075153L;
    private static final long MINI_EXPLOSION_X_MULTIPLIER = 573259391L;
    private static final long MINI_EXPLOSION_Z_MULTIPLIER = 1400305337L;

    private final List<Blast> blasts;

    private DamageArea(List<Blast> blasts) {
        this.blasts = Collections.unmodifiableList(new ArrayList<>(blasts));
    }

    public static DamageArea getOrCreate(IDimensionInfo provider,
                                         ChunkCoord coord,
                                         int mainMinY,
                                         int mainMaxY,
                                         int miniMinY,
                                         int miniMaxY,
                                         float mainChance,
                                         float miniChance,
                                         int mainMinRadius,
                                         int mainMaxRadius,
                                         int miniMinRadius,
                                         int miniMaxRadius) {
        float normalizedMainChance = Math.max(0.0f, Math.min(1.0f, mainChance));
        float normalizedMiniChance = Math.max(0.0f, Math.min(1.0f, miniChance));
        int mainChanceScaled = Math.round(normalizedMainChance * 10_000.0f);
        int miniChanceScaled = Math.round(normalizedMiniChance * 10_000.0f);

        DamageKey key = new DamageKey(
                coord.dimension(),
                coord.chunkX(),
                coord.chunkZ(),
                Math.min(mainMinY, mainMaxY),
                Math.max(mainMinY, mainMaxY),
                Math.min(miniMinY, miniMaxY),
                Math.max(miniMinY, miniMaxY),
                mainChanceScaled,
                miniChanceScaled,
                Math.max(1, Math.min(mainMinRadius, mainMaxRadius)),
                Math.max(1, Math.max(mainMinRadius, mainMaxRadius)),
                Math.max(1, Math.min(miniMinRadius, miniMaxRadius)),
                Math.max(1, Math.max(miniMinRadius, miniMaxRadius)),
                provider.getProfile() != null && provider.getProfile().isExplosionsInCitiesOnly(),
                provider.getSeed());

        DamageArea cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        DamageArea generated = build(key, provider);
        CACHE.put(key, generated);
        return generated;
    }

    public List<Blast> getBlasts() {
        return blasts;
    }

    public boolean isEmpty() {
        return blasts.isEmpty();
    }

    public static void resetCache() {
        CACHE.clear();
    }

    private static DamageArea build(DamageKey key, IDimensionInfo provider) {
        float mainChance = key.mainChanceScaled() / 10_000.0f;
        float miniChance = key.miniChanceScaled() / 10_000.0f;
        if (mainChance <= 0.0f && miniChance <= 0.0f) {
            return new DamageArea(List.of());
        }

        Random damageRandom = new Random(
                key.seed() + (long) key.chunkZ() * MAIN_EXPLOSION_Z_MULTIPLIER + (long) key.chunkX() * 899826547L);
        int offset = (Math.max(key.mainMaxRadius(), key.miniMaxRadius()) + 15) / 16;
        List<Blast> blasts = new ArrayList<>();

        for (int sourceChunkX = key.chunkX() - offset; sourceChunkX <= key.chunkX() + offset; sourceChunkX++) {
            for (int sourceChunkZ = key.chunkZ() - offset; sourceChunkZ <= key.chunkZ() + offset; sourceChunkZ++) {
                ChunkCoord sourceCoord = new ChunkCoord(key.dimension(), sourceChunkX, sourceChunkZ);
                if (key.explosionsInCitiesOnly()
                        && provider != null
                        && provider.getProfile() != null
                        && !BuildingInfo.isCityRaw(sourceCoord, provider, provider.getProfile())) {
                    continue;
                }

                addBlastIfIntersects(
                        blasts,
                        key,
                        provider,
                        damageRandom,
                        sourceCoord,
                        sourceChunkX,
                        sourceChunkZ,
                        mainChance,
                        key.mainMinY(),
                        key.mainMaxY(),
                        key.mainMinRadius(),
                        key.mainMaxRadius(),
                        true,
                        MAIN_EXPLOSION_X_MULTIPLIER,
                        MAIN_EXPLOSION_Z_MULTIPLIER);

                addBlastIfIntersects(
                        blasts,
                        key,
                        provider,
                        damageRandom,
                        sourceCoord,
                        sourceChunkX,
                        sourceChunkZ,
                        miniChance,
                        key.miniMinY(),
                        key.miniMaxY(),
                        key.miniMinRadius(),
                        key.miniMaxRadius(),
                        false,
                        MINI_EXPLOSION_X_MULTIPLIER,
                        MINI_EXPLOSION_Z_MULTIPLIER);
            }
        }

        if (blasts.isEmpty()) {
            return new DamageArea(List.of());
        }
        return new DamageArea(blasts);
    }

    private static void addBlastIfIntersects(List<Blast> blasts,
                                             DamageKey target,
                                             IDimensionInfo provider,
                                             Random damageRandom,
                                             ChunkCoord sourceCoord,
                                             int sourceChunkX,
                                             int sourceChunkZ,
                                             float chance,
                                             int minY,
                                             int maxY,
                                             int minRadius,
                                             int maxRadius,
                                             boolean destructive,
                                             long xMultiplier,
                                             long zMultiplier) {
        if (chance <= 0.0f) {
            return;
        }

        Random random = chunkRandom(target.seed(), sourceChunkX, sourceChunkZ, xMultiplier, zMultiplier);
        if (random.nextFloat() >= chance) {
            return;
        }

        int centerX = ((sourceChunkX - target.chunkX()) << 4) + random.nextInt(16);
        int centerY = resolveBlastY(provider, sourceCoord, random, minY, maxY);
        int centerZ = ((sourceChunkZ - target.chunkZ()) << 4) + random.nextInt(16);
        int radius = boundedRandom(random, minRadius, maxRadius);
        if (!intersectsChunk(centerX, centerZ, radius)) {
            return;
        }
        if (!passesStyleExplosionChance(provider, sourceCoord, damageRandom)) {
            return;
        }

        blasts.add(new Blast(
                centerX,
                centerY,
                centerZ,
                radius,
                destructive,
                random.nextLong()));
    }

    private static boolean passesStyleExplosionChance(IDimensionInfo provider, ChunkCoord coord, Random random) {
        if (provider == null || provider.getProfile() == null) {
            return true;
        }
        LostChunkCharacteristics characteristics = BuildingInfo.getChunkCharacteristics(coord, provider);
        if (characteristics == null || characteristics.cityStyle == null) {
            return true;
        }
        Float styleChance = characteristics.cityStyle.getExplosionChance();
        if (styleChance == null) {
            return true;
        }
        if (styleChance <= 0.0f) {
            return false;
        }
        return random.nextFloat() < Math.min(styleChance, 1.0f);
    }

    private static int resolveBlastY(IDimensionInfo provider,
                                     ChunkCoord sourceCoord,
                                     Random random,
                                     int minY,
                                     int maxY) {
        int relativeY = boundedRandomExclusiveUpper(random, minY, maxY);
        if (provider == null || provider.getProfile() == null) {
            return relativeY;
        }
        int cityLevel = BuildingInfo.getCityLevel(sourceCoord, provider);
        return cityLevel * 6 + relativeY;
    }

    private static boolean intersectsChunk(int centerX, int centerZ, int radius) {
        int dx = axisDistance(centerX, 0, 15);
        int dz = axisDistance(centerZ, 0, 15);
        return (dx * dx) + (dz * dz) <= radius * radius;
    }

    private static int axisDistance(int value, int min, int max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0;
    }

    private static int boundedRandom(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static int boundedRandomExclusiveUpper(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min);
    }

    private static Random chunkRandom(long worldSeed,
                                      int chunkX,
                                      int chunkZ,
                                      long xMultiplier,
                                      long zMultiplier) {
        long seed = worldSeed + (long) chunkX * xMultiplier + (long) chunkZ * zMultiplier;
        return new Random(seed);
    }

    private record DamageKey(String dimension,
                             int chunkX,
                             int chunkZ,
                             int mainMinY,
                             int mainMaxY,
                             int miniMinY,
                             int miniMaxY,
                             int mainChanceScaled,
                             int miniChanceScaled,
                             int mainMinRadius,
                             int mainMaxRadius,
                             int miniMinRadius,
                             int miniMaxRadius,
                             boolean explosionsInCitiesOnly,
                             long seed) {
    }

    public record Blast(int x, int y, int z, int radius, boolean destructive, long randomSeed) {
    }
}
